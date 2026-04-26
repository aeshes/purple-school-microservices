package org.aoizora.account.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserCoursesResponse {
    private List<CourseDTO> courses;
}
