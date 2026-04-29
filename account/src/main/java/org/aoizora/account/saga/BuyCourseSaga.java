package org.aoizora.account.saga;

import lombok.Setter;
import org.aoizora.account.dao.domain.PurchaseState;
import org.aoizora.account.dao.repository.UserRepository;
import org.aoizora.account.service.UserService;

public class BuyCourseSaga {

    private BuyCourseSagaState state;

    @Setter
    private UserService userService;

    @Setter
    private UserRepository userRepository;

    public BuyCourseSaga() {

    }


    public void setState(String userId, String courseId, PurchaseState state) {
        switch (state) {
            case STARTED -> {
                break;
            }

            case WAITING_FOR_PAYMENT -> {
                break;
            }

            case PURCHASED -> {
                break;
            }

            case CANCELLED -> {

            }
        }

        this.state.setContext(this);
        this.userService.updateCourseStatus(userId, courseId, state);
    }
}
