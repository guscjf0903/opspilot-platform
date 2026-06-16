package com.opspilot.ai.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentAnalysisReport(
        UUID analysisId,
        String clusterId,
        String namespace,
        String targetKind,
        String targetName,
        ResourceStatus severity,
        String summary,
        List<String> impactScope,
        List<RootCauseCandidate> rootCauseCandidates,
        List<IncidentEvidence> evidence,
        List<RecommendedAction> recommendations,
        List<String> nextChecks,
        String provider,
        String model,
        String providerResponseId,
        AiTokenUsage tokenUsage,
        long latencyMs,
        Instant createdAt
) {
}
