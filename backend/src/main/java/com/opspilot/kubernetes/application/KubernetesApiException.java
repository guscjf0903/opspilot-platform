package com.opspilot.kubernetes.application;

public class KubernetesApiException extends RuntimeException {

    public KubernetesApiException(String operation, Throwable cause) {
        super("Kubernetes API request failed while trying to " + operation, cause);
    }
}
