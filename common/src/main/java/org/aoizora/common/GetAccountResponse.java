package org.aoizora.common;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAccountResponse {
    private String correlationId;
    private boolean success;
    private String errorMessage;
    private UserAccountDTO user;
}
