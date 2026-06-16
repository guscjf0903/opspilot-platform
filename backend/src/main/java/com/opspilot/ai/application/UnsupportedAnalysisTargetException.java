package com.opspilot.ai.application;

public class UnsupportedAnalysisTargetException extends RuntimeException {

    public UnsupportedAnalysisTargetException(String kind) {
        super("Analysis target kind is not supported yet: " + kind);
    }
}
