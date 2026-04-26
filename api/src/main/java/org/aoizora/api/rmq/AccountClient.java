package org.aoizora.api.rmq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aoizora.api.config.RabbitMQConfig;
import org.aoizora.common.GetAccountRequest;
import org.aoizora.common.GetAccountResponse;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountClient {

    private final Map<String, CompletableFuture<GetAccountResponse>> pendingRequests = new ConcurrentHashMap<>();

    private final RabbitTemplate rabbitTemplate;


    public CompletableFuture<GetAccountResponse> getAccount(String id) {
        String correlationId = UUID.randomUUID().toString();

        GetAccountRequest request = new GetAccountRequest();
        request.setCorrelationId(correlationId);
        request.setReplyTo(RabbitMQConfig.USER_INFO_REPLY_QUEUE);
        request.setId(id);

        CompletableFuture<GetAccountResponse> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.USER_INFO_ROUTING_KEY,
                request,
                message -> {
                    message.getMessageProperties().setCorrelationId(correlationId);
                    message.getMessageProperties().setReplyTo(RabbitMQConfig.USER_INFO_REPLY_QUEUE);

                    return message;
                }
        );

        log.info("Sent user info request. correlationId: {}", correlationId);

        return future.orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    pendingRequests.remove(correlationId);
                    log.error("Timeout for correlationId: {}", correlationId, throwable);
                    return GetAccountResponse.builder()
                            .correlationId(correlationId)
                            .errorMessage(throwable.getMessage())
                            .build();
                });
    }

    @RabbitListener(queues = RabbitMQConfig.USER_INFO_REPLY_QUEUE)
    public void handleUserInfoReply(Message message, Channel channel) {
        final String correlationId = message.getMessageProperties().getCorrelationId();

        if (correlationId == null) {
            log.error("Received message without correlationId");
            return;
        }

        CompletableFuture<GetAccountResponse> future = pendingRequests.remove(correlationId);

        if (future == null) {
            log.warn("Received orphaned reply for correlationId: {}", correlationId);
            return;
        }

        try {
            GetAccountResponse response = (GetAccountResponse) rabbitTemplate.getMessageConverter().fromMessage(message);

            if (!response.isSuccess()) {
                future.completeExceptionally(new RuntimeException(response.getErrorMessage()));
            } else {
                future.complete(response);
            }
        } catch (Exception ex) {
            log.error("Error processing reply for correlationId: {}", correlationId, ex);
            future.completeExceptionally(ex);
        }
    }
}
