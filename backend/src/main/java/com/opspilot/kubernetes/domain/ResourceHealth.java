package com.opspilot.kubernetes.domain;

public record ResourceHealth(
        ResourceStatus status,
        String reason,
        String message
) {

    public static ResourceHealth healthy() {
        return new ResourceHealth(ResourceStatus.HEALTHY, "Healthy", "Resource is healthy");
    }

    public static ResourceHealth warning(String reason, String message) {
        return new ResourceHealth(ResourceStatus.WARNING, reason, message);
    }

    public static ResourceHealth critical(String reason, String message) {
        return new ResourceHealth(ResourceStatus.CRITICAL, reason, message);
    }

    public static ResourceHealth unknown(String reason, String message) {
        return new ResourceHealth(ResourceStatus.UNKNOWN, reason, message);
    }
}
