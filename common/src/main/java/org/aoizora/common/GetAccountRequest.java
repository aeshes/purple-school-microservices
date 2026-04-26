package org.aoizora.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAccountRequest {
    private String correlationId;
    private String replyTo;
    private String id;
}
