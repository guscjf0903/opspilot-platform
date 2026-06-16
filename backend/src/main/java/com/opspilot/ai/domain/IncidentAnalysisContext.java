package com.opspilot.ai.domain;

import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.metrics.domain.ResourceMetrics;
import com.opspilot.topology.domain.TopologyGraph;
import java.time.Instant;
import java.util.List;

public record IncidentAnalysisContext(
        String clusterId,
        IncidentAnalysisTarget target,
        int timeRangeMinutes,
        Instant collectedAt,
        List<EventSummary> events,
        ResourceMetrics metrics,
        TopologyGraph topology
) {
}
