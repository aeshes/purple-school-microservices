package org.aoizora.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterRequestMessage extends AuthRequestMessage {
    private String name;
    private String email;
    private String password;

    public RegisterRequestMessage() {
        setType("register");
    }
}
