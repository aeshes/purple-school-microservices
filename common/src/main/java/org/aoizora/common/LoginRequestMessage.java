package org.aoizora.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginRequestMessage extends AuthRequestMessage {
    private String email;
    private String password;

    public LoginRequestMessage() {
        setType("login");
    }
}
