package org.aoizora.account.rmq;

import org.aoizora.account.dto.PaymentRequest;
import org.aoizora.account.dto.PaymentResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class PaymentServiceClient extends BaseServiceClient<PaymentRequest, PaymentResponse> {

    public PaymentServiceClient(RMQClient rmqClient) {
        super(rmqClient, "payment.command", PaymentResponse.class);
    }

    public CompletableFuture<PaymentResponse> pay(PaymentRequest request) {
        return execute(request);
    }
}
