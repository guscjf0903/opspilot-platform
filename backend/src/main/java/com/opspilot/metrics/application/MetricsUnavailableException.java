package com.opspilot.metrics.application;

public class MetricsUnavailableException extends RuntimeException {

    public MetricsUnavailableException(String message) {
        super(message);
    }

    public MetricsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
