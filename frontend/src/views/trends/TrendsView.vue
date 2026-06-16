<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { storeToRefs } from 'pinia'
import MetricLineChart from '../../components/metrics/MetricLineChart.vue'
import { useNamespacesQuery } from '../../composables/queries/useKubernetesInventoryQueries'
import {
  useCollectTrendSnapshotsMutation,
  useCostDailyTrendQuery,
  useKubernetesWorkloadUsageTrendQuery,
} from '../../composables/queries/useTrendQueries'
import { translateResourceStatus } from '../../i18n/messages'
import { useClusterSelectionStore } from '../../stores/clusterSelection'
import { useUiPreferencesStore } from '../../stores/uiPreferences'
import type { MetricPoint } from '../../types/metrics'
import type {
  CostDailySnapshot,
  KubernetesWorkloadUsageSnapshot,
  TrendSnapshotCollectionResult,
} from '../../types/trends'

const selectionStore = useClusterSelectionStore()
const { clusterId, namespace } = storeToRefs(selectionStore)
const uiPreferences = useUiPreferencesStore()
const { locale } = storeToRefs(uiPreferences)
const { t } = uiPreferences

const selectedNamespace = ref(namespace.value)
const selectedWorkloadName = ref('')
const allWorkloadsFilter = ref('')
const periodDays = ref(30)
const queryEndAt = ref(new Date())
const lastCollectionResult = ref<TrendSnapshotCollectionResult>()

const kubernetesFrom = computed(() => dateTimeDaysAgo(periodDays.value))
const kubernetesTo = computed(() => queryEndAt.value.toISOString())
const costFrom = computed(() => dateDaysAgo(periodDays.value))
const costTo = computed(() => dateOnly(queryEndAt.value))

const namespacesQuery = useNamespacesQuery(clusterId)
const kubernetesTrendQuery = useKubernetesWorkloadUsageTrendQuery(
  clusterId,
  selectedNamespace,
  selectedWorkloadName,
  kubernetesFrom,
  kubernetesTo,
)
const costTrendQuery = useCostDailyTrendQuery(
  clusterId,
  selectedNamespace,
  selectedWorkloadName,
  costFrom,
  costTo,
)
const kubernetesWorkloadOptionQuery = useKubernetesWorkloadUsageTrendQuery(
  clusterId,
  selectedNamespace,
  allWorkloadsFilter,
  kubernetesFrom,
  kubernetesTo,
)
const costWorkloadOptionQuery = useCostDailyTrendQuery(
  clusterId,
  selectedNamespace,
  allWorkloadsFilter,
  costFrom,
  costTo,
)
const collectMutation = useCollectTrendSnapshotsMutation(clusterId, selectedNamespace)

const kubernetesSnapshots = computed(() => kubernetesTrendQuery.data.value ?? [])
const costSnapshots = computed(() => costTrendQuery.data.value ?? [])
const kubernetesWorkloadOptionSnapshots = computed(
  () => kubernetesWorkloadOptionQuery.data.value ?? kubernetesSnapshots.value,
)
const costWorkloadOptionSnapshots = computed(() => costWorkloadOptionQuery.data.value ?? costSnapshots.value)
const isPending = computed(
  () => namespacesQuery.isPending.value || kubernetesTrendQuery.isPending.value || costTrendQuery.isPending.value,
)
const isError = computed(
  () => namespacesQuery.isError.value || kubernetesTrendQuery.isError.value || costTrendQuery.isError.value,
)
const workloadOptions = computed(() => {
  const names = new Set<string>()
  ;[...kubernetesWorkloadOptionSnapshots.value, ...costWorkloadOptionSnapshots.value].forEach((snapshot) =>
    names.add(snapshot.workloadName),
  )

  return [...names].sort()
})
const latestKubernetesSnapshots = computed(() =>
  [...kubernetesSnapshots.value]
    .sort((left, right) => new Date(right.collectedAt).getTime() - new Date(left.collectedAt).getTime())
    .slice(0, 8),
)
const latestCostSnapshots = computed(() =>
  [...costSnapshots.value]
    .sort((left, right) => right.snapshotDate.localeCompare(left.snapshotDate))
    .slice(0, 8),
)
const lastCollectedAt = computed(() => {
  const timestamps = [
    ...kubernetesSnapshots.value.map((snapshot) => snapshot.collectedAt),
    ...costSnapshots.value.map((snapshot) => snapshot.collectedAt),
  ]
    .map((timestamp) => new Date(timestamp).getTime())
    .filter(Number.isFinite)

  if (!timestamps.length) {
    return '-'
  }

  return formatDateTime(new Date(Math.max(...timestamps)).toISOString())
})

