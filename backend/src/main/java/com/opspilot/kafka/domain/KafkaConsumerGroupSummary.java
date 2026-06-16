package com.opspilot.kafka.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;

public record KafkaConsumerGroupSummary(
        String groupId,
        String state,
        int memberCount,
        int topicCount,
        int partitionCount,
        long totalLag,
        ResourceStatus status,
        String reason
) {
}
