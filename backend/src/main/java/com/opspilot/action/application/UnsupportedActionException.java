package com.opspilot.action.application;

public class UnsupportedActionException extends RuntimeException {

    public UnsupportedActionException(String actionType) {
        super("Unsupported action type: " + actionType);
    }
}
