package org.aoizora.account.dto;

import lombok.Builder;
import lombok.Data;
import org.aoizora.common.Role;

@Data
@Builder
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private Role role;
}
