package org.aoizora.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aoizora.account.config.RabbitMQConfig;
import org.aoizora.account.dao.domain.User;
import org.aoizora.account.dao.repository.UserRepository;
import org.aoizora.account.dto.*;
import org.aoizora.common.GetAccountRequest;
import org.aoizora.common.GetAccountResponse;
import org.aoizora.common.UserAccountDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static org.aoizora.account.config.RabbitMQConfig.EXCHANGE_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueries {

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.USER_INFO_QUEUE)
    public void userInfo(GetAccountRequest request) {
        String replyTo = request.getReplyTo();

        if (replyTo == null) {
            log.error("Не указана очередь reply_to");
            return;
        }

        Optional<User> user = userRepository.findById(request.getId());
        user.ifPresent(it -> {
            GetAccountResponse response = new GetAccountResponse();
            UserAccountDTO dto = new UserAccountDTO();
            dto.setId(it.getId());
            dto.setName(it.getName());
            dto.setEmail(it.getEmail());
            dto.setRole(it.getRole());
            response.setSuccess(true);
            response.setCorrelationId(request.getCorrelationId());
            response.setUser(dto);

            rabbitTemplate.convertAndSend(EXCHANGE_NAME, replyTo, response);

            log.info("Отправлен ответ в очередь {}.", replyTo);
        });

    }

    @RabbitListener(queues = RabbitMQConfig.USER_COURSES_QUEUE)
    public void userCourses(UserCoursesRequest request) {

    }
}
