package com.opspilot.kubernetes.application;

public class UnknownClusterException extends RuntimeException {

    public UnknownClusterException(String clusterId) {
        super("Cluster not found: " + clusterId);
    }
}
