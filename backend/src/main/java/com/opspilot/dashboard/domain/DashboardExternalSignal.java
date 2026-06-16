package com.opspilot.dashboard.domain;

import java.util.List;

public record DashboardExternalSignal(
        DashboardExternalSignalStatus status,
        String reason,
        List<DashboardExternalMetric> metrics
) {

    public static DashboardExternalSignal unavailable(String reason) {
        return new DashboardExternalSignal(DashboardExternalSignalStatus.UNAVAILABLE, reason, List.of());
    }

    public static DashboardExternalSignal available(String reason, List<DashboardExternalMetric> metrics) {
        return new DashboardExternalSignal(DashboardExternalSignalStatus.AVAILABLE, reason, metrics);
    }
}
