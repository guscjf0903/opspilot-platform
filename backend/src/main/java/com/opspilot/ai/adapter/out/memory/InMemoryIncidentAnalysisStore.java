package com.opspilot.ai.adapter.out.memory;

import com.opspilot.ai.application.port.out.IncidentAnalysisStore;
import com.opspilot.ai.domain.IncidentAnalysisReport;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "opspilot.ai.store", name = "type", havingValue = "memory")
public class InMemoryIncidentAnalysisStore implements IncidentAnalysisStore {

    private final ConcurrentMap<UUID, IncidentAnalysisReport> reports = new ConcurrentHashMap<>();

    @Override
    public IncidentAnalysisReport save(IncidentAnalysisReport report) {
        reports.put(report.analysisId(), report);

        return report;
    }

    @Override
    public Optional<IncidentAnalysisReport> findById(UUID analysisId) {
        return Optional.ofNullable(reports.get(analysisId));
    }

    @Override
    public List<IncidentAnalysisReport> findByClusterId(String clusterId) {
        return reports.values().stream()
                .filter(report -> report.clusterId().equals(clusterId))
                .sorted(Comparator.comparing(IncidentAnalysisReport::createdAt).reversed())
                .toList();
    }
}
