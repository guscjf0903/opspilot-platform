package com.opspilot.topology.domain;

import java.time.Instant;
import java.util.List;

public record TopologyGraph(
        String clusterId,
        String namespace,
        String rootNodeId,
        Instant collectedAt,
        List<TopologyNode> nodes,
        List<TopologyEdge> edges
) {
}
