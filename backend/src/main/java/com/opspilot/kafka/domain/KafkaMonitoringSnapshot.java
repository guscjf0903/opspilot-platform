package com.opspilot.kafka.domain;

import java.time.Instant;
import java.util.List;

public record KafkaMonitoringSnapshot(
        String clusterId,
        Instant collectedAt,
        int brokerCount,
        List<KafkaTopicSummary> topics,
        List<KafkaConsumerGroupSummary> consumerGroups,
        List<KafkaConsumerGroupLag> consumerGroupLags
) {
}
