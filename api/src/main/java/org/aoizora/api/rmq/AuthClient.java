package org.aoizora.api.rmq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.aoizora.api.config.RabbitMQConfig;
import org.aoizora.common.AuthResponseMessage;
import org.aoizora.common.LoginRequestMessage;
import org.aoizora.common.RegisterRequestMessage;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthClient {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private AmqpAdmin amqpAdmin; // Используется для declare/delete

    private final ConcurrentHashMap<String, CompletableFuture<AuthResponseMessage>> pendingRequests = new ConcurrentHashMap<>();

    private SimpleMessageListenerContainer responseListenerContainer;
    private String responseQueueName;

    @PostConstruct
    public void init() {
        // 1. Генерируем уникальное имя для очереди ответов
        responseQueueName = "client.auth.response." + UUID.randomUUID().toString();
        log.info("Initializing temporary response queue: {}", responseQueueName);

        // 2. Создаем временную очередь
        Queue responseQueue = QueueBuilder.nonDurable(responseQueueName)
                .autoDelete()
                .exclusive() // <-- Очередь будет удалена при закрытии соединения
                .build();
        amqpAdmin.declareQueue(responseQueue); // Объявляем очередь на сервере
        log.info("Temporary queue declared: {}", responseQueueName);

        // 3. Создаем Binding для этой очереди
        // В Spring AMQP 3.1.5 нельзя получить exchange через amqpAdmin.getExchange().
        // Вместо этого мы создаем Binding, предполагая, что exchange уже существует.
        // Мы используем ExchangeBuilder для создания объекта Exchange, который нужен только для создания Binding.
        Exchange authExchange = ExchangeBuilder.topicExchange(RabbitMQConfig.EXCHANGE_NAME).build();
        Binding binding = BindingBuilder
                .bind(responseQueue)
                .to(authExchange)
                .with(responseQueueName) // Используем имя очереди как routing key
                .noargs();
        amqpAdmin.declareBinding(binding);
        log.info("Binding declared for queue: {} with routing key: {}", responseQueueName, responseQueueName);

        // 4. Настраиваем Listener для получения ответов
        responseListenerContainer = new SimpleMessageListenerContainer();
        responseListenerContainer.setConnectionFactory(connectionFactory);
        responseListenerContainer.setQueueNames(responseQueueName);

        responseListenerContainer.setMessageListener(message -> {
            try {
                AuthResponseMessage response = (AuthResponseMessage) rabbitTemplate.getMessageConverter()
                        .fromMessage(message);
                if (response != null && response.getCorrelationId() != null) {
                    CompletableFuture<AuthResponseMessage> future = pendingRequests.remove(response.getCorrelationId());
                    if (future != null) {
                        future.complete(response);
                        log.debug("Response delivered for correlationId: {}", response.getCorrelationId());
                    } else {
                        log.warn("No pending request for correlationId: {}", response.getCorrelationId());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing response", e);
            }
        });

        responseListenerContainer.start();
        log.info("AuthClient initialization complete. Listening on: {}", responseQueueName);
    }

    public CompletableFuture<AuthResponseMessage> register(String name, String email, String password) {
        String correlationId = UUID.randomUUID().toString();

        RegisterRequestMessage request = new RegisterRequestMessage();
        request.setCorrelationId(correlationId);
        request.setReplyTo(responseQueueName);
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);

        CompletableFuture<AuthResponseMessage> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.REGISTER_ROUTING_KEY,
                request
        );

        log.info("Sent register request. correlationId: {}, replyTo: {}", correlationId, responseQueueName);

        // Таймаут на случай отсутствия ответа
        return future.orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    pendingRequests.remove(correlationId);
                    log.error("Timeout for correlationId: {}", correlationId, throwable);
                    return AuthResponseMessage.builder()
                            .correlationId(correlationId)
                            .success(false)
                            .errorMessage("Request timeout")
                            .build();
                });
    }

    public CompletableFuture<AuthResponseMessage> login(String email, String password) {
        String correlationId = UUID.randomUUID().toString();

        LoginRequestMessage request = new LoginRequestMessage();
        request.setCorrelationId(correlationId);
        request.setReplyTo(responseQueueName);
        request.setEmail(email);
        request.setPassword(password);

        CompletableFuture<AuthResponseMessage> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.LOGIN_ROUTING_KEY,
                request
        );

        log.info("Sent login request. correlationId: {}, replyTo: {}", correlationId, responseQueueName);

        return future.orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    pendingRequests.remove(correlationId);
                    log.error("Timeout for correlationId: {}", correlationId, throwable);
                    return AuthResponseMessage.builder()
                            .correlationId(correlationId)
                            .success(false)
                            .errorMessage("Request timeout")
                            .build();
                });
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up AuthClient...");
        if (responseListenerContainer != null && responseListenerContainer.isRunning()) {
            responseListenerContainer.stop();
        }
        // Удаляем очередь. Благодаря auto-delete и exclusive это не строго обязательно,
        // но хороший тон для явной очистки.
        if (responseQueueName != null && amqpAdmin != null) {
            amqpAdmin.deleteQueue(responseQueueName);
            log.info("Deleted temporary queue: {}", responseQueueName);
        }
        pendingRequests.forEach((id, future) -> future.cancel(true));
        pendingRequests.clear();
    }
}
