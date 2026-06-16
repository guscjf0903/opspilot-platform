package com.opspilot.metrics.domain;

import java.time.Instant;

public record MetricPoint(
        Instant timestamp,
        double value
) {
}
