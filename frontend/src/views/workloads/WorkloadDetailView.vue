<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import MetricLineChart from '../../components/metrics/MetricLineChart.vue'
import {
  useActionAuditLogsQuery,
  useDryRunActionMutation,
  useExecuteActionMutation,
} from '../../composables/queries/useActionQueries'
import { useIncidentAnalysisMutation } from '../../composables/queries/useIncidentAnalysisMutation'
import {
  useDeploymentsQuery,
  useEventsQuery,
  usePodsQuery,
} from '../../composables/queries/useKubernetesInventoryQueries'
import { useWorkloadMetricsQuery } from '../../composables/queries/useMetricsQuery'
import { translateResourceReason, translateResourceStatus } from '../../i18n/messages'
import { useClusterSelectionStore } from '../../stores/clusterSelection'
import { useUiPreferencesStore } from '../../stores/uiPreferences'
import type { ActionExecutionResult, ActionPreview, ActionRequest, ActionRiskLevel, ActionStatus, ActionType } from '../../types/actions'
import type { IncidentAnalysisReport } from '../../types/aiAnalysis'
import type { ResourceStatus } from '../../types/kubernetes'

const route = useRoute()
const selectionStore = useClusterSelectionStore()
const { clusterId } = storeToRefs(selectionStore)
const uiPreferences = useUiPreferencesStore()
const { locale } = storeToRefs(uiPreferences)
const { t } = uiPreferences

const namespace = computed(() => String(route.params.namespace))
const kind = computed(() => String(route.params.kind).toLowerCase())
const name = computed(() => String(route.params.name))
const metricsRangeMinutes = computed(() => 30)
const metricsSupported = computed(() => kind.value === 'pod' || kind.value === 'deployment')
const deploymentsQuery = useDeploymentsQuery(clusterId, namespace)
const podsQuery = usePodsQuery(clusterId, namespace)
const eventsQuery = useEventsQuery(clusterId, namespace)
const analysisMutation = useIncidentAnalysisMutation(() => clusterId.value)
const analysisReport = ref<IncidentAnalysisReport>()
const dryRunMutation = useDryRunActionMutation(clusterId)
const executeMutation = useExecuteActionMutation(clusterId)
const actionAuditLogsQuery = useActionAuditLogsQuery(clusterId, namespace, name)
const actionPreview = ref<ActionPreview>()
const actionResult = ref<ActionExecutionResult>()
const selectedActionType = ref<ActionType>()
const scaleReplicas = ref(1)
const approvalIdInput = ref('')
const metricsQuery = useWorkloadMetricsQuery(
  clusterId,
  namespace,
  kind,
  name,
  metricsRangeMinutes,
  metricsSupported,
)

const resource = computed(() => {
  if (kind.value === 'deployment') {
    return (deploymentsQuery.data.value ?? []).find((deployment) => deployment.name === name.value)
  }

  if (kind.value === 'pod') {
    return (podsQuery.data.value ?? []).find((pod) => pod.name === name.value)
  }

  return undefined
})

const relatedEvents = computed(() =>
  (eventsQuery.data.value ?? []).filter((event) => event.involvedName === name.value).slice(0, 8),
)
const recentActionLogs = computed(() => (actionAuditLogsQuery.data.value ?? []).slice(0, 5))

