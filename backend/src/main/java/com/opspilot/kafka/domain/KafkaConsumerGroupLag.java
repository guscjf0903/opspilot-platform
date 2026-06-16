package com.opspilot.kafka.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.List;

public record KafkaConsumerGroupLag(
        String groupId,
        ResourceStatus status,
        String reason,
        long totalLag,
        Instant collectedAt,
        List<KafkaPartitionLag> partitions
) {

    public static KafkaConsumerGroupLag unavailable(String groupId, String reason, Instant collectedAt) {
        return new KafkaConsumerGroupLag(
                groupId,
                ResourceStatus.UNKNOWN,
                reason,
                0,
                collectedAt,
                List.of()
        );
    }
}
