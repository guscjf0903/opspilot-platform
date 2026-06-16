package com.opspilot.kafka.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.util.List;

public record KafkaPartitionSummary(
        String topic,
        int partition,
        Integer leaderId,
        List<Integer> replicas,
        List<Integer> inSyncReplicas,
        boolean underReplicated,
        boolean offline,
        ResourceStatus status
) {
}
