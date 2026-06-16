package com.opspilot.ai.application.port.out;

import com.opspilot.ai.domain.IncidentAnalysisReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentAnalysisStore {

    IncidentAnalysisReport save(IncidentAnalysisReport report);

    Optional<IncidentAnalysisReport> findById(UUID analysisId);

    List<IncidentAnalysisReport> findByClusterId(String clusterId);
}
