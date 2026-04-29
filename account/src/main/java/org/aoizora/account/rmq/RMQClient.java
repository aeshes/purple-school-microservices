package org.aoizora.account.rmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RMQClient {
    private final RabbitTemplate rabbitTemplate;
    private final Map<String, CompletableFuture<Object>> requests = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final long DEFAULT_TIMEOUT_MS = 30000;
    private static final String REPLY_QUEUE = "amq.rabbitmq.reply-to";

    public <T> CompletableFuture<T> call(String queue, Object request, Class<T> responseType) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Object> future = new CompletableFuture<>();
        requests.put(correlationId, future);

        try {
            rabbitTemplate.convertAndSend(queue, request, message -> {
                MessageProperties props = message.getMessageProperties();
                props.setCorrelationId(correlationId);
                props.setReplyTo(REPLY_QUEUE);
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return message;
            });

            scheduleTimeout(correlationId, future);

            return future.thenApply(response -> convertResponse(response, responseType));

        } catch (Exception e) {
            requests.remove(correlationId);
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    @RabbitListener(queues = REPLY_QUEUE)
    public void handleResponse(Message message) {
        String correlationId = message.getMessageProperties().getCorrelationId();
        CompletableFuture<Object> future = requests.remove(correlationId);

        if (future != null && !future.isDone()) {
            try {
                Object response = mapper.readValue(message.getBody(), Object.class);
                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }
    }

    private void scheduleTimeout(String correlationId, CompletableFuture<Object> future) {
        CompletableFuture.delayedExecutor(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS).execute(() -> {
            if (!future.isDone()) {
                requests.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout for request: " + correlationId));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T convertResponse(Object response, Class<T> responseClass) {
        if (response == null) {
            throw new RuntimeException("Null response received");
        }
        if (responseClass.isInstance(response)) {
            return (T) response;
        }
        return mapper.convertValue(response, responseClass);
    }
}
