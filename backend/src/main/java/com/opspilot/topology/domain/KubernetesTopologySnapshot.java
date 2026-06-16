package com.opspilot.topology.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record KubernetesTopologySnapshot(
        String namespace,
        Instant collectedAt,
        List<Resource> resources
) {

    public record Resource(
            String kind,
            String namespace,
            String name,
            Map<String, String> labels,
            Map<String, String> annotations,
            List<OwnerReference> ownerReferences,
            Map<String, String> selector,
            List<Relation> relations
    ) {
    }

    public record OwnerReference(
            String kind,
            String name
    ) {
    }

    public record Relation(
            String targetKind,
            String targetNamespace,
            String targetName,
            String type
    ) {
    }
}
