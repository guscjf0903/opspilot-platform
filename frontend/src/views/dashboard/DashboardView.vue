<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useApiHealthQuery } from '../../composables/queries/useApiHealthQuery'
import { useDashboardQuery } from '../../composables/queries/useDashboardQuery'
import { useNamespacesQuery } from '../../composables/queries/useKubernetesInventoryQueries'
import { translateResourceReason, translateResourceStatus } from '../../i18n/messages'
import { useClusterSelectionStore } from '../../stores/clusterSelection'
import { useUiPreferencesStore } from '../../stores/uiPreferences'
import type { DashboardExternalMetric, DashboardWorkloadSummary } from '../../types/dashboard'
import type { ResourceStatus } from '../../types/kubernetes'
import { toPlatformAvailability } from '../../types/platform'

const selectionStore = useClusterSelectionStore()
const { clusterId, dashboardNamespace } = storeToRefs(selectionStore)
const uiPreferences = useUiPreferencesStore()
const { locale } = storeToRefs(uiPreferences)
const { t } = uiPreferences
const healthQuery = useApiHealthQuery()
const namespacesQuery = useNamespacesQuery(clusterId)
const dashboardQuery = useDashboardQuery(clusterId, dashboardNamespace)

const apiAvailability = computed(() => {
  if (healthQuery.isError.value) {
    return 'unavailable'
  }

  return toPlatformAvailability(healthQuery.data.value?.status)
})

const apiStatusLabel = computed(() => {
  if (healthQuery.isPending.value) {
    return t('dashboard.apiChecking')
  }

  return {
    available: t('dashboard.apiAvailable'),
    unavailable: t('dashboard.apiUnavailable'),
    unknown: t('dashboard.apiUnknown'),
  }[apiAvailability.value]
})

const summary = computed(() => dashboardQuery.data.value)

const summaryCards = computed(() => [
  {
    title: t('dashboard.clusterHealth'),
    value: summary.value ? statusLabel(summary.value.clusterStatus) : '-',
    caption: t('dashboard.liveKubernetesData'),
    tone: summary.value?.clusterStatus ?? 'unknown',
  },
  {
    title: t('dashboard.unhealthyWorkloads'),
    value: String(
      (summary.value?.counts.criticalWorkloadCount ?? 0) + (summary.value?.counts.warningWorkloadCount ?? 0),
    ),
    caption: t('dashboard.criticalAndWarning'),
    tone: summary.value?.counts.criticalWorkloadCount
      ? 'critical'
      : summary.value?.counts.warningWorkloadCount
        ? 'warning'
        : 'healthy',
  },
  {
    title: t('dashboard.recentWarningEvents'),
    value: String(summary.value?.counts.recentWarningEventCount ?? 0),
    caption: t('dashboard.lastThirtyMinutes'),
    tone: summary.value?.counts.recentWarningEventCount ? 'warning' : 'healthy',
  },
  {
    title: t('dashboard.nodes'),
    value: String(summary.value?.counts.nodeCount ?? 0),
    caption: t('dashboard.connectedNodes'),
    tone: getNodeSummaryStatus(),
  },
])

function getNodeSummaryStatus(): ResourceStatus {
  const nodes = summary.value?.nodes ?? []

  if (nodes.some((node) => node.status === 'critical')) {
    return 'critical'
  }

  if (nodes.some((node) => node.status === 'warning')) {
    return 'warning'
  }

  if (nodes.some((node) => node.status === 'unknown')) {
    return 'unknown'
  }

  return nodes.length ? 'healthy' : 'unknown'
}

function statusClass(status: ResourceStatus) {
  return `resource-status status-${status}`
}

function statusLabel(status: ResourceStatus) {
  return translateResourceStatus(locale.value, status)
}

function reasonLabel(reason: string) {
  return translateResourceReason(locale.value, reason)
}

function workloadRoute(workload: DashboardWorkloadSummary) {
  return `/workloads/${workload.namespace}/${workload.kind.toLowerCase()}/${workload.name}`
}

function formatCpu(value?: number) {
  if (value === undefined || value === null) {
    return '-'
  }

  return `${value.toFixed(value >= 1 ? 2 : 3)} cores`
}

