package org.aoizora.account.rmq;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class BaseServiceClient<Q, S> {
    protected final RMQClient rmqClient;
    protected final String queueName;
    protected final Class<S> responseClass;

    public BaseServiceClient(RMQClient rmqClient, String queueName, Class<S> responseClass) {
        this.rmqClient = rmqClient;
        this.queueName = queueName;
        this.responseClass = responseClass;
    }

    protected CompletableFuture<S> execute(Q request) {
        log.debug("Sending request to {}: {}", queueName, request);

        return rmqClient.call(queueName, request, responseClass)
                .whenComplete((response, error) -> {
                    if (error != null) {
                        log.error("Error calling {}: {}", queueName, error.getMessage());
                    } else {
                        log.debug("Received response from {}: {}", queueName, response);
                    }
                });
    }
}
