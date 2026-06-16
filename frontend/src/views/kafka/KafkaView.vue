<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import {
  useKafkaConsumerGroupsQuery,
  useKafkaOverviewQuery,
  useKafkaTopicsQuery,
  useKafkaTopConsumerGroupLagsQuery,
} from '../../composables/queries/useKafkaQueries'
import { translateResourceReason, translateResourceStatus } from '../../i18n/messages'
import { useClusterSelectionStore } from '../../stores/clusterSelection'
import { useUiPreferencesStore } from '../../stores/uiPreferences'
import type { ResourceStatus } from '../../types/kubernetes'

const selectionStore = useClusterSelectionStore()
const { clusterId } = storeToRefs(selectionStore)
const uiPreferences = useUiPreferencesStore()
const { locale } = storeToRefs(uiPreferences)
const { t } = uiPreferences

const overviewQuery = useKafkaOverviewQuery(clusterId)
const topicsQuery = useKafkaTopicsQuery(clusterId)
const consumerGroupsQuery = useKafkaConsumerGroupsQuery(clusterId)
const topLagQuery = useKafkaTopConsumerGroupLagsQuery(clusterId, 5)

const overview = computed(() => overviewQuery.data.value)
const topics = computed(() => topicsQuery.data.value ?? [])
const consumerGroups = computed(() => consumerGroupsQuery.data.value ?? [])
const topLag = computed(() => topLagQuery.data.value ?? [])

const isPending = computed(
  () =>
    overviewQuery.isPending.value ||
    topicsQuery.isPending.value ||
    consumerGroupsQuery.isPending.value ||
    topLagQuery.isPending.value,
)

const isError = computed(
  () =>
    overviewQuery.isError.value ||
    topicsQuery.isError.value ||
    consumerGroupsQuery.isError.value ||
    topLagQuery.isError.value,
)

const summaryCards = computed(() => [
  {
    title: t('kafka.brokers'),
    value: String(overview.value?.brokerCount ?? 0),
    caption: t('kafka.connectedBrokers'),
    tone: overview.value?.available ? 'healthy' : 'unknown',
  },
  {
    title: t('kafka.topics'),
    value: String(overview.value?.topicCount ?? 0),
    caption: t('kafka.topicMetadata'),
    tone: getTopicSummaryStatus(),
  },
  {
    title: t('kafka.consumerGroups'),
    value: String(overview.value?.consumerGroupCount ?? 0),
    caption: t('kafka.groupOffsets'),
    tone: getConsumerGroupSummaryStatus(),
  },
  {
    title: t('kafka.totalLag'),
    value: formatLag(overview.value?.totalLag ?? 0),
    caption: laggingGroupCaption(overview.value?.laggingConsumerGroupCount ?? 0),
    tone: overview.value?.status ?? 'unknown',
  },
])

function getTopicSummaryStatus(): ResourceStatus {
  if (!topics.value.length) {
    return overview.value?.available ? 'healthy' : 'unknown'
  }

  return mostSevere(topics.value.map((topic) => topic.status))
}

function getConsumerGroupSummaryStatus(): ResourceStatus {
  if (!consumerGroups.value.length) {
    return overview.value?.available ? 'healthy' : 'unknown'
  }

  return mostSevere(consumerGroups.value.map((group) => group.status))
}

function mostSevere(statuses: ResourceStatus[]): ResourceStatus {
  const rank: Record<ResourceStatus, number> = {
    healthy: 0,
    unknown: 1,
    warning: 2,
    critical: 3,
  }

  return statuses.sort((left, right) => rank[right] - rank[left])[0] ?? 'unknown'
}

function statusClass(status: ResourceStatus) {
  return `resource-status status-${status}`
}

function statusLabel(status: ResourceStatus) {
  return translateResourceStatus(locale.value, status)
}

function reasonLabel(reason?: string) {
  return translateResourceReason(locale.value, reason ?? '-')
}

function formatLag(value?: number) {
  return new Intl.NumberFormat(locale.value === 'ko' ? 'ko-KR' : 'en-US').format(value ?? 0)
}

function laggingGroupCaption(count: number) {
  return locale.value === 'ko' ? `lag 발생 group ${count}개` : `${count} lagging groups`
}

