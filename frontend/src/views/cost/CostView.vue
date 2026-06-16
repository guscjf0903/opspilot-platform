<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { storeToRefs } from 'pinia'
import {
  useCostRecommendationsQuery,
  useCostSummaryQuery,
  useNamespaceCostsQuery,
  useWorkloadCostsQuery,
} from '../../composables/queries/useCostQueries'
import { useNamespacesQuery } from '../../composables/queries/useKubernetesInventoryQueries'
import { translateResourceStatus } from '../../i18n/messages'
import { useClusterSelectionStore } from '../../stores/clusterSelection'
import { useUiPreferencesStore } from '../../stores/uiPreferences'
import type { CostRecommendation, CostRecommendationType, CostRiskLevel } from '../../types/cost'
import type { ResourceStatus } from '../../types/kubernetes'

const selectionStore = useClusterSelectionStore()
const { clusterId, namespace } = storeToRefs(selectionStore)
const selectedNamespace = ref(namespace.value)
const uiPreferences = useUiPreferencesStore()
const { locale } = storeToRefs(uiPreferences)
const { t } = uiPreferences

const namespacesQuery = useNamespacesQuery(clusterId)
const costSummaryQuery = useCostSummaryQuery(clusterId, selectedNamespace)
const namespaceCostsQuery = useNamespaceCostsQuery(clusterId, selectedNamespace)
const workloadCostsQuery = useWorkloadCostsQuery(clusterId, selectedNamespace)
const recommendationsQuery = useCostRecommendationsQuery(clusterId, selectedNamespace)

const summary = computed(() => costSummaryQuery.data.value)
const recommendations = computed(() => recommendationsQuery.data.value ?? [])
const topRecommendations = computed(() => recommendations.value.slice(0, 5))
const isPending = computed(
  () =>
    namespacesQuery.isPending.value ||
    costSummaryQuery.isPending.value ||
    namespaceCostsQuery.isPending.value ||
    workloadCostsQuery.isPending.value ||
    recommendationsQuery.isPending.value,
)
const isError = computed(
  () =>
    namespacesQuery.isError.value ||
    costSummaryQuery.isError.value ||
    namespaceCostsQuery.isError.value ||
    workloadCostsQuery.isError.value ||
    recommendationsQuery.isError.value,
)

watchEffect(() => {
  const namespaces = namespacesQuery.data.value ?? []

  if (
    selectedNamespace.value &&
    namespaces.length > 0 &&
    !namespaces.some((item) => item.name === selectedNamespace.value)
  ) {
    selectedNamespace.value = namespaces[0].name
  }
})

