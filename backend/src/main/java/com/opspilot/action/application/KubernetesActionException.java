package com.opspilot.action.application;

public class KubernetesActionException extends RuntimeException {

    public KubernetesActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
