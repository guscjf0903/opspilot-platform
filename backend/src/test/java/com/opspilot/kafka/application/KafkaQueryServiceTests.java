package com.opspilot.kafka.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.opspilot.kafka.application.port.out.KafkaMonitoringPort;
import com.opspilot.kafka.config.OpspilotKafkaProperties;
import com.opspilot.kafka.domain.KafkaConsumerGroupLag;
import com.opspilot.kafka.domain.KafkaConsumerGroupSummary;
import com.opspilot.kafka.domain.KafkaMonitoringSnapshot;
import com.opspilot.kafka.domain.KafkaTopicSummary;
import com.opspilot.kubernetes.application.UnknownClusterException;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KafkaQueryServiceTests {

    private static final Instant NOW = Instant.parse("2026-06-08T00:30:00Z");

    private KafkaMonitoringPort kafkaMonitoringPort;
    private OpspilotKafkaProperties kafkaProperties;
    private KafkaQueryService kafkaQueryService;

    @BeforeEach
    void setUp() {
        kafkaMonitoringPort = mock(KafkaMonitoringPort.class);
        kafkaProperties = new OpspilotKafkaProperties();
        OpspilotKubernetesProperties kubernetesProperties = new OpspilotKubernetesProperties();
        kafkaQueryService = new KafkaQueryService(
                kafkaMonitoringPort,
                kubernetesProperties,
                kafkaProperties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void returnsOverviewFromKafkaSnapshot() {
        when(kafkaMonitoringPort.fetchSnapshot("local"))
                .thenReturn(snapshot(group("order-consumer", 120, ResourceStatus.WARNING)));

        var overview = kafkaQueryService.getOverview("local");

        assertThat(overview.available()).isTrue();
        assertThat(overview.status()).isEqualTo(ResourceStatus.WARNING);
        assertThat(overview.brokerCount()).isEqualTo(1);
        assertThat(overview.topicCount()).isEqualTo(1);
        assertThat(overview.consumerGroupCount()).isEqualTo(1);
        assertThat(overview.totalLag()).isEqualTo(120);
        assertThat(overview.laggingConsumerGroupCount()).isEqualTo(1);
    }

    @Test
    void returnsUnavailableOverviewWhenKafkaIsDisabled() {
        kafkaProperties.setEnabled(false);

        var overview = kafkaQueryService.getOverview("local");

        assertThat(overview.available()).isFalse();
        assertThat(overview.reason()).isEqualTo("KAFKA_DISABLED");
        verifyNoInteractions(kafkaMonitoringPort);
    }

    @Test
    void sortsTopConsumerGroupLagByLagDescending() {
        when(kafkaMonitoringPort.fetchSnapshot("local"))
                .thenReturn(snapshot(
                        group("fast-consumer", 3, ResourceStatus.HEALTHY),
                        group("slow-consumer", 250, ResourceStatus.WARNING)
                ));

        var topLag = kafkaQueryService.getTopConsumerGroupLags("local", 1);

        assertThat(topLag).singleElement()
                .satisfies(groupLag -> {
                    assertThat(groupLag.groupId()).isEqualTo("slow-consumer");
                    assertThat(groupLag.totalLag()).isEqualTo(250);
                });
    }

    @Test
    void rejectsUnknownCluster() {
        assertThatThrownBy(() -> kafkaQueryService.getOverview("missing"))
                .isInstanceOf(UnknownClusterException.class);
    }

    private KafkaMonitoringSnapshot snapshot(KafkaConsumerGroupSummary... groups) {
        List<KafkaConsumerGroupSummary> consumerGroups = List.of(groups);
        List<KafkaConsumerGroupLag> lags = consumerGroups.stream()
                .map(group -> new KafkaConsumerGroupLag(
                        group.groupId(),
                        group.status(),
                        group.reason(),
                        group.totalLag(),
                        NOW,
                        List.of()
                ))
                .toList();

        return new KafkaMonitoringSnapshot(
                "local",
                NOW,
                1,
                List.of(new KafkaTopicSummary(
                        "orders.created",
                        1,
                        1,
                        0,
                        0,
                        ResourceStatus.HEALTHY,
                        List.of()
                )),
                consumerGroups,
                lags
        );
    }

    private KafkaConsumerGroupSummary group(String groupId, long lag, ResourceStatus status) {
        return new KafkaConsumerGroupSummary(
                groupId,
                "STABLE",
                1,
                1,
                1,
                lag,
                status,
                status == ResourceStatus.HEALTHY ? "KAFKA_CONSUMER_GROUP_HEALTHY" : "KAFKA_LAG_WARNING"
        );
    }
}
