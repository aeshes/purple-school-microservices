package org.aoizora.account.service;

import lombok.RequiredArgsConstructor;
import org.aoizora.account.config.RabbitMQConfig;
import org.aoizora.account.dao.repository.UserRepository;
import org.aoizora.account.dto.GetAccountRequest;
import org.aoizora.account.dto.UserCoursesRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserQueries {

    private final UserRepository userRepository;

    @RabbitListener(queues = RabbitMQConfig.USER_INFO_QUEUE)
    public void userInfo(GetAccountRequest request) {

    }

    @RabbitListener(queues = RabbitMQConfig.USER_COURSES_QUEUE)
    public void userCourses(UserCoursesRequest request) {

    }
}
