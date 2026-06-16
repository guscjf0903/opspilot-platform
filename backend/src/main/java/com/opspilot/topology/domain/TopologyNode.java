package com.opspilot.topology.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;

public record TopologyNode(
        String id,
        String kind,
        String namespace,
        String name,
        ResourceStatus status,
        String reason,
        String message
) {
}
