package com.opspilot.ai.adapter.out.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.ai.application.port.out.IncidentAnalysisStore;
import com.opspilot.ai.domain.IncidentAnalysisReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaIncidentAnalysisStore implements IncidentAnalysisStore {

    private final JpaIncidentAnalysisReportRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public IncidentAnalysisReport save(IncidentAnalysisReport report) {
        return repository.save(IncidentAnalysisReportEntity.fromDomain(report, objectMapper))
                .toDomain(objectMapper);
    }

    @Override
    public Optional<IncidentAnalysisReport> findById(UUID analysisId) {
        return repository.findById(analysisId)
                .map(entity -> entity.toDomain(objectMapper));
    }

    @Override
    public List<IncidentAnalysisReport> findByClusterId(String clusterId) {
        return repository.findByClusterIdOrderByCreatedAtDesc(clusterId).stream()
                .map(entity -> entity.toDomain(objectMapper))
                .toList();
    }
}
