package org.aoizora.account.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetCourseRequest {
    private String courseId;

    public GetCourseRequest() {

    }

    public GetCourseRequest(String courseId) {
        this.courseId = courseId;
    }
}
