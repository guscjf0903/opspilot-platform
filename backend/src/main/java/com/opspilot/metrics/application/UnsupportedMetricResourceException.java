package com.opspilot.metrics.application;

public class UnsupportedMetricResourceException extends RuntimeException {

    public UnsupportedMetricResourceException(String kind) {
        super("Metric lookup is not supported for resource kind: " + kind);
    }
}
