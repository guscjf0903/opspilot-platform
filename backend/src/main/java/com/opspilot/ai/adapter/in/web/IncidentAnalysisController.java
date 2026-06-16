package com.opspilot.ai.adapter.in.web;

import com.opspilot.ai.application.IncidentAnalysisService;
import com.opspilot.ai.domain.IncidentAnalysisReport;
import com.opspilot.ai.domain.IncidentAnalysisRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}/analysis/incidents")
@RequiredArgsConstructor
public class IncidentAnalysisController {

    private final IncidentAnalysisService incidentAnalysisService;

    @PostMapping
    public IncidentAnalysisReport analyzeIncident(
            @PathVariable String clusterId,
            @RequestBody @Valid IncidentAnalysisRequest request
    ) {
        return incidentAnalysisService.analyzeIncident(clusterId, request);
    }

    @GetMapping("/{analysisId}")
    public IncidentAnalysisReport getIncidentAnalysis(@PathVariable UUID analysisId) {
        return incidentAnalysisService.getIncidentAnalysis(analysisId);
    }

    @GetMapping
    public List<IncidentAnalysisReport> getIncidentAnalyses(@PathVariable String clusterId) {
        return incidentAnalysisService.getIncidentAnalyses(clusterId);
    }
}
