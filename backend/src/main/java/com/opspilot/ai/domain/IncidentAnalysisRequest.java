package com.opspilot.ai.domain;

import jakarta.validation.constraints.NotBlank;

public record IncidentAnalysisRequest(
        @NotBlank String namespace,
        @NotBlank String targetKind,
        @NotBlank String targetName,
        Integer timeRangeMinutes
) {
}
