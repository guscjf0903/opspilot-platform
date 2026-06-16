package com.opspilot.action.domain;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record ActionRequest(
        @NotBlank String type,
        @NotBlank String namespace,
        @NotBlank String targetKind,
        @NotBlank String targetName,
        Map<String, String> parameters,
        String reason
) {
    public Map<String, String> safeParameters() {
        return parameters == null ? Map.of() : parameters;
    }
}
