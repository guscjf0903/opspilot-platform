package com.opspilot.action.application;

public class ActionTargetNotFoundException extends RuntimeException {

    public ActionTargetNotFoundException(String targetKind, String targetName) {
        super("Action target not found: " + targetKind + "/" + targetName);
    }
}