const cpuUsagePoints = computed(() => aggregateKubernetesPoints(kubernetesSnapshots.value, 'cpuUsagePercent'))
const memoryUsagePoints = computed(() => aggregateKubernetesPoints(kubernetesSnapshots.value, 'memoryUsagePercent'))
const monthlyCostPoints = computed(() => aggregateCostPoints(costSnapshots.value, 'estimatedMonthlyCost'))
const monthlySavingPoints = computed(() => aggregateCostPoints(costSnapshots.value, 'estimatedMonthlySaving'))
const cpuUsageEmptyLabel = computed(() => usageChartEmptyLabel(cpuUsagePoints.value.length))
const memoryUsageEmptyLabel = computed(() => usageChartEmptyLabel(memoryUsagePoints.value.length))
const monthlyCostEmptyLabel = computed(() => costChartEmptyLabel(monthlyCostPoints.value.length))
const monthlySavingEmptyLabel = computed(() => costChartEmptyLabel(monthlySavingPoints.value.length))
const collectionFeedback = computed(() => {
  if (collectMutation.isError.value) {
    return {
      className: 'trend-feedback status-critical',
      title: t('trends.saveFailed'),
      message: t('trends.saveFailedDescription'),
    }
  }

  if (lastCollectionResult.value) {
    return {
      className: 'trend-feedback status-healthy',
      title: t('trends.saveSucceeded'),
      message: t('trends.saveSucceededDescription')
        .replace('{usageCount}', String(lastCollectionResult.value.kubernetesSnapshotCount))
        .replace('{costCount}', String(lastCollectionResult.value.costSnapshotCount)),
    }
  }

  return undefined
})

watchEffect(() => {
  const namespaces = namespacesQuery.data.value ?? []

  if (
    selectedNamespace.value &&
    namespaces.length > 0 &&
    !namespaces.some((item) => item.name === selectedNamespace.value)
  ) {
    selectedNamespace.value = ''
  }
})

watchEffect(() => {
  if (
    selectedWorkloadName.value &&
    workloadOptions.value.length > 0 &&
    !workloadOptions.value.includes(selectedWorkloadName.value)
  ) {
    selectedWorkloadName.value = ''
  }
})

function collectSnapshots() {
  collectMutation.mutate(undefined, {
    onSuccess: (result) => {
      lastCollectionResult.value = result
      queryEndAt.value = new Date(new Date(result.collectedAt).getTime() + 1000)
    },
  })
}

function usageChartEmptyLabel(pointCount: number) {
  if (kubernetesSnapshots.value.length === 0) {
    return t('trends.noUsageRecords')
  }

  if (pointCount === 0) {
    return t('trends.noMetricValues')
  }

  return t('trends.needMoreUsageRecords')
}

function costChartEmptyLabel(pointCount: number) {
  if (costSnapshots.value.length === 0) {
    return t('trends.noCostRecords')
  }

  if (pointCount < 2) {
    return t('trends.needMoreCostRecords')
  }

  return t('trends.noCostRecords')
}

function dateTimeDaysAgo(days: number) {
  const date = new Date(queryEndAt.value)
  date.setDate(date.getDate() - days)

  return date.toISOString()
}

function dateDaysAgo(days: number) {
  const date = new Date(queryEndAt.value)
  date.setDate(date.getDate() - days)

  return dateOnly(date)
}

function dateOnly(date: Date) {
  return date.toISOString().slice(0, 10)
}

function aggregateKubernetesPoints(
  snapshots: KubernetesWorkloadUsageSnapshot[],
  field: 'cpuUsagePercent' | 'memoryUsagePercent',
): MetricPoint[] {
  const valuesByTimestamp = new Map<string, number[]>()

  snapshots.forEach((snapshot) => {
    const value = snapshot[field]

    if (value === undefined || value === null || !Number.isFinite(value)) {
      return
    }

    valuesByTimestamp.set(snapshot.collectedAt, [...(valuesByTimestamp.get(snapshot.collectedAt) ?? []), value])
  })

  return [...valuesByTimestamp.entries()]
    .sort(([left], [right]) => new Date(left).getTime() - new Date(right).getTime())
    .map(([timestamp, values]) => ({
      timestamp,
      value: average(values),
    }))
}