function formatBytes(value?: number) {
  if (value === undefined || value === null) {
    return '-'
  }

  const units = ['B', 'KiB', 'MiB', 'GiB']
  let normalizedValue = value
  let unitIndex = 0

  while (normalizedValue >= 1024 && unitIndex < units.length - 1) {
    normalizedValue /= 1024
    unitIndex += 1
  }

  return `${normalizedValue.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

function formatPercent(value?: number) {
  if (value === undefined || value === null) {
    return '-'
  }

  return `${value.toFixed(1)}%`
}

function nodeUsageDescription(metric: DashboardExternalMetric) {
  return `CPU ${formatPercent(metric.cpuUsagePercent)} (${formatCpu(metric.cpuUsageCores)}) / Memory ${formatPercent(
    metric.memoryUsagePercent,
  )} (${formatBytes(metric.memoryUsageBytes)})`
}

function kafkaLagDescription(metric: DashboardExternalMetric) {
  const lag = new Intl.NumberFormat(locale.value === 'ko' ? 'ko-KR' : 'en-US').format(metric.value ?? 0)

  return `${lag} ${metric.unit ?? 'messages'} / ${reasonLabel(metric.description ?? '-')}`
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ t('dashboard.eyebrow') }}</p>
        <h1>{{ t('dashboard.title') }}</h1>
        <p class="subtitle">{{ t('dashboard.subtitle') }}</p>
      </div>

      <div class="dashboard-heading-actions">
        <label class="namespace-filter">
          <span>Namespace</span>
          <select
            :value="dashboardNamespace"
            :disabled="namespacesQuery.isPending.value"
            @change="selectionStore.selectDashboardNamespace(($event.target as HTMLSelectElement).value)"
          >
            <option value="">{{ t('dashboard.allNamespaces') }}</option>
            <option v-for="item in namespacesQuery.data.value ?? []" :key="item.name" :value="item.name">
              {{ item.name }}
            </option>
          </select>
        </label>
        <div class="api-status" :class="`status-${apiAvailability}`">
          <span></span>
          {{ t('dashboard.api') }} {{ apiStatusLabel }}
        </div>
      </div>
    </div>

    <article v-if="dashboardQuery.isError.value" class="panel state-panel status-critical">
      <h2>{{ t('dashboard.unavailable') }}</h2>
      <p>{{ t('workloads.apiUnavailableDescription') }}</p>
    </article>

    <article v-else-if="dashboardQuery.isPending.value" class="panel state-panel">
      <h2>{{ t('dashboard.loading') }}</h2>
      <p>{{ t('dashboard.loadingDescription') }}</p>
    </article>

    <template v-else-if="summary">
      <div class="summary-grid">
        <article v-for="card in summaryCards" :key="card.title" class="summary-card">
          <p>{{ card.title }}</p>
          <strong :class="`tone-${card.tone}`">{{ card.value }}</strong>
          <small>{{ card.caption }}</small>
        </article>
      </div>

      <div class="dashboard-main-grid">
        <article class="panel dashboard-panel">
          <p class="eyebrow">{{ t('dashboard.attentionRequired') }}</p>
          <h2>{{ t('dashboard.unhealthyWorkloads') }}</h2>
          <p v-if="!summary.unhealthyWorkloads.length" class="empty-state">
            {{ t('dashboard.noUnhealthyWorkloads') }}
          </p>
          <ul v-else class="resource-list dashboard-list">
            <li v-for="workload in summary.unhealthyWorkloads" :key="`${workload.kind}:${workload.namespace}:${workload.name}`">
              <RouterLink :to="workloadRoute(workload)" class="resource-link">
                <div>
                  <strong>{{ workload.name }}</strong>
                  <small>{{ workload.namespace }} / {{ workload.kind }} / {{ reasonLabel(workload.reason) }}</small>
                </div>
                <span :class="statusClass(workload.status)">{{ statusLabel(workload.status) }}</span>
              </RouterLink>
            </li>
          </ul>
        </article>

        <article class="panel dashboard-panel">
          <p class="eyebrow">{{ t('workloads.recentSignals') }}</p>
          <h2>{{ t('dashboard.recentWarningEvents') }}</h2>
          <p v-if="!summary.recentWarningEvents.length" class="empty-state">
            {{ t('workloads.noWarningEvents') }}
          </p>
          <ul v-else class="resource-list event-list dashboard-list">
            <li v-for="event in summary.recentWarningEvents" :key="event.name">
              <div>
                <strong>{{ reasonLabel(event.reason) }}</strong>
                <small>{{ event.involvedKind }}/{{ event.involvedName }}</small>
              </div>
              <span :class="statusClass(event.status)">{{ statusLabel(event.status) }}</span>
            </li>
          </ul>
        </article>
      </div>

      <div class="dashboard-main-grid dashboard-secondary-grid">
        <article class="panel dashboard-panel">
          <p class="eyebrow">{{ t('dashboard.restartSignals') }}</p>
          <h2>{{ t('dashboard.restartCountTopPods') }}</h2>
          <p v-if="!summary.restartCountTopPods.length" class="empty-state">
            {{ t('dashboard.noRestartedPods') }}
          </p>
          <ul v-else class="resource-list dashboard-list">
            <li v-for="pod in summary.restartCountTopPods" :key="`${pod.namespace}:${pod.name}`">
              <RouterLink :to="`/workloads/${pod.namespace}/pod/${pod.name}`" class="resource-link">
                <div>
                  <strong>{{ pod.name }}</strong>
                  <small>{{ pod.namespace }} / {{ reasonLabel(pod.reason) }}</small>
                </div>
                <b>{{ pod.restartCount }}</b>
              </RouterLink>
            </li>
          </ul>
        </article>

        <article class="panel dashboard-panel">
          <p class="eyebrow">{{ t('dashboard.clusterNodes') }}</p>
          <h2>Nodes</h2>
          <p v-if="!summary.nodes.length" class="empty-state">{{ t('workloads.noNodes') }}</p>
          <ul v-else class="resource-list dashboard-list">
            <li v-for="node in summary.nodes" :key="node.name">
              <span>{{ node.name }}</span>
              <span :class="statusClass(node.status)">{{ statusLabel(node.status) }}</span>
            </li>
          </ul>
        </article>
      </div>

      <article class="panel dashboard-panel">
        <p class="eyebrow">{{ t('dashboard.namespaceOverview') }}</p>
        <h2>Namespaces</h2>
        <table>
          <thead>
            <tr>
              <th>{{ t('table.name') }}</th>
              <th>{{ t('table.status') }}</th>
              <th>Deployments</th>
              <th>Pods</th>
              <th>{{ t('dashboard.unhealthyWorkloads') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="namespace in summary.namespaces" :key="namespace.name">
              <td>{{ namespace.name }}</td>
              <td><span :class="statusClass(namespace.status)">{{ statusLabel(namespace.status) }}</span></td>
              <td>{{ namespace.deploymentCount }}</td>
              <td>{{ namespace.podCount }}</td>
              <td>{{ namespace.unhealthyWorkloadCount }}</td>
            </tr>
          </tbody>
        </table>
      </article>

      <div class="integration-grid">
        <article class="panel integration-card">
          <p class="eyebrow">
            {{
              summary.nodeUsage.status === 'available'
                ? t('dashboard.liveMetrics')
                : t('dashboard.followUpIntegration')
            }}
          </p>
          <h2>{{ t('dashboard.nodeUsage') }}</h2>
          <ul
            v-if="summary.nodeUsage.status === 'available' && summary.nodeUsage.metrics.length"
            class="resource-list dashboard-list metric-compact-list"
          >
            <li v-for="metric in summary.nodeUsage.metrics" :key="metric.name">
              <div>
                <strong>{{ metric.name }}</strong>
                <small>{{ nodeUsageDescription(metric) }}</small>
              </div>
            </li>
          </ul>
          <template v-else>
            <strong>{{ t('dashboard.integrationUnavailable') }}</strong>
            <small>{{ summary.nodeUsage.reason || t('dashboard.prometheusRequired') }}</small>
          </template>
        </article>
        <article class="panel integration-card">
          <p class="eyebrow">
            {{
              summary.kafkaLag.status === 'available'
                ? t('dashboard.kafkaMonitoring')
                : t('dashboard.followUpIntegration')
            }}
          </p>
          <h2>{{ t('dashboard.kafkaLag') }}</h2>
          <ul
            v-if="summary.kafkaLag.status === 'available' && summary.kafkaLag.metrics.length"
            class="resource-list dashboard-list metric-compact-list"
          >
            <li v-for="metric in summary.kafkaLag.metrics" :key="metric.name">
              <RouterLink to="/kafka" class="resource-link">
                <div>
                  <strong>{{ metric.name }}</strong>
                  <small>{{ kafkaLagDescription(metric) }}</small>
                </div>
                <span :class="statusClass(metric.status ?? 'unknown')">
                  {{ statusLabel(metric.status ?? 'unknown') }}
                </span>
              </RouterLink>
            </li>
          </ul>
          <template v-else-if="summary.kafkaLag.status === 'available'">
            <strong class="tone-healthy">{{ t('dashboard.noKafkaLag') }}</strong>
            <small>{{ reasonLabel(summary.kafkaLag.reason) }}</small>
          </template>
          <template v-else>
            <strong>{{ t('dashboard.integrationUnavailable') }}</strong>
            <small>{{ reasonLabel(summary.kafkaLag.reason) || t('dashboard.kafkaRequired') }}</small>
          </template>
        </article>
        <article class="panel integration-card">
          <p class="eyebrow">{{ t('dashboard.followUpIntegration') }}</p>
          <h2>{{ t('dashboard.costEstimate') }}</h2>
          <strong>{{ t('dashboard.integrationUnavailable') }}</strong>
          <small>{{ t('dashboard.openCostRequired') }}</small>
        </article>
      </div>
    </template>
  </section>
</template>
