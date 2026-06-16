package com.opspilot.kafka.adapter.out.admin;

import com.opspilot.kafka.application.KafkaUnavailableException;
import com.opspilot.kafka.application.port.out.KafkaMonitoringPort;
import com.opspilot.kafka.config.OpspilotKafkaProperties;
import com.opspilot.kafka.domain.KafkaConsumerGroupLag;
import com.opspilot.kafka.domain.KafkaConsumerGroupSummary;
import com.opspilot.kafka.domain.KafkaMonitoringSnapshot;
import com.opspilot.kafka.domain.KafkaPartitionLag;
import com.opspilot.kafka.domain.KafkaPartitionSummary;
import com.opspilot.kafka.domain.KafkaTopicSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "opspilot.kafka", name = "mode", havingValue = "admin", matchIfMissing = true)
public class KafkaAdminMonitoringAdapter implements KafkaMonitoringPort {

    private static final String KAFKA_ADMIN_QUERY_FAILED = "KAFKA_ADMIN_QUERY_FAILED";

    private final AdminClient adminClient;
    private final OpspilotKafkaProperties kafkaProperties;
    private final Clock kafkaClock;

    public KafkaAdminMonitoringAdapter(
            AdminClient adminClient,
            OpspilotKafkaProperties kafkaProperties,
            Clock kafkaClock
    ) {
        this.adminClient = adminClient;
        this.kafkaProperties = kafkaProperties;
        this.kafkaClock = kafkaClock;
    }

