package org.aoizora.api.rmq;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aoizora.api.config.RabbitMQConfig;
import org.aoizora.common.GetAccountRequest;
import org.aoizora.common.GetAccountResponse;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
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
    private final ConnectionFactory connectionFactory;
    private SimpleMessageListenerContainer replyContainer;
    private final AmqpAdmin amqpAdmin;
    private String replyQueueName;

    @PostConstruct
    public void init() {
        setupReplyListener();
    }

    private void setupReplyListener() {
        replyQueueName = "account.user-info.reply." + UUID.randomUUID();

        Queue replyQueue = QueueBuilder.nonDurable(replyQueueName)
                .autoDelete()
                .exclusive()
                .build();
        Exchange authExchange = ExchangeBuilder.topicExchange(RabbitMQConfig.EXCHANGE_NAME).build();
        Binding binding = BindingBuilder
                .bind(replyQueue)
                .to(authExchange)
                .with(replyQueueName)
                .noargs();

        amqpAdmin.declareQueue(replyQueue);
        amqpAdmin.declareBinding(binding);

        replyContainer = new SimpleMessageListenerContainer(connectionFactory);
        replyContainer.setQueueNames(replyQueueName);
        replyContainer.setConcurrentConsumers(1);
        replyContainer.setAutoStartup(true);
        replyContainer.setErrorHandler(throwable -> {
            log.error("Error in reply container", throwable);
        });

        replyContainer.setMessageListener(message -> {
            log.info("!!! MESSAGE RECEIVED in listener !!!");
            log.info("Queue: {}", replyQueueName);
            log.info("Message properties: {}", message.getMessageProperties());
            log.info("Message body length: {}", message.getBody().length);

            try {
                GetAccountResponse response = (GetAccountResponse) rabbitTemplate.getMessageConverter()
                        .fromMessage(message);
                if (response != null && response.getCorrelationId() != null) {
                    CompletableFuture<GetAccountResponse> future = pendingRequests.remove(response.getCorrelationId());
                    if (future != null) {
                        future.complete(response);
                        log.info("Response delivered for correlationId: {}", response.getCorrelationId());
                    } else {
                        log.warn("No pending request for correlationId: {}", response.getCorrelationId());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing response", e);
            }
        });

        replyContainer.start();

        log.info("Настроен общий слушатель ответов на очереди: {}", replyQueueName);
    }

    public CompletableFuture<GetAccountResponse> getAccount(String id) {
        String correlationId = UUID.randomUUID().toString();

        GetAccountRequest request = new GetAccountRequest();
        request.setCorrelationId(correlationId);
        request.setReplyTo(replyQueueName);
        request.setId(id);

        CompletableFuture<GetAccountResponse> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.USER_INFO_ROUTING_KEY,
                request
        );

        log.info("Sent user info request. correlationId: {}, replyTo: {}", correlationId, replyQueueName);

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
}
