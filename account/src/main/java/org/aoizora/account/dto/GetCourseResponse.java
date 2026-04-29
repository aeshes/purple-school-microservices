package org.aoizora.account.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetCourseResponse {
    private String courseId;
    private String name;
    private Integer price;
}
