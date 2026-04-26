package org.aoizora.account.dao.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCourse {
    private String courseId;
    private PurchaseState purchaseState;
}