function formatMoney(value?: number, currency = summary.value?.currency ?? 'USD') {
  const localeName = locale.value === 'ko' ? 'ko-KR' : 'en-US'

  return new Intl.NumberFormat(localeName, {
    style: 'currency',
    currency,
    maximumFractionDigits: Math.abs(value ?? 0) >= 100 ? 0 : 2,
  }).format(value ?? 0)
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

function confidenceLabel(confidence: number) {
  return `${Math.round(confidence * 100)}%`
}

function statusClass(status: ResourceStatus) {
  return `resource-status status-${status}`
}

function statusLabel(status: ResourceStatus) {
  return translateResourceStatus(locale.value, status)
}

function riskLabel(risk: CostRiskLevel) {
  const labels: Record<CostRiskLevel, string> = {
    low: t('cost.risk.low'),
    medium: t('cost.risk.medium'),
    high: t('cost.risk.high'),
  }

  return labels[risk]
}

function riskClass(risk: CostRiskLevel) {
  return `cost-risk risk-${risk}`
}

function recommendationTypeLabel(type: CostRecommendationType) {
  const labels: Record<CostRecommendationType, string> = {
    CPU_RIGHTSIZING: t('cost.recommendation.cpuRightsizing'),
    MEMORY_RIGHTSIZING: t('cost.recommendation.memoryRightsizing'),
    IDLE_WORKLOAD: t('cost.recommendation.idleWorkload'),
  }

  return labels[type]
}

function estimationModeLabel(mode?: string) {
  const labels: Record<string, string> = {
    OPENCOST_ALLOCATION: t('cost.estimationMode.opencost'),
    OPENCOST_ALLOCATION_WITH_REQUEST_FALLBACK: t('cost.estimationMode.mixed'),
    REQUEST_BASED_LOCAL_ESTIMATE: t('cost.estimationMode.requestBased'),
  }

  return labels[mode ?? ''] ?? mode ?? '-'
}

function recordEntries(record: Record<string, string>) {
  return Object.entries(record)
}

function recommendationFieldLabel(key: string) {
  const labels: Record<string, string> = {
    action: t('cost.field.action'),
    cpuRequest: t('cost.field.cpuRequest'),
    cpuUsage: t('cost.field.cpuUsage'),
    cpuUsageRatio: t('cost.field.cpuUsageRatio'),
    estimatedMonthlyCost: t('cost.field.estimatedMonthlyCost'),
    memoryRequest: t('cost.field.memoryRequest'),
    memoryUsage: t('cost.field.memoryUsage'),
    memoryUsageRatio: t('cost.field.memoryUsageRatio'),
    usageRatio: t('cost.field.usageRatio'),
  }

  return labels[key] ?? key
}

function workloadRoute(workload: { namespace: string; kind: string; name: string }) {
  return `/workloads/${workload.namespace}/${workload.kind.toLowerCase()}/${workload.name}`
}

function recommendationRoute(recommendation: CostRecommendation) {
  return `/workloads/${recommendation.namespace}/${recommendation.targetKind.toLowerCase()}/${recommendation.targetName}`
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ t('cost.eyebrow') }}</p>
        <h1>{{ t('cost.title') }}</h1>
        <p class="subtitle">{{ t('cost.subtitle') }}</p>
      </div>

      <label class="namespace-filter">
        <span>Namespace</span>
        <select
          v-model="selectedNamespace"
          :disabled="namespacesQuery.isPending.value"
        >
          <option value="">{{ t('cost.allNamespaces') }}</option>
          <option v-for="item in namespacesQuery.data.value ?? []" :key="item.name" :value="item.name">
            {{ item.name }}
          </option>
        </select>
      </label>
    </div>

    <article v-if="isError" class="panel state-panel status-critical">
      <h2>{{ t('cost.apiUnavailable') }}</h2>
      <p>{{ t('cost.apiUnavailableDescription') }}</p>
    </article>

    <article v-else-if="isPending" class="panel state-panel">
      <h2>{{ t('cost.loading') }}</h2>
      <p>{{ t('cost.loadingDescription') }}</p>
    </article>

    <template v-else>
      <div class="inventory-summary cost-summary">
        <article class="summary-card">
          <p>{{ t('cost.monthlyCost') }}</p>
          <strong>{{ formatMoney(summary?.estimatedMonthlyCost, summary?.currency) }}</strong>
          <small>{{ estimationModeLabel(summary?.estimationMode) }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('cost.monthlySaving') }}</p>
          <strong class="tone-good">{{ formatMoney(summary?.estimatedMonthlySaving, summary?.currency) }}</strong>
          <small>{{ t('cost.estimatedFromRequests') }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('cost.recommendationCount') }}</p>
          <strong>{{ summary?.recommendationCount ?? 0 }}</strong>
          <small>{{ t('cost.metricsRequired') }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('cost.coverage') }}</p>
          <strong>{{ summary?.workloadCount ?? 0 }}</strong>
          <small>{{ summary?.namespaceCount ?? 0 }} Namespace</small>
        </article>
      </div>

      <article class="panel cost-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">{{ t('cost.recommendations') }}</p>
            <h2>{{ t('cost.recommendationTitle') }}</h2>
          </div>
        </div>
        <div class="cost-help-grid">
          <div>
            <strong>{{ t('cost.help.usageRatioTitle') }}</strong>
            <p>{{ t('cost.help.usageRatioDescription') }}</p>
          </div>
          <div>
            <strong>{{ t('cost.help.idleTitle') }}</strong>
            <p>{{ t('cost.help.idleDescription') }}</p>
          </div>
        </div>

        <p v-if="!topRecommendations.length" class="empty-state">{{ t('cost.noRecommendations') }}</p>
        <div v-else class="recommendation-grid">
          <RouterLink
            v-for="recommendation in topRecommendations"
            :key="recommendation.id"
            class="cost-recommendation-card"
            :to="recommendationRoute(recommendation)"
          >
            <div class="recommendation-card-heading">
              <div>
                <small>{{ recommendationTypeLabel(recommendation.type) }}</small>
                <strong>{{ recommendation.title }}</strong>
              </div>
              <span :class="riskClass(recommendation.risk)">{{ riskLabel(recommendation.risk) }}</span>
            </div>
            <p>{{ recommendation.reason }}</p>
            <div class="recommendation-meta">
              <span>{{ recommendation.namespace }}/{{ recommendation.targetName }}</span>
              <span>{{ t('aiAnalysis.confidence') }} {{ confidenceLabel(recommendation.confidence) }}</span>
              <strong>{{ formatMoney(recommendation.estimatedMonthlySaving, recommendation.currency) }}</strong>
            </div>
            <div class="recommendation-diff">
              <dl>
                <strong>{{ t('cost.currentValues') }}</strong>
                <div v-for="[key, value] in recordEntries(recommendation.current)" :key="key">
                  <dt>{{ recommendationFieldLabel(key) }}</dt>
                  <dd>{{ value }}</dd>
                </div>
              </dl>
              <dl>
                <strong>{{ t('cost.recommendedValues') }}</strong>
                <div v-for="[key, value] in recordEntries(recommendation.recommendation)" :key="key">
                  <dt>{{ recommendationFieldLabel(key) }}</dt>
                  <dd>{{ value }}</dd>
                </div>
              </dl>
            </div>
          </RouterLink>
        </div>
      </article>

      <div class="panel-grid cost-data-grid">
        <article class="panel inventory-panel">
          <p class="eyebrow">{{ t('cost.namespaceBreakdown') }}</p>
          <h2>Namespace</h2>
          <p v-if="!namespaceCostsQuery.data.value?.length" class="empty-state">
            {{ t('cost.noNamespaceCosts') }}
          </p>
          <table v-else>
            <thead>
              <tr>
                <th>Namespace</th>
                <th>{{ t('cost.monthlyCostShort') }}</th>
                <th>{{ t('cost.monthlySavingShort') }}</th>
                <th>{{ t('cost.workloads') }}</th>
                <th>{{ t('cost.recommendationsShort') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in namespaceCostsQuery.data.value" :key="item.namespace">
                <td>{{ item.namespace }}</td>
                <td>{{ formatMoney(item.estimatedMonthlyCost, item.currency) }}</td>
                <td class="tone-good">{{ formatMoney(item.estimatedMonthlySaving, item.currency) }}</td>
                <td>{{ item.workloadCount }}</td>
                <td>{{ item.recommendationCount }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="panel inventory-panel">
          <p class="eyebrow">{{ t('cost.workloadBreakdown') }}</p>
          <h2>Deployment</h2>
          <p v-if="!workloadCostsQuery.data.value?.length" class="empty-state">
            {{ t('cost.noWorkloadCosts') }}
          </p>
          <table v-else>
            <thead>
              <tr>
                <th>{{ t('table.name') }}</th>
                <th>{{ t('table.status') }}</th>
                <th>{{ t('cost.monthlyCostShort') }}</th>
                <th>CPU</th>
                <th>Memory</th>
                <th>{{ t('cost.cpuMemoryUsageRatio') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="workload in workloadCostsQuery.data.value" :key="`${workload.namespace}/${workload.name}`">
                <td>
                  <RouterLink class="table-resource-link" :to="workloadRoute(workload)">
                    {{ workload.name }}
                  </RouterLink>
                </td>
                <td><span :class="statusClass(workload.status)">{{ statusLabel(workload.status) }}</span></td>
                <td>{{ formatMoney(workload.estimatedMonthlyCost, workload.currency) }}</td>
                <td>{{ formatCpu(workload.cpuRequestCores) }}</td>
                <td>{{ formatBytes(workload.memoryRequestBytes) }}</td>
                <td>
                  <span v-if="workload.metricsAvailable" class="usage-ratio-pair">
                    <span>CPU {{ formatPercent(workload.cpuUsagePercent) }}</span>
                    <span>Memory {{ formatPercent(workload.memoryUsagePercent) }}</span>
                  </span>
                  <span v-else class="tone-warning">{{ t('metrics.unavailable') }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </article>
      </div>
    </template>
  </section>
</template>
