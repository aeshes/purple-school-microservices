package org.aoizora.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aoizora.account.config.RabbitMQConfig;
import org.aoizora.account.dao.domain.User;
import org.aoizora.account.dto.AuthRequest;
import org.aoizora.account.dto.AuthResponse;
import org.aoizora.account.dto.RegisterRequest;
import org.aoizora.account.service.exception.EmailAlreadyExistsException;
import org.aoizora.common.AuthRequestMessage;
import org.aoizora.common.AuthResponseMessage;
import org.aoizora.common.LoginRequestMessage;
import org.aoizora.common.RegisterRequestMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import static org.aoizora.account.config.RabbitMQConfig.EXCHANGE_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQService {

    private final AuthService authService;
    private final RabbitTemplate rabbitTemplate;


    @RabbitListener(queues = RabbitMQConfig.REGISTER_QUEUE)
    public void handleRegistrationRequest(AuthRequestMessage request) {
        String replyTo = request.getReplyTo();

        if (replyTo == null) {
            log.error("Не указана очередь reply_to для correlationId: {}", request.getCorrelationId());
            return;
        }

        AuthResponseMessage.AuthResponseMessageBuilder responseBuilder = AuthResponseMessage.builder()
                .correlationId(request.getCorrelationId());

        try {
            if (request instanceof RegisterRequestMessage registerRequest) {
                RegisterRequest authRequest = RegisterRequest.builder()
                        .name(registerRequest.getName())
                        .email(registerRequest.getEmail())
                        .password(registerRequest.getPassword())
                        .build();

                User user = authService.register(authRequest);
                responseBuilder.success(true).data(user);

                log.info("Пользователь успешно зарегистрирован: {}", user.getEmail());

            } else {
                responseBuilder.success(false).errorMessage("Неизвестный тип запроса");
            }

        } catch (EmailAlreadyExistsException e) {
            log.error("Ошибка при регистрации пользователя: {}", e.getMessage());

            responseBuilder.success(false).errorMessage(e.getMessage());
        }

        AuthResponseMessage response = responseBuilder.build();
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, replyTo, response);

        log.info("Отправлен ответ на запрос регистрации для correlationId: {} в очередь: {}", response.getCorrelationId(), replyTo);
    }

    @RabbitListener(queues = RabbitMQConfig.LOGIN_QUEUE)
    public void handleLoginRequest(AuthRequestMessage request) {
        log.info("Получен запрос с correlationId: {}, replyTo: {}", request.getCorrelationId(), request.getReplyTo());

        String replyTo = request.getReplyTo();

        if (replyTo == null) {
            log.error("Не указана очередь reply_to для correlationId: {}", request.getCorrelationId());
            return;
        }

        AuthResponseMessage.AuthResponseMessageBuilder responseBuilder = AuthResponseMessage.builder()
                .correlationId(request.getCorrelationId());

        try {
            if (request instanceof LoginRequestMessage loginRequest) {
                AuthRequest authRequest = AuthRequest.builder()
                        .email(loginRequest.getEmail())
                        .password(loginRequest.getPassword())
                        .build();

                AuthResponse authResponse = authService.login(authRequest);
                responseBuilder.success(true).data(authResponse);

                log.info("Пользователь успешно вошел в систему: {}", authResponse.getEmail());

            } else {
                responseBuilder.success(false).errorMessage("Неизвестный тип запроса");
            }

        } catch (Exception e) {
            log.error("Неуспешный вход в систему: {}", e.getMessage());

            responseBuilder.success(false).errorMessage("Неуспешный вход в систему: " + e.getMessage());
        }

        AuthResponseMessage response = responseBuilder.build();
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, replyTo, response);

        log.info("Отправлен ответ на запрос входа для correlationId: {} в очередь: {}", response.getCorrelationId(), replyTo);
    }
}
