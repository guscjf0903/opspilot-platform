package com.opspilot.topology.domain;

public record TopologyEdge(
        String source,
        String target,
        String type
) {
}
