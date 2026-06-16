package com.opspilot.kafka.application;

import com.opspilot.kafka.application.port.out.KafkaMonitoringPort;
import com.opspilot.kafka.config.OpspilotKafkaProperties;
import com.opspilot.kafka.domain.KafkaConsumerGroupLag;
import com.opspilot.kafka.domain.KafkaConsumerGroupSummary;
import com.opspilot.kafka.domain.KafkaMonitoringSnapshot;
import com.opspilot.kafka.domain.KafkaOverview;
import com.opspilot.kafka.domain.KafkaTopicSummary;
import com.opspilot.kubernetes.application.UnknownClusterException;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaQueryService {

    private static final int DEFAULT_LAG_LIMIT = 5;

    private final KafkaMonitoringPort kafkaMonitoringPort;
    private final OpspilotKubernetesProperties kubernetesProperties;
    private final OpspilotKafkaProperties kafkaProperties;
    private final Clock kafkaClock;

    public KafkaOverview getOverview(String clusterId) {
        validateCluster(clusterId);

        if (!kafkaProperties.isEnabled()) {
            return KafkaOverview.unavailable(clusterId, "KAFKA_DISABLED", kafkaClock.instant());
        }

        try {
            return toOverview(kafkaMonitoringPort.fetchSnapshot(clusterId));
        } catch (KafkaUnavailableException exception) {
            return KafkaOverview.unavailable(clusterId, exception.getMessage(), kafkaClock.instant());
        }
    }

    public List<KafkaTopicSummary> getTopics(String clusterId) {
        return fetchSnapshot(clusterId)
                .map(KafkaMonitoringSnapshot::topics)
                .orElse(List.of());
    }

    public List<KafkaConsumerGroupSummary> getConsumerGroups(String clusterId) {
        return fetchSnapshot(clusterId)
                .map(KafkaMonitoringSnapshot::consumerGroups)
                .orElse(List.of());
    }

    public List<KafkaConsumerGroupLag> getTopConsumerGroupLags(String clusterId, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);

        return fetchSnapshot(clusterId)
                .map(snapshot -> snapshot.consumerGroupLags().stream()
                        .sorted(Comparator.comparing(KafkaConsumerGroupLag::totalLag).reversed())
                        .limit(normalizedLimit)
                        .toList())
                .orElse(List.of());
    }

    public KafkaConsumerGroupLag getConsumerGroupLag(String clusterId, String groupId) {
        validateCluster(clusterId);

        if (!kafkaProperties.isEnabled()) {
            return KafkaConsumerGroupLag.unavailable(groupId, "KAFKA_DISABLED", kafkaClock.instant());
        }

        return fetchSnapshot(clusterId)
                .flatMap(snapshot -> snapshot.consumerGroupLags().stream()
                        .filter(lag -> lag.groupId().equals(groupId))
                        .findFirst())
                .orElseGet(() -> KafkaConsumerGroupLag.unavailable(
                        groupId,
                        "KAFKA_CONSUMER_GROUP_NOT_FOUND",
                        kafkaClock.instant()
                ));
    }

    private Optional<KafkaMonitoringSnapshot> fetchSnapshot(String clusterId) {
        validateCluster(clusterId);

        if (!kafkaProperties.isEnabled()) {
            return Optional.empty();
        }

        try {
            return Optional.of(kafkaMonitoringPort.fetchSnapshot(clusterId));
        } catch (KafkaUnavailableException exception) {
            return Optional.empty();
        }
    }

    private KafkaOverview toOverview(KafkaMonitoringSnapshot snapshot) {
        long totalLag = snapshot.consumerGroups().stream()
                .mapToLong(KafkaConsumerGroupSummary::totalLag)
                .sum();
        int laggingConsumerGroups = (int) snapshot.consumerGroups().stream()
                .filter(group -> group.totalLag() > 0)
                .count();

        return new KafkaOverview(
                snapshot.clusterId(),
                true,
                getMostSevereStatus(snapshot),
                getOverviewReason(snapshot),
                snapshot.brokerCount(),
                snapshot.topics().size(),
                snapshot.consumerGroups().size(),
                totalLag,
                laggingConsumerGroups,
                snapshot.collectedAt()
        );
    }

    private ResourceStatus getMostSevereStatus(KafkaMonitoringSnapshot snapshot) {
        return snapshot.topics().stream()
                .map(KafkaTopicSummary::status)
                .max(Comparator.comparing(this::statusRank))
                .map(topicStatus -> snapshot.consumerGroups().stream()
                        .map(KafkaConsumerGroupSummary::status)
                        .max(Comparator.comparing(this::statusRank))
                        .filter(groupStatus -> statusRank(groupStatus) > statusRank(topicStatus))
                        .orElse(topicStatus))
                .orElseGet(() -> snapshot.consumerGroups().stream()
                        .map(KafkaConsumerGroupSummary::status)
                        .max(Comparator.comparing(this::statusRank))
                        .orElse(ResourceStatus.HEALTHY));
    }

    private String getOverviewReason(KafkaMonitoringSnapshot snapshot) {
        if (snapshot.brokerCount() == 0) {
            return "KAFKA_NO_BROKERS";
        }

        ResourceStatus status = getMostSevereStatus(snapshot);

        return switch (status) {
            case CRITICAL -> "KAFKA_CRITICAL_LAG_OR_PARTITION";
            case WARNING -> "KAFKA_WARNING_LAG_OR_PARTITION";
            case UNKNOWN -> "KAFKA_STATUS_UNKNOWN";
            case HEALTHY -> "KAFKA_CONNECTED";
        };
    }

    private int statusRank(ResourceStatus status) {
        return switch (status) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            case UNKNOWN -> 1;
            case HEALTHY -> 0;
        };
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LAG_LIMIT;
        }

        return Math.min(limit, 20);
    }

    private void validateCluster(String clusterId) {
        if (!kubernetesProperties.getClusterId().equals(clusterId)) {
            throw new UnknownClusterException(clusterId);
        }
    }
}
