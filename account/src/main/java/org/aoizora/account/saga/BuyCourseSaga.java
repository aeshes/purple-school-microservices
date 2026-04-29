package org.aoizora.account.saga;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aoizora.account.dao.domain.PurchaseState;
import org.aoizora.account.dto.PaymentRequest;
import org.aoizora.account.rmq.CourseServiceClient;
import org.aoizora.account.rmq.PaymentServiceClient;
import org.aoizora.account.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class BuyCourseSaga {

    private final UserService userService;
    private final CourseServiceClient courseClient;
    private final PaymentServiceClient paymentClient;

    private final Map<String, SagaContext> activeSagas = new ConcurrentHashMap<>();

    @Transactional
    public String start(String userId, String courseId) {
        String sagaId = UUID.randomUUID().toString();

        userService.addCourse(userId, courseId);
        activeSagas.put(sagaId, new SagaContext(sagaId, userId, courseId));

        log.info("Saga {} started. Added course {} for user {}.", sagaId, courseId, userId);

        return sagaId;
    }

    @Transactional
    public void confirmPayment(String sagaId) {
        SagaContext context = activeSagas.get(sagaId);
        if (context == null) {
            throw new RuntimeException("Saga not found: " + sagaId);
        }

        userService.updateCourseStatus(context.getUserId(), context.getCourseId(), PurchaseState.WAITING_FOR_PAYMENT);

        log.info("Payment confirmed for saga {}. Status: WAITING_FOR_PAYMENT.", sagaId);

        processPayment(sagaId, context);
    }

    private void processPayment(String sagaId, SagaContext context) {
        PaymentRequest request = PaymentRequest.builder()
                        .sagaId(sagaId)
                        .userId(context.getUserId())
                        .courseId(context.getCourseId())
                        .price(getCoursePrice(context.getCourseId()))
                        .build();


        paymentClient.pay(request)
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        completePurchase(sagaId);
                    } else {
                        cancelSaga(sagaId, "Payment failed");
                    }
                })
                .exceptionally(throwable -> {
                    cancelSaga(sagaId, "Payment error: " + throwable.getMessage());
                    return null;
                });
    }

    private Integer getCoursePrice(String courseId) {
        return 42;
    }

    @Transactional
    public void completePurchase(String sagaId) {
        SagaContext context = activeSagas.get(sagaId);

        if (context == null) {
            throw new RuntimeException("Saga not found: " + sagaId);
        }

        userService.updateCourseStatus(context.getUserId(), context.getCourseId(), PurchaseState.PURCHASED);
        activeSagas.remove(sagaId);

        log.info("Purchase completed for saga {}. Status PURCHASED.", sagaId);
    }

    @Transactional
    public void cancelSaga(String sagaId, String reason) {
        SagaContext context = activeSagas.remove(sagaId);

        if (context == null) {
            return;
        }

        userService.updateCourseStatus(context.getUserId(), context.getCourseId(), PurchaseState.CANCELLED);

        log.info("Saga {} cancelled. Reason: {}.", sagaId, reason);
    }

    @Data
    private static class SagaContext {
        private final String sagaId;
        private final String userId;
        private final String courseId;
        private boolean paymentProcessed;
    }
}
