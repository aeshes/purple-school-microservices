package org.aoizora.account.rmq;

import org.aoizora.account.dto.GetCourseRequest;
import org.aoizora.account.dto.GetCourseResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class CourseServiceClient extends BaseServiceClient<GetCourseRequest, GetCourseResponse> {

    public CourseServiceClient(RMQClient rmqClient) {
        super(rmqClient, "course.get-course.query", GetCourseResponse.class);
    }

    public CompletableFuture<GetCourseResponse> getCourse(String courseId) {
        return execute(new GetCourseRequest(courseId));
    }
}