function formatOffset(value?: number) {
  return value === undefined || value === null ? '-' : formatLag(value)
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ t('kafka.eyebrow') }}</p>
        <h1>{{ t('kafka.title') }}</h1>
        <p class="subtitle">{{ t('kafka.subtitle') }}</p>
      </div>
    </div>

    <article v-if="isError" class="panel state-panel status-critical">
      <h2>{{ t('kafka.apiUnavailable') }}</h2>
      <p>{{ t('kafka.apiUnavailableDescription') }}</p>
    </article>

    <article v-else-if="isPending" class="panel state-panel">
      <h2>{{ t('kafka.loading') }}</h2>
      <p>{{ t('kafka.loadingDescription') }}</p>
    </article>

    <article v-else-if="overview && !overview.available" class="panel state-panel status-warning">
      <h2>{{ t('kafka.unavailable') }}</h2>
      <p>{{ reasonLabel(overview.reason) }}</p>
    </article>

    <template v-else>
      <div class="inventory-summary">
        <article v-for="card in summaryCards" :key="card.title" class="summary-card">
          <p>{{ card.title }}</p>
          <strong :class="`tone-${card.tone}`">{{ card.value }}</strong>
          <small>{{ card.caption }}</small>
        </article>
      </div>

      <article class="panel inventory-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">{{ t('kafka.lagRanking') }}</p>
            <h2>{{ t('kafka.consumerLag') }}</h2>
          </div>
        </div>
        <p v-if="!topLag.length" class="empty-state">{{ t('kafka.noLag') }}</p>
        <table v-else>
          <thead>
            <tr>
              <th>{{ t('kafka.consumerGroup') }}</th>
              <th>{{ t('table.status') }}</th>
              <th>{{ t('kafka.totalLag') }}</th>
              <th>{{ t('kafka.partitions') }}</th>
              <th>{{ t('table.reason') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="groupLag in topLag" :key="groupLag.groupId">
              <td>{{ groupLag.groupId }}</td>
              <td><span :class="statusClass(groupLag.status)">{{ statusLabel(groupLag.status) }}</span></td>
              <td>{{ formatLag(groupLag.totalLag) }}</td>
              <td>{{ groupLag.partitions.length }}</td>
              <td>{{ reasonLabel(groupLag.reason) }}</td>
            </tr>
          </tbody>
        </table>
      </article>

      <div class="panel-grid kafka-detail-grid">
        <article class="panel inventory-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Topic</p>
              <h2>{{ t('kafka.topics') }}</h2>
            </div>
          </div>
          <p v-if="!topics.length" class="empty-state">{{ t('kafka.noTopics') }}</p>
          <table v-else>
            <thead>
              <tr>
                <th>{{ t('table.name') }}</th>
                <th>{{ t('table.status') }}</th>
                <th>{{ t('kafka.partitions') }}</th>
                <th>{{ t('kafka.replicationFactor') }}</th>
                <th>{{ t('kafka.underReplicated') }}</th>
                <th>{{ t('kafka.offlinePartitions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="topic in topics" :key="topic.name">
                <td>{{ topic.name }}</td>
                <td><span :class="statusClass(topic.status)">{{ statusLabel(topic.status) }}</span></td>
                <td>{{ topic.partitionCount }}</td>
                <td>{{ topic.replicationFactor }}</td>
                <td>{{ topic.underReplicatedPartitionCount }}</td>
                <td>{{ topic.offlinePartitionCount }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="panel inventory-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">{{ t('kafka.consumerGroup') }}</p>
              <h2>{{ t('kafka.consumerGroups') }}</h2>
            </div>
          </div>
          <p v-if="!consumerGroups.length" class="empty-state">{{ t('kafka.noConsumerGroups') }}</p>
          <table v-else>
            <thead>
              <tr>
                <th>{{ t('kafka.consumerGroup') }}</th>
                <th>{{ t('table.status') }}</th>
                <th>{{ t('kafka.state') }}</th>
                <th>{{ t('kafka.members') }}</th>
                <th>{{ t('kafka.totalLag') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="group in consumerGroups" :key="group.groupId">
                <td>{{ group.groupId }}</td>
                <td><span :class="statusClass(group.status)">{{ statusLabel(group.status) }}</span></td>
                <td>{{ group.state }}</td>
                <td>{{ group.memberCount }}</td>
                <td>{{ formatLag(group.totalLag) }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </div>

      <article v-if="topLag[0]?.partitions.length" class="panel inventory-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">{{ topLag[0].groupId }}</p>
            <h2>{{ t('kafka.partitionLag') }}</h2>
          </div>
        </div>
        <table>
          <thead>
            <tr>
              <th>Topic</th>
              <th>Partition</th>
              <th>{{ t('kafka.currentOffset') }}</th>
              <th>{{ t('kafka.logEndOffset') }}</th>
              <th>{{ t('kafka.lag') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="partition in topLag[0].partitions" :key="`${partition.topic}:${partition.partition}`">
              <td>{{ partition.topic }}</td>
              <td>{{ partition.partition }}</td>
              <td>{{ formatOffset(partition.currentOffset) }}</td>
              <td>{{ formatOffset(partition.logEndOffset) }}</td>
              <td>{{ formatLag(partition.lag) }}</td>
            </tr>
          </tbody>
        </table>
      </article>
    </template>
  </section>
</template>
