package com.opspilot.kafka.domain;

public record KafkaPartitionLag(
        String topic,
        int partition,
        Long currentOffset,
        Long logEndOffset,
        long lag
) {
}
