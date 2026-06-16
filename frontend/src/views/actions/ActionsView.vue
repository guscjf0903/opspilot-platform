<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import {
  useActionApprovalsQuery,
  useActionAuditLogsQuery,
  useApproveActionMutation,
  useRejectActionMutation,
} from '../../composables/queries/useActionQueries'
import { useClusterSelectionStore } from '../../stores/clusterSelection'
import { useUiPreferencesStore } from '../../stores/uiPreferences'
import type { ActionAuditLog, ActionRiskLevel, ActionStatus, ActionType } from '../../types/actions'

const selectionStore = useClusterSelectionStore()
const { clusterId } = storeToRefs(selectionStore)
const uiPreferences = useUiPreferencesStore()
const { locale } = storeToRefs(uiPreferences)
const { t } = uiPreferences

const approvalStatus = ref<ActionStatus | ''>('PENDING_APPROVAL')
const auditLogsQuery = useActionAuditLogsQuery(clusterId)
const approvalsQuery = useActionApprovalsQuery(clusterId, approvalStatus)
const approveMutation = useApproveActionMutation(clusterId)
const rejectMutation = useRejectActionMutation(clusterId)

const auditLogs = computed(() => auditLogsQuery.data.value ?? [])
const approvals = computed(() => approvalsQuery.data.value ?? [])
const recentLogs = computed(() => auditLogs.value.slice(0, 20))
const successCount = computed(() => auditLogs.value.filter((log) => log.status === 'SUCCESS').length)
const pendingCount = computed(() => approvals.value.filter((approval) => approval.status === 'PENDING_APPROVAL').length)
const failedCount = computed(() => auditLogs.value.filter((log) => log.status === 'FAILED').length)

function actionTypeLabel(type: ActionType) {
  const labels: Record<ActionType, string> = {
    RESTART_DEPLOYMENT: t('actions.type.restartDeployment'),
    SCALE_DEPLOYMENT: t('actions.type.scaleDeployment'),
    ROLLOUT_UNDO: t('actions.type.rolloutUndo'),
    DELETE_POD: t('actions.type.deletePod'),
  }

  return labels[type]
}

function statusLabel(status: ActionStatus) {
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

function riskLabel(risk: ActionRiskLevel) {
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

function riskClass(risk: ActionRiskLevel) {
  return `action-risk action-risk-${risk.toLowerCase()}`
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat(locale.value === 'ko' ? 'ko-KR' : 'en-US', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function firstDiff(log: ActionAuditLog) {
  const diff = log.diff[0]

  return diff ? `${diff.field}: ${diff.beforeValue} -> ${diff.afterValue}` : '-'
}

async function approve(approvalId: string) {
  await approveMutation.mutateAsync({ approvalId, reason: 'Approved from OpsPilot Actions view.' })
}

async function reject(approvalId: string) {
  await rejectMutation.mutateAsync({ approvalId, reason: 'Rejected from OpsPilot Actions view.' })
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ t('actions.eyebrow') }}</p>
        <h1>{{ t('actions.title') }}</h1>
        <p class="subtitle">{{ t('actions.subtitle') }}</p>
      </div>
    </div>

    <div class="inventory-summary action-summary">
      <article class="summary-card">
        <p>{{ t('actions.summary.logs') }}</p>
        <strong>{{ auditLogs.length }}</strong>
        <small>{{ t('actions.summary.auditStored') }}</small>
      </article>
      <article class="summary-card">
        <p>{{ t('actions.summary.success') }}</p>
        <strong>{{ successCount }}</strong>
        <small>{{ t('actions.status.success') }}</small>
      </article>
      <article class="summary-card">
        <p>{{ t('actions.summary.pending') }}</p>
        <strong>{{ pendingCount }}</strong>
        <small>{{ t('actions.status.pendingApproval') }}</small>
      </article>
      <article class="summary-card">
        <p>{{ t('actions.summary.failed') }}</p>
        <strong>{{ failedCount }}</strong>
        <small>{{ t('actions.status.failed') }}</small>
      </article>
    </div>

    <div class="panel-grid action-grid">
      <article class="panel inventory-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">{{ t('actions.approvals') }}</p>
            <h2>{{ t('actions.pendingApprovals') }}</h2>
          </div>
        </div>

        <p v-if="approvalsQuery.isPending.value" class="empty-state">{{ t('actions.loading') }}</p>
        <p v-else-if="!approvals.length" class="empty-state">{{ t('actions.noApprovals') }}</p>
        <ul v-else class="analysis-list">
          <li v-for="approval in approvals" :key="approval.id">
            <strong>{{ actionTypeLabel(approval.actionType) }}</strong>
            <small>{{ approval.namespace }}/{{ approval.targetName }} · {{ formatDate(approval.requestedAt) }}</small>
            <span>{{ approval.requester }} / {{ approval.requesterRole }}</span>
            <div v-if="approval.status === 'PENDING_APPROVAL'" class="action-button-row">
              <button
                type="button"
                class="secondary-action-button"
                :disabled="approveMutation.isPending.value"
                @click="approve(approval.id)"
              >
                {{ t('actions.approve') }}
              </button>
              <button
                type="button"
                class="danger-action-button"
                :disabled="rejectMutation.isPending.value"
                @click="reject(approval.id)"
              >
                {{ t('actions.reject') }}
              </button>
            </div>
            <span v-else :class="actionStatusClass(approval.status)">{{ statusLabel(approval.status) }}</span>
          </li>
        </ul>
      </article>

      <article class="panel inventory-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">{{ t('actions.audit') }}</p>
            <h2>{{ t('actions.recentLogs') }}</h2>
          </div>
        </div>

        <p v-if="auditLogsQuery.isPending.value" class="empty-state">{{ t('actions.loading') }}</p>
        <p v-else-if="!recentLogs.length" class="empty-state">{{ t('actions.noLogs') }}</p>
        <ul v-else class="analysis-list action-log-list">
          <li v-for="log in recentLogs" :key="log.id">
            <div class="action-log-heading">
              <strong>{{ actionTypeLabel(log.actionType) }}</strong>
              <span :class="actionStatusClass(log.status)">{{ statusLabel(log.status) }}</span>
            </div>
            <small>{{ log.namespace }}/{{ log.targetName }} · {{ formatDate(log.createdAt) }}</small>
            <span>{{ firstDiff(log) }}</span>
            <span :class="riskClass(log.risk)">{{ riskLabel(log.risk) }}</span>
          </li>
        </ul>
      </article>
    </div>
  </section>
</template>