const isPending = computed(() => deploymentsQuery.isPending.value || podsQuery.isPending.value)
const isError = computed(
  () => deploymentsQuery.isError.value || podsQuery.isError.value || eventsQuery.isError.value,
)
const metrics = computed(() => metricsQuery.data.value)
const evidenceById = computed(() => {
  const evidenceMap = new Map<string, string>()

  for (const evidence of analysisReport.value?.evidence ?? []) {
    evidenceMap.set(evidence.id, `${evidence.title}: ${evidence.message}`)
  }

  return evidenceMap
})
const analysisErrorMessage = computed(() => {
  const error = analysisMutation.error.value

  return error instanceof Error ? error.message : t('aiAnalysis.errorDescription')
})
const analysisUsageLabel = computed(() => {
  if (!analysisReport.value) {
    return ''
  }

  const usage = analysisReport.value.tokenUsage
  const model = analysisReport.value.model || '-'

  return t('aiAnalysis.usageSummary')
    .replace('{model}', model)
    .replace('{inputTokens}', String(usage?.inputTokens ?? 0))
    .replace('{outputTokens}', String(usage?.outputTokens ?? 0))
    .replace('{totalTokens}', String(usage?.totalTokens ?? 0))
    .replace('{latencyMs}', String(analysisReport.value.latencyMs ?? 0))
})
const cpuBaselineLabel = computed(() => {
  if (!metrics.value) {
    return '-'
  }

  if (metrics.value.summary.cpuRequestCores !== undefined && metrics.value.summary.cpuRequestCores !== null) {
    return formatCpu(metrics.value.summary.cpuRequestCores)
  }

  if (metrics.value.summary.cpuCapacityCores !== undefined && metrics.value.summary.cpuCapacityCores !== null) {
    return formatCpu(metrics.value.summary.cpuCapacityCores)
  }

  return '-'
})
const memoryBaselineLabel = computed(() => {
  if (!metrics.value) {
    return '-'
  }

  if (metrics.value.summary.memoryRequestBytes !== undefined && metrics.value.summary.memoryRequestBytes !== null) {
    return formatBytes(metrics.value.summary.memoryRequestBytes)
  }

  if (metrics.value.summary.memoryCapacityBytes !== undefined && metrics.value.summary.memoryCapacityBytes !== null) {
    return formatBytes(metrics.value.summary.memoryCapacityBytes)
  }

  return '-'
})
const actionErrorMessage = computed(() => {
  const error = dryRunMutation.error.value ?? executeMutation.error.value

  return error instanceof Error ? error.message : t('actions.safePanel.error')
})

watchEffect(() => {
  if (resource.value && 'desiredReplicas' in resource.value) {
    scaleReplicas.value = resource.value.desiredReplicas
  }
})

function statusClass(status: ResourceStatus) {
  return `resource-status status-${status}`
}

function statusLabel(status: ResourceStatus) {
  return translateResourceStatus(locale.value, status)
}

