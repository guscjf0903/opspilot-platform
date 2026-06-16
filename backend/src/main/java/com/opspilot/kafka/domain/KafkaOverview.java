package com.opspilot.kafka.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;

public record KafkaOverview(
        String clusterId,
        boolean available,
        ResourceStatus status,
        String reason,
        int brokerCount,
        int topicCount,
        int consumerGroupCount,
        long totalLag,
        int laggingConsumerGroupCount,
        Instant collectedAt
) {

    public static KafkaOverview unavailable(String clusterId, String reason, Instant collectedAt) {
        return new KafkaOverview(
                clusterId,
                false,
                ResourceStatus.UNKNOWN,
                reason,
                0,
                0,
                0,
                0,
                0,
                collectedAt
        );
    }
}
