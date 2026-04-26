package org.aoizora.account.dto;

import lombok.Builder;
import lombok.Data;
import org.aoizora.common.Role;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String email;
    private Role role;
}
