package org.aoizora.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegisterRequestMessage.class, name = "register"),
        @JsonSubTypes.Type(value = LoginRequestMessage.class, name = "login")
})
public abstract class AuthRequestMessage {
    private String correlationId;
    private String replyTo;
    private String type;
}
