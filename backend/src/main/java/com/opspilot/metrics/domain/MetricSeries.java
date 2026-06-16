package com.opspilot.metrics.domain;

import java.util.List;

public record MetricSeries(
        String name,
        String unit,
        List<MetricPoint> points
) {

    public static MetricSeries empty(String name, String unit) {
        return new MetricSeries(name, unit, List.of());
    }
}
