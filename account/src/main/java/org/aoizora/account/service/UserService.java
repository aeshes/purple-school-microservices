package org.aoizora.account.service;

import lombok.RequiredArgsConstructor;
import org.aoizora.account.dao.domain.PurchaseState;
import org.aoizora.account.dao.domain.UserCourse;
import org.aoizora.account.dao.repository.UserRepository;
import org.aoizora.account.service.exception.AlreadyExistsException;
import org.aoizora.account.service.exception.CourseNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void addCourse(String userId, String courseId) {
        var user = userRepository.findById(userId);
        user.ifPresent(it -> {
            boolean exists = it.getCourses().stream()
                    .anyMatch(c -> c.getCourseId().equals(courseId));
            if (!exists) {
                it.getCourses().add(new UserCourse(courseId, PurchaseState.STARTED));
                userRepository.save(it);
            } else {
                throw new AlreadyExistsException(String.format("Курс %s уже добавлен пользователю %s.", courseId, userId));
            }
        });
    }

    @Transactional(readOnly = true)
    public void deleteCourse(String userId, String courseId) {
        var user = userRepository.findById(userId);
        user.ifPresent(it -> {
            boolean exists = it.getCourses().stream()
                    .anyMatch(c -> c.getCourseId().equals(courseId));
            if (exists) {
                it.getCourses().removeIf(c -> c.getCourseId().equals(courseId));
                userRepository.save(it);
            }
        });
    }

    @Transactional
    public void updateCourseStatus(String userId, String courseId, PurchaseState state) {
        var user = userRepository.findById(userId);
        user.ifPresent(it -> {
            var course = it.getCourses().stream()
                    .filter(c -> c.getCourseId().equals(courseId))
                    .findFirst();

            course.ifPresent(c -> c.setPurchaseState(state));

            if (course.isEmpty()) {
                throw new CourseNotFoundException(String.format("У пользователя %s нет курса %s.", userId, courseId));
            }
        });
    }
}
