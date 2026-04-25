package org.aoizora.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor  // ← ОБЯЗАТЕЛЬНО для Jackson
@AllArgsConstructor
public class AuthResponseMessage {
    private String correlationId;
    private boolean success;
    private String errorMessage;
    private Object data;
}
