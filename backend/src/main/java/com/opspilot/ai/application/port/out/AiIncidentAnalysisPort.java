package com.opspilot.ai.application.port.out;

import com.opspilot.ai.domain.IncidentAnalysisContext;
import com.opspilot.ai.domain.IncidentAnalysisReport;

public interface AiIncidentAnalysisPort {

    IncidentAnalysisReport analyze(IncidentAnalysisContext context);
}
