package com.opspilot.kafka.adapter.out.admin;

import com.opspilot.kafka.application.port.out.KafkaMonitoringPort;
import com.opspilot.kafka.domain.KafkaConsumerGroupLag;
import com.opspilot.kafka.domain.KafkaConsumerGroupSummary;
import com.opspilot.kafka.domain.KafkaMonitoringSnapshot;
import com.opspilot.kafka.domain.KafkaPartitionLag;
import com.opspilot.kafka.domain.KafkaPartitionSummary;
import com.opspilot.kafka.domain.KafkaTopicSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "opspilot.kafka", name = "mode", havingValue = "fake")
public class FakeKafkaMonitoringAdapter implements KafkaMonitoringPort {

    private final Clock kafkaClock;

    public FakeKafkaMonitoringAdapter(Clock kafkaClock) {
        this.kafkaClock = kafkaClock;
    }

    @Override
    public KafkaMonitoringSnapshot fetchSnapshot(String clusterId) {
        KafkaTopicSummary topic = new KafkaTopicSummary(
                "orders.created",
                1,
                1,
                0,
                0,
                ResourceStatus.HEALTHY,
                List.of(new KafkaPartitionSummary(
                        "orders.created",
                        0,
                        0,
                        List.of(0),
                        List.of(0),
                        false,
                        false,
                        ResourceStatus.HEALTHY
                ))
        );
        KafkaPartitionLag partitionLag = new KafkaPartitionLag("orders.created", 0, 10L, 10L, 0);
        KafkaConsumerGroupLag groupLag = new KafkaConsumerGroupLag(
                "order-consumer",
                ResourceStatus.HEALTHY,
                "KAFKA_CONSUMER_GROUP_HEALTHY",
                0,
                kafkaClock.instant(),
                List.of(partitionLag)
        );
        KafkaConsumerGroupSummary group = new KafkaConsumerGroupSummary(
                "order-consumer",
                "STABLE",
                1,
                1,
                1,
                0,
                ResourceStatus.HEALTHY,
                "KAFKA_CONSUMER_GROUP_HEALTHY"
        );

        return new KafkaMonitoringSnapshot(
                clusterId,
                kafkaClock.instant(),
                1,
                List.of(topic),
                List.of(group),
                List.of(groupLag)
        );
    }
}
