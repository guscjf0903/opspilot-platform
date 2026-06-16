package com.opspilot.ai.application;

public class AnalysisTargetNotFoundException extends RuntimeException {

    public AnalysisTargetNotFoundException(String kind, String name) {
        super("Analysis target not found: %s/%s".formatted(kind, name));
    }
}