function reasonLabel(reason: string) {
  return translateResourceReason(locale.value, reason)
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

async function runIncidentAnalysis() {
  if (!resource.value) {
    return
  }

  analysisReport.value = undefined
  try {
    analysisReport.value = await analysisMutation.mutateAsync({
      namespace: namespace.value,
      targetKind: resource.value.kind,
      targetName: name.value,
      timeRangeMinutes: 30,
    })
  } catch {
    analysisReport.value = undefined
  }
}

function confidenceLabel(confidence: number) {
  return `${Math.round(confidence * 100)}%`
}

function riskLabel(risk: string) {
  const labels: Record<string, string> = {
    low: t('cost.risk.low'),
    medium: t('cost.risk.medium'),
    high: t('cost.risk.high'),
  }

  return labels[risk] ?? risk
}

function evidenceTypeLabel(type: string) {
  const labels: Record<string, string> = {
    target: t('aiAnalysis.evidenceType.target'),
    event: t('aiAnalysis.evidenceType.event'),
    metric: t('aiAnalysis.evidenceType.metric'),
    topology: t('aiAnalysis.evidenceType.topology'),
    ai: t('aiAnalysis.evidenceType.ai'),
  }

  return labels[type] ?? type
}

function evidenceLabel(evidenceId: string) {
  return evidenceById.value.get(evidenceId) ?? evidenceId
}

function actionTypeLabel(type: ActionType) {
  const labels: Record<ActionType, string> = {
    RESTART_DEPLOYMENT: t('actions.type.restartDeployment'),
    SCALE_DEPLOYMENT: t('actions.type.scaleDeployment'),
    ROLLOUT_UNDO: t('actions.type.rolloutUndo'),
    DELETE_POD: t('actions.type.deletePod'),
  }

  return labels[type]
}

function actionStatusLabel(status: ActionStatus) {
  const labels: Record<ActionStatus, string> = {
    DRY_RUN: t('actions.status.dryRun'),
    PENDING_APPROVAL: t('actions.status.pendingApproval'),
    APPROVED: t('actions.status.approved'),
    REJECTED: t('actions.status.rejected'),
    SUCCESS: t('actions.status.success'),
    FAILED: t('actions.status.failed'),
  }

  return labels[status]
}

function actionRiskLabel(risk: ActionRiskLevel) {
  const labels: Record<ActionRiskLevel, string> = {
    LOW: t('actions.risk.low'),
    MEDIUM: t('actions.risk.medium'),
    HIGH: t('actions.risk.high'),
  }

  return labels[risk]
}

function actionStatusClass(status: ActionStatus) {
  return `action-status action-status-${status.toLowerCase().replace('_', '-')}`
}

function actionRiskClass(risk: ActionRiskLevel) {
  return `action-risk action-risk-${risk.toLowerCase()}`
}

function actionRequest(type: ActionType): ActionRequest {
  const parameters: Record<string, string> = {}
  if (type === 'SCALE_DEPLOYMENT') {
    parameters.replicas = String(scaleReplicas.value)
  }
  if (approvalIdInput.value.trim()) {
    parameters.approvalId = approvalIdInput.value.trim()
  }

  return {
    type,
    namespace: namespace.value,
    targetKind: type === 'DELETE_POD' ? 'Pod' : 'Deployment',
    targetName: name.value,
    parameters,
  }
}

async function runActionDryRun(type: ActionType) {
  selectedActionType.value = type
  actionResult.value = undefined
  approvalIdInput.value = ''
  actionPreview.value = await dryRunMutation.mutateAsync(actionRequest(type))
}

async function executePreviewedAction() {
  if (!selectedActionType.value) {
    return
  }

  actionResult.value = await executeMutation.mutateAsync(actionRequest(selectedActionType.value))
}
</script>

<template>
  <section class="page">
    <RouterLink class="back-link" to="/">{{ t('detail.backToDashboard') }}</RouterLink>

    <div class="page-heading detail-heading">
      <div>
        <p class="eyebrow">{{ t('detail.eyebrow') }}</p>
        <h1>{{ name }}</h1>
        <p class="subtitle">{{ namespace }} / {{ kind }}</p>
      </div>
    </div>

    <article v-if="isError" class="panel state-panel status-critical">
      <h2>{{ t('workloads.apiUnavailable') }}</h2>
      <p>{{ t('workloads.apiUnavailableDescription') }}</p>
    </article>

    <article v-else-if="isPending" class="panel state-panel">
      <h2>{{ t('detail.loading') }}</h2>
    </article>

    <article v-else-if="!resource" class="panel state-panel">
      <h2>{{ t('detail.notFound') }}</h2>
      <p>{{ t('detail.notFoundDescription') }}</p>
    </article>

    <template v-else>
      <div class="detail-grid">
        <article class="panel detail-card">
          <p class="eyebrow">{{ t('detail.currentStatus') }}</p>
          <span :class="statusClass(resource.status)">{{ statusLabel(resource.status) }}</span>
          <h2>{{ reasonLabel(resource.reason) }}</h2>
          <p class="detail-message">{{ resource.message }}</p>
        </article>

        <article class="panel detail-card">
          <p class="eyebrow">{{ t('detail.resourceInfo') }}</p>
          <dl class="detail-list">
            <div>
              <dt>Namespace</dt>
              <dd>{{ namespace }}</dd>
            </div>
            <div>
              <dt>{{ t('detail.kind') }}</dt>
              <dd>{{ resource.kind }}</dd>
            </div>
            <div v-if="'restartCount' in resource">
              <dt>{{ t('table.restarts') }}</dt>
              <dd>{{ resource.restartCount }}</dd>
            </div>
            <div v-if="'nodeName' in resource">
              <dt>Node</dt>
              <dd>{{ resource.nodeName ?? '-' }}</dd>
            </div>
            <div v-if="'availableReplicas' in resource">
              <dt>{{ t('table.available') }}</dt>
              <dd>{{ resource.availableReplicas }}/{{ resource.desiredReplicas }}</dd>
            </div>
          </dl>
        </article>
      </div>

      <article class="panel action-panel">
        <div class="ai-analysis-heading">
          <div>
            <p class="eyebrow">{{ t('actions.safePanel.eyebrow') }}</p>
            <h2>{{ t('actions.safePanel.title') }}</h2>
            <p class="ai-analysis-description">{{ t('actions.safePanel.description') }}</p>
          </div>
        </div>

        <div class="action-control-grid">
          <div class="action-control-card" v-if="kind === 'deployment'">
            <strong>{{ t('actions.type.restartDeployment') }}</strong>
            <button
              type="button"
              class="secondary-action-button"
              :disabled="dryRunMutation.isPending.value"
              @click="runActionDryRun('RESTART_DEPLOYMENT')"
            >
              {{ t('actions.safePanel.dryRun') }}
            </button>
          </div>

          <div class="action-control-card" v-if="kind === 'deployment'">
            <strong>{{ t('actions.type.scaleDeployment') }}</strong>
            <label class="inline-field">
              <span>{{ t('actions.safePanel.replicas') }}</span>
              <input v-model.number="scaleReplicas" type="number" min="0" />
            </label>
            <button
              type="button"
              class="secondary-action-button"
              :disabled="dryRunMutation.isPending.value"
              @click="runActionDryRun('SCALE_DEPLOYMENT')"
            >
              {{ t('actions.safePanel.dryRun') }}
            </button>
          </div>

          <div class="action-control-card" v-if="kind === 'deployment'">
            <strong>{{ t('actions.type.rolloutUndo') }}</strong>
            <button
              type="button"
              class="secondary-action-button"
              :disabled="dryRunMutation.isPending.value"
              @click="runActionDryRun('ROLLOUT_UNDO')"
            >
              {{ t('actions.safePanel.dryRun') }}
            </button>
          </div>

          <div class="action-control-card" v-if="kind === 'pod'">
            <strong>{{ t('actions.type.deletePod') }}</strong>
            <button
              type="button"
              class="danger-action-button"
              :disabled="dryRunMutation.isPending.value"
              @click="runActionDryRun('DELETE_POD')"
            >
              {{ t('actions.safePanel.dryRun') }}
            </button>
          </div>
        </div>

        <div v-if="dryRunMutation.isError.value || executeMutation.isError.value" class="metric-unavailable">
          <strong>{{ t('actions.safePanel.error') }}</strong>
          <small>{{ actionErrorMessage }}</small>
        </div>

        <div v-if="actionPreview" class="action-preview-card">
          <div class="action-log-heading">
            <strong>{{ t('actions.safePanel.preview') }} · {{ actionTypeLabel(actionPreview.type) }}</strong>
            <span :class="actionRiskClass(actionPreview.risk)">{{ actionRiskLabel(actionPreview.risk) }}</span>
          </div>
          <p>{{ actionPreview.message }}</p>
          <ul v-if="actionPreview.warnings.length" class="action-warning-list">
            <li v-for="warning in actionPreview.warnings" :key="warning">{{ warning }}</li>
          </ul>
          <dl class="action-diff-list">
            <div v-for="diff in actionPreview.diff" :key="diff.field">
              <dt>{{ diff.field }}</dt>
              <dd>{{ diff.beforeValue }} -> {{ diff.afterValue }}</dd>
            </div>
          </dl>
          <label v-if="actionPreview.approvalRequired" class="inline-field approval-field">
            <span>{{ t('actions.safePanel.approvalRequired') }}</span>
            <input v-model="approvalIdInput" type="text" placeholder="approvalId" />
          </label>
          <button
            type="button"
            class="primary-action-button"
            :disabled="executeMutation.isPending.value"
            @click="executePreviewedAction"
          >
            {{ executeMutation.isPending.value ? t('actions.safePanel.executing') : t('actions.safePanel.execute') }}
          </button>
        </div>
        <p v-else class="empty-state">{{ t('actions.safePanel.noPreview') }}</p>

        <div v-if="actionResult" class="analysis-summary-card">
          <span :class="actionStatusClass(actionResult.status)">{{ actionStatusLabel(actionResult.status) }}</span>
          <strong>{{ actionResult.message }}</strong>
          <small>{{ t('actions.safePanel.auditHint') }} {{ actionResult.auditLogId }}</small>
          <small v-if="actionResult.approvalId">
            {{ t('actions.safePanel.approvalHint') }} {{ actionResult.approvalId }}
          </small>
        </div>

        <section class="action-recent-section">
          <h3>{{ t('actions.safePanel.recentLogs') }}</h3>
          <p v-if="!recentActionLogs.length" class="empty-state">{{ t('actions.noLogs') }}</p>
          <ul v-else class="analysis-list">
            <li v-for="log in recentActionLogs" :key="log.id">
              <strong>{{ actionTypeLabel(log.actionType) }}</strong>
              <small>{{ actionStatusLabel(log.status) }} / {{ actionRiskLabel(log.risk) }}</small>
              <span v-if="log.diff[0]">{{ log.diff[0].field }}: {{ log.diff[0].beforeValue }} -> {{ log.diff[0].afterValue }}</span>
              <span v-else>{{ log.message }}</span>
            </li>
          </ul>
        </section>
      </article>

      <article v-if="metricsSupported" class="panel metrics-panel">
        <div class="metrics-heading">
          <div>
            <p class="eyebrow">{{ t('metrics.eyebrow') }}</p>
            <h2>{{ t('metrics.title') }}</h2>
          </div>
          <span class="metric-range">{{ t('metrics.lastThirtyMinutes') }}</span>
        </div>

        <p v-if="metricsQuery.isPending.value" class="empty-state">{{ t('metrics.loading') }}</p>
        <p v-else-if="metricsQuery.isError.value" class="empty-state">{{ t('metrics.apiUnavailable') }}</p>
        <div v-else-if="metrics?.status === 'unavailable'" class="metric-unavailable">
          <strong>{{ t('metrics.unavailable') }}</strong>
          <small>{{ metrics.reason }}</small>
        </div>
        <template v-else-if="metrics">
          <div class="metric-summary-grid">
            <div class="metric-summary-item">
              <small>{{ t('metrics.cpuUsage') }}</small>
              <strong>{{ formatCpu(metrics.summary.cpuUsageCores) }}</strong>
              <span>{{ t('metrics.usageRatio') }} {{ formatPercent(metrics.summary.cpuUsagePercent) }}</span>
            </div>
            <div class="metric-summary-item">
              <small>{{ t('metrics.memoryUsage') }}</small>
              <strong>{{ formatBytes(metrics.summary.memoryUsageBytes) }}</strong>
              <span>{{ t('metrics.usageRatio') }} {{ formatPercent(metrics.summary.memoryUsagePercent) }}</span>
            </div>
            <div class="metric-summary-item">
              <small>{{ t('metrics.cpuBaseline') }}</small>
              <strong>{{ cpuBaselineLabel }}</strong>
              <span>{{ t('metrics.requestOrCapacity') }}</span>
            </div>
            <div class="metric-summary-item">
              <small>{{ t('metrics.memoryBaseline') }}</small>
              <strong>{{ memoryBaselineLabel }}</strong>
              <span>{{ t('metrics.requestOrCapacity') }}</span>
            </div>
          </div>

          <div class="metric-chart-grid">
            <MetricLineChart
              :title="t('metrics.cpuChart')"
              :unit-label="t('metrics.cpuUnit')"
              :points="metrics.cpu.points"
              :format-value="formatCpu"
              :empty-label="t('metrics.noPoints')"
            />
            <MetricLineChart
              :title="t('metrics.memoryChart')"
              :unit-label="t('metrics.memoryUnit')"
              :points="metrics.memory.points"
              :format-value="formatBytes"
              :empty-label="t('metrics.noPoints')"
            />
          </div>
        </template>
      </article>

      <article class="panel ai-analysis-panel">
        <div class="ai-analysis-heading">
          <div>
            <p class="eyebrow">{{ t('aiAnalysis.eyebrow') }}</p>
            <h2>{{ t('aiAnalysis.title') }}</h2>
            <p class="ai-analysis-description">{{ t('aiAnalysis.description') }}</p>
          </div>
          <button
            class="primary-action-button"
            type="button"
            :disabled="analysisMutation.isPending.value"
            @click="runIncidentAnalysis"
          >
            {{ analysisMutation.isPending.value ? t('aiAnalysis.running') : t('aiAnalysis.run') }}
          </button>
        </div>

        <div v-if="analysisMutation.isError.value" class="metric-unavailable">
          <strong>{{ t('aiAnalysis.error') }}</strong>
          <small>{{ analysisErrorMessage }}</small>
        </div>

        <p v-else-if="!analysisReport" class="empty-state">{{ t('aiAnalysis.empty') }}</p>

        <template v-else>
          <div class="analysis-summary-card">
            <span :class="statusClass(analysisReport.severity)">
              {{ statusLabel(analysisReport.severity) }}
            </span>
            <strong>{{ analysisReport.summary }}</strong>
            <small>{{ t('aiAnalysis.provider') }} {{ analysisReport.provider }}</small>
            <small>{{ analysisUsageLabel }}</small>
          </div>

          <div class="analysis-grid">
            <section>
              <h3>{{ t('aiAnalysis.candidates') }}</h3>
              <ul class="analysis-list">
                <li v-for="candidate in analysisReport.rootCauseCandidates" :key="candidate.title">
                  <strong>{{ candidate.title }}</strong>
                  <small>{{ t('aiAnalysis.confidence') }} {{ confidenceLabel(candidate.confidence) }}</small>
                  <span v-for="evidenceId in candidate.evidenceIds" :key="evidenceId">
                    {{ evidenceLabel(evidenceId) }}
                  </span>
                </li>
              </ul>
            </section>

            <section>
              <h3>{{ t('aiAnalysis.recommendations') }}</h3>
              <ul class="analysis-list">
                <li v-for="recommendation in analysisReport.recommendations" :key="recommendation.action">
                  <strong>{{ recommendation.action }}</strong>
                  <small>{{ t('aiAnalysis.risk') }} {{ riskLabel(recommendation.risk) }}</small>
                  <span>{{ recommendation.reason }}</span>
                </li>
              </ul>
            </section>
          </div>

          <div class="analysis-grid">
            <section>
              <h3>{{ t('aiAnalysis.evidence') }}</h3>
              <ul class="analysis-list">
                <li v-for="evidence in analysisReport.evidence" :key="evidence.id">
                  <strong>{{ evidence.title }}</strong>
                  <small>{{ evidenceTypeLabel(evidence.type) }} / {{ statusLabel(evidence.status) }}</small>
                  <span>{{ evidence.message }}</span>
                </li>
              </ul>
            </section>

            <section>
              <h3>{{ t('aiAnalysis.nextChecks') }}</h3>
              <ul class="analysis-list">
                <li v-for="check in analysisReport.nextChecks" :key="check">
                  <span>{{ check }}</span>
                </li>
              </ul>
            </section>
          </div>
        </template>
      </article>

      <article class="panel inventory-panel">
        <p class="eyebrow">{{ t('workloads.recentSignals') }}</p>
        <h2>{{ t('detail.relatedEvents') }}</h2>
        <p v-if="!relatedEvents.length" class="empty-state">{{ t('detail.noRelatedEvents') }}</p>
        <ul v-else class="resource-list event-list">
          <li v-for="event in relatedEvents" :key="event.name">
            <div>
              <strong>{{ reasonLabel(event.reason) }}</strong>
              <small>{{ event.message }}</small>
            </div>
            <span :class="statusClass(event.status)">{{ statusLabel(event.status) }}</span>
          </li>
        </ul>
      </article>
    </template>
  </section>
</template>
