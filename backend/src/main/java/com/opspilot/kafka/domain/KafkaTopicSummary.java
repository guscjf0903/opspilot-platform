package com.opspilot.kafka.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.util.List;

public record KafkaTopicSummary(
        String name,
        int partitionCount,
        int replicationFactor,
        int underReplicatedPartitionCount,
        int offlinePartitionCount,
        ResourceStatus status,
        List<KafkaPartitionSummary> partitions
) {
}
