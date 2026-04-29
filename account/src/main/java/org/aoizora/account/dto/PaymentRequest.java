package org.aoizora.account.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentRequest {
    private String sagaId;
    private String userId;
    private String courseId;
    private Integer price;
}