function aggregateCostPoints(
  snapshots: CostDailySnapshot[],
  field: 'estimatedMonthlyCost' | 'estimatedMonthlySaving',
): MetricPoint[] {
  const valuesByDate = new Map<string, number>()

  snapshots.forEach((snapshot) => {
    valuesByDate.set(snapshot.snapshotDate, (valuesByDate.get(snapshot.snapshotDate) ?? 0) + snapshot[field])
  })

  return [...valuesByDate.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([date, value]) => ({
      timestamp: `${date}T00:00:00.000Z`,
      value,
    }))
}

function average(values: number[]) {
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

function formatPercent(value?: number) {
  if (value === undefined || value === null) {
    return '-'
  }

  return `${value.toFixed(1)}%`
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

function formatMoney(value?: number, currency = 'USD') {
  const localeName = locale.value === 'ko' ? 'ko-KR' : 'en-US'

  return new Intl.NumberFormat(localeName, {
    style: 'currency',
    currency,
    maximumFractionDigits: Math.abs(value ?? 0) >= 100 ? 0 : 2,
  }).format(value ?? 0)
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat(locale.value === 'ko' ? 'ko-KR' : 'en-US', {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function statusClass(status: string) {
  return `resource-status status-${status}`
}

function statusLabel(status: KubernetesWorkloadUsageSnapshot['status']) {
  return translateResourceStatus(locale.value, status)
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ t('trends.eyebrow') }}</p>
        <h1>{{ t('trends.title') }}</h1>
        <p class="subtitle">{{ t('trends.subtitle') }}</p>
        <p class="trend-help-text">{{ t('trends.helpText') }}</p>
        <p class="trend-help-text">{{ t('trends.aggregationHelp') }}</p>
      </div>

      <div class="trend-heading-actions">
        <label class="namespace-filter">
          <span>{{ t('trends.period') }}</span>
          <select v-model="periodDays">
            <option :value="30">{{ t('trends.last30Days') }}</option>
            <option :value="90">{{ t('trends.last90Days') }}</option>
            <option :value="365">{{ t('trends.last365Days') }}</option>
          </select>
        </label>
        <label class="namespace-filter">
          <span>Namespace</span>
          <select v-model="selectedNamespace" :disabled="namespacesQuery.isPending.value">
            <option value="">{{ t('trends.allNamespaces') }}</option>
            <option v-for="item in namespacesQuery.data.value ?? []" :key="item.name" :value="item.name">
              {{ item.name }}
            </option>
          </select>
        </label>
        <label class="namespace-filter">
          <span>{{ t('trends.workloadFilter') }}</span>
          <select v-model="selectedWorkloadName">
            <option value="">{{ t('trends.allWorkloads') }}</option>
            <option v-for="name in workloadOptions" :key="name" :value="name">
              {{ name }}
            </option>
          </select>
        </label>
        <button
          type="button"
          class="primary-action-button"
          :disabled="collectMutation.isPending.value"
          @click="collectSnapshots"
        >
          {{ collectMutation.isPending.value ? t('trends.collecting') : t('trends.collect') }}
        </button>
      </div>
    </div>

    <article v-if="collectionFeedback" class="panel trend-feedback-panel" :class="collectionFeedback.className">
      <strong>{{ collectionFeedback.title }}</strong>
      <p>{{ collectionFeedback.message }}</p>
    </article>

    <article v-if="isError" class="panel state-panel status-critical">
      <h2>{{ t('trends.apiUnavailable') }}</h2>
      <p>{{ t('trends.apiUnavailableDescription') }}</p>
    </article>

    <article v-else-if="isPending" class="panel state-panel">
      <h2>{{ t('trends.loading') }}</h2>
      <p>{{ t('trends.loadingDescription') }}</p>
    </article>

    <template v-else>
      <div class="inventory-summary">
        <article class="summary-card">
          <p>{{ t('trends.kubernetesSnapshots') }}</p>
          <strong>{{ kubernetesSnapshots.length }}</strong>
          <small>{{ t('trends.usageRecordHelp') }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('trends.costSnapshots') }}</p>
          <strong>{{ costSnapshots.length }}</strong>
          <small>{{ t('trends.costRecordHelp') }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('trends.latestCollection') }}</p>
          <strong>{{ lastCollectedAt }}</strong>
          <small>{{ t('trends.latestCollectionHelp') }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('trends.selectedNamespace') }}</p>
          <strong>{{ selectedNamespace || t('trends.allNamespaces') }}</strong>
          <small>{{ t('trends.selectedWorkload') }}: {{ selectedWorkloadName || t('trends.allWorkloads') }}</small>
        </article>
      </div>

      <div class="metric-chart-grid trend-chart-grid">
        <MetricLineChart
          :title="t('trends.cpuUsageRatio')"
          unit-label="%"
          :points="cpuUsagePoints"
          :format-value="formatPercent"
          :empty-label="cpuUsageEmptyLabel"
        />
        <MetricLineChart
          :title="t('trends.memoryUsageRatio')"
          unit-label="%"
          :points="memoryUsagePoints"
          :format-value="formatPercent"
          :empty-label="memoryUsageEmptyLabel"
        />
        <MetricLineChart
          :title="t('trends.monthlyCost')"
          unit-label="USD"
          :points="monthlyCostPoints"
          :format-value="(value) => formatMoney(value)"
          :empty-label="monthlyCostEmptyLabel"
        />
        <MetricLineChart
          :title="t('trends.monthlySaving')"
          unit-label="USD"
          :points="monthlySavingPoints"
          :format-value="(value) => formatMoney(value)"
          :empty-label="monthlySavingEmptyLabel"
        />
      </div>

      <div class="panel-grid trend-data-grid">
        <article class="panel inventory-panel">
          <p class="eyebrow">{{ t('trends.kubernetesUsage') }}</p>
          <h2>{{ t('trends.latestWorkloadUsage') }}</h2>
          <p v-if="!latestKubernetesSnapshots.length" class="empty-state">
            {{ t('trends.noUsageRecords') }}
          </p>
          <table v-else>
            <thead>
              <tr>
                <th>{{ t('table.name') }}</th>
                <th>{{ t('table.status') }}</th>
                <th>{{ t('trends.cpuRequest') }}</th>
                <th>{{ t('trends.cpuUsage') }}</th>
                <th>{{ t('trends.memoryRequest') }}</th>
                <th>{{ t('trends.memoryUsage') }}</th>
                <th>{{ t('trends.collectedAt') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="snapshot in latestKubernetesSnapshots" :key="snapshot.id">
                <td>{{ snapshot.namespace }}/{{ snapshot.workloadName }}</td>
                <td><span :class="statusClass(snapshot.status)">{{ statusLabel(snapshot.status) }}</span></td>
                <td>{{ formatCpu(snapshot.cpuRequestCores) }}</td>
                <td>{{ formatPercent(snapshot.cpuUsagePercent) }}</td>
                <td>{{ formatBytes(snapshot.memoryRequestBytes) }}</td>
                <td>{{ formatPercent(snapshot.memoryUsagePercent) }}</td>
                <td>{{ formatDateTime(snapshot.collectedAt) }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="panel inventory-panel">
          <p class="eyebrow">{{ t('trends.costTrend') }}</p>
          <h2>{{ t('trends.latestCostSnapshots') }}</h2>
          <p v-if="!latestCostSnapshots.length" class="empty-state">
            {{ t('trends.noCostRecords') }}
          </p>
          <table v-else>
            <thead>
              <tr>
                <th>{{ t('table.name') }}</th>
                <th>{{ t('trends.snapshotDate') }}</th>
                <th>{{ t('trends.monthlyCost') }}</th>
                <th>{{ t('trends.dailyCost') }}</th>
                <th>{{ t('trends.monthlySaving') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="snapshot in latestCostSnapshots" :key="snapshot.id">
                <td>{{ snapshot.namespace }}/{{ snapshot.workloadName }}</td>
                <td>{{ snapshot.snapshotDate }}</td>
                <td>{{ formatMoney(snapshot.estimatedMonthlyCost, snapshot.currency) }}</td>
                <td>{{ formatMoney(snapshot.estimatedDailyCost, snapshot.currency) }}</td>
                <td class="tone-good">{{ formatMoney(snapshot.estimatedMonthlySaving, snapshot.currency) }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </div>
    </template>
  </section>
</template>
