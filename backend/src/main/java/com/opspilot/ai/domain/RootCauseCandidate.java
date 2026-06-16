package com.opspilot.ai.domain;

import java.util.List;

public record RootCauseCandidate(
        String title,
        double confidence,
        List<String> evidenceIds
) {
}
