package com.opspilot.ai.application;

import java.util.UUID;

public class IncidentAnalysisNotFoundException extends RuntimeException {

    public IncidentAnalysisNotFoundException(UUID analysisId) {
        super("Incident analysis not found: " + analysisId);
    }
}