    @Override
    public KafkaMonitoringSnapshot fetchSnapshot(String clusterId) {
        try {
            Instant collectedAt = kafkaClock.instant();
            int brokerCount = await(adminClient.describeCluster().nodes()).size();
            List<KafkaTopicSummary> topics = fetchTopics();
            List<String> groupIds = fetchConsumerGroupIds();
            Map<String, ConsumerGroupDescription> groupDescriptions = fetchConsumerGroupDescriptions(groupIds);
            List<KafkaConsumerGroupLag> groupLags = groupIds.stream()
                    .map(groupId -> fetchConsumerGroupLag(groupId, groupDescriptions.get(groupId), collectedAt))
                    .sorted(Comparator.comparing(KafkaConsumerGroupLag::totalLag).reversed())
                    .toList();
            Map<String, KafkaConsumerGroupLag> lagsByGroupId = groupLags.stream()
                    .collect(Collectors.toMap(KafkaConsumerGroupLag::groupId, Function.identity()));
            List<KafkaConsumerGroupSummary> consumerGroups = groupIds.stream()
                    .map(groupId -> toConsumerGroupSummary(groupId, groupDescriptions.get(groupId), lagsByGroupId.get(groupId)))
                    .sorted(Comparator.comparing(KafkaConsumerGroupSummary::totalLag).reversed()
                            .thenComparing(KafkaConsumerGroupSummary::groupId))
                    .toList();

            return new KafkaMonitoringSnapshot(
                    clusterId,
                    collectedAt,
                    brokerCount,
                    topics,
                    consumerGroups,
                    groupLags
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaUnavailableException(KAFKA_ADMIN_QUERY_FAILED, exception);
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            throw new KafkaUnavailableException(KAFKA_ADMIN_QUERY_FAILED, exception);
        }
    }

    private List<KafkaTopicSummary> fetchTopics() throws ExecutionException, InterruptedException, TimeoutException {
        Set<String> topicNames = await(adminClient.listTopics(new ListTopicsOptions().listInternal(false)).names());

        if (topicNames.isEmpty()) {
            return List.of();
        }

        Map<String, TopicDescription> descriptions = await(adminClient.describeTopics(topicNames).allTopicNames());

        return descriptions.values().stream()
                .map(this::toTopicSummary)
                .sorted(Comparator.comparing(KafkaTopicSummary::name))
                .toList();
    }

    private List<String> fetchConsumerGroupIds() throws ExecutionException, InterruptedException, TimeoutException {
        Collection<ConsumerGroupListing> groups = await(adminClient.listConsumerGroups().all());

        return groups.stream()
                .map(ConsumerGroupListing::groupId)
                .sorted()
                .toList();
    }

    private Map<String, ConsumerGroupDescription> fetchConsumerGroupDescriptions(List<String> groupIds)
            throws ExecutionException, InterruptedException, TimeoutException {
        if (groupIds.isEmpty()) {
            return Map.of();
        }

        return await(adminClient.describeConsumerGroups(groupIds).all());
    }

    private KafkaTopicSummary toTopicSummary(TopicDescription topicDescription) {
        List<KafkaPartitionSummary> partitions = topicDescription.partitions().stream()
                .map(partitionInfo -> toPartitionSummary(topicDescription.name(), partitionInfo))
                .sorted(Comparator.comparing(KafkaPartitionSummary::partition))
                .toList();
        int underReplicatedCount = (int) partitions.stream()
                .filter(KafkaPartitionSummary::underReplicated)
                .count();
        int offlineCount = (int) partitions.stream()
                .filter(KafkaPartitionSummary::offline)
                .count();

        return new KafkaTopicSummary(
                topicDescription.name(),
                partitions.size(),
                partitions.stream()
                        .findFirst()
                        .map(partition -> partition.replicas().size())
                        .orElse(0),
                underReplicatedCount,
                offlineCount,
                getTopicStatus(underReplicatedCount, offlineCount),
                partitions
        );
    }

    private KafkaPartitionSummary toPartitionSummary(String topicName, TopicPartitionInfo partitionInfo) {
        List<Integer> replicas = partitionInfo.replicas().stream().map(Node::id).toList();
        List<Integer> inSyncReplicas = partitionInfo.isr().stream().map(Node::id).toList();
        Integer leaderId = partitionInfo.leader() == null || partitionInfo.leader().id() < 0
                ? null
                : partitionInfo.leader().id();
        boolean offline = leaderId == null;
        boolean underReplicated = inSyncReplicas.size() < replicas.size();

        return new KafkaPartitionSummary(
                topicName,
                partitionInfo.partition(),
                leaderId,
                replicas,
                inSyncReplicas,
                underReplicated,
                offline,
                offline ? ResourceStatus.CRITICAL : underReplicated ? ResourceStatus.WARNING : ResourceStatus.HEALTHY
        );
    }

    private ResourceStatus getTopicStatus(int underReplicatedCount, int offlineCount) {
        if (offlineCount > 0) {
            return ResourceStatus.CRITICAL;
        }

        if (underReplicatedCount > 0) {
            return ResourceStatus.WARNING;
        }

        return ResourceStatus.HEALTHY;
    }

    private KafkaConsumerGroupLag fetchConsumerGroupLag(
            String groupId,
            ConsumerGroupDescription groupDescription,
            Instant collectedAt
    ) {
        try {
            Map<TopicPartition, OffsetAndMetadata> committedOffsets = await(
                    adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata()
            );
            Map<TopicPartition, OffsetSpec> latestOffsetSpecs = committedOffsets.keySet().stream()
                    .collect(Collectors.toMap(Function.identity(), ignored -> OffsetSpec.latest()));
            Map<TopicPartition, ListOffsetsResultInfo> logEndOffsets = latestOffsetSpecs.isEmpty()
                    ? Map.of()
                    : await(adminClient.listOffsets(latestOffsetSpecs).all());

            List<KafkaPartitionLag> partitionLags = committedOffsets.entrySet().stream()
                    .map(entry -> toPartitionLag(entry.getKey(), entry.getValue(), logEndOffsets.get(entry.getKey())))
                    .sorted(Comparator.comparing(KafkaPartitionLag::topic).thenComparing(KafkaPartitionLag::partition))
                    .toList();
            long totalLag = partitionLags.stream().mapToLong(KafkaPartitionLag::lag).sum();

            return new KafkaConsumerGroupLag(
                    groupId,
                    getConsumerGroupStatus(totalLag, memberCount(groupDescription)),
                    getConsumerGroupReason(totalLag, memberCount(groupDescription)),
                    totalLag,
                    collectedAt,
                    partitionLags
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaUnavailableException(KAFKA_ADMIN_QUERY_FAILED, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaUnavailableException(KAFKA_ADMIN_QUERY_FAILED, exception);
        }
    }

    private KafkaPartitionLag toPartitionLag(
            TopicPartition topicPartition,
            OffsetAndMetadata offsetAndMetadata,
            ListOffsetsResultInfo logEndOffsetInfo
    ) {
        Long currentOffset = offsetAndMetadata == null ? null : offsetAndMetadata.offset();
        Long logEndOffset = logEndOffsetInfo == null ? null : logEndOffsetInfo.offset();
        long lag = currentOffset == null || logEndOffset == null
                ? 0
                : Math.max(0, logEndOffset - currentOffset);

        return new KafkaPartitionLag(
                topicPartition.topic(),
                topicPartition.partition(),
                currentOffset,
                logEndOffset,
                lag
        );
    }

    private KafkaConsumerGroupSummary toConsumerGroupSummary(
            String groupId,
            ConsumerGroupDescription groupDescription,
            KafkaConsumerGroupLag lag
    ) {
        List<KafkaPartitionLag> partitions = lag == null ? List.of() : lag.partitions();
        long totalLag = lag == null ? 0 : lag.totalLag();
        int memberCount = memberCount(groupDescription);

        return new KafkaConsumerGroupSummary(
                groupId,
                groupDescription == null ? "UNKNOWN" : groupDescription.state().toString(),
                memberCount,
                (int) partitions.stream().map(KafkaPartitionLag::topic).distinct().count(),
                partitions.size(),
                totalLag,
                getConsumerGroupStatus(totalLag, memberCount),
                getConsumerGroupReason(totalLag, memberCount)
        );
    }

    private int memberCount(ConsumerGroupDescription groupDescription) {
        return groupDescription == null ? 0 : groupDescription.members().size();
    }

    private ResourceStatus getConsumerGroupStatus(long totalLag, int memberCount) {
        if (totalLag >= kafkaProperties.getCriticalLagThreshold()) {
            return ResourceStatus.CRITICAL;
        }

        if (totalLag >= kafkaProperties.getWarningLagThreshold() || (memberCount == 0 && totalLag > 0)) {
            return ResourceStatus.WARNING;
        }

        return ResourceStatus.HEALTHY;
    }

    private String getConsumerGroupReason(long totalLag, int memberCount) {
        if (totalLag >= kafkaProperties.getCriticalLagThreshold()) {
            return "KAFKA_LAG_CRITICAL";
        }

        if (totalLag >= kafkaProperties.getWarningLagThreshold()) {
            return "KAFKA_LAG_WARNING";
        }

        if (memberCount == 0 && totalLag > 0) {
            return "KAFKA_CONSUMER_GROUP_INACTIVE";
        }

        return "KAFKA_CONSUMER_GROUP_HEALTHY";
    }

    private <T> T await(KafkaFuture<T> future) throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(kafkaProperties.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
}
