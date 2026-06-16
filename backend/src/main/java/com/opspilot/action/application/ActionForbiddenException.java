package com.opspilot.action.application;

public class ActionForbiddenException extends RuntimeException {

    public ActionForbiddenException(String message) {
        super(message);
    }
}
