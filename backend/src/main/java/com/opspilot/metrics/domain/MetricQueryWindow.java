package com.opspilot.metrics.domain;

import java.time.Duration;
import java.time.Instant;

public record MetricQueryWindow(
        int rangeMinutes,
        Instant start,
        Instant end,
        Duration step
) {

    private static final int DEFAULT_RANGE_MINUTES = 30;
    private static final int MIN_RANGE_MINUTES = 5;
    private static final int MAX_RANGE_MINUTES = 180;

    public static MetricQueryWindow recent(Integer requestedRangeMinutes, Instant end) {
        int normalizedRange = requestedRangeMinutes == null ? DEFAULT_RANGE_MINUTES : requestedRangeMinutes;
        normalizedRange = Math.max(MIN_RANGE_MINUTES, Math.min(MAX_RANGE_MINUTES, normalizedRange));
        Duration step = normalizedRange <= 60 ? Duration.ofSeconds(60) : Duration.ofSeconds(120);

        return new MetricQueryWindow(
                normalizedRange,
                end.minus(Duration.ofMinutes(normalizedRange)),
                end,
                step
        );
    }
}
