export type ActionType = 'RESTART_DEPLOYMENT' | 'SCALE_DEPLOYMENT' | 'ROLLOUT_UNDO' | 'DELETE_POD'

export type ActionRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export type ActionStatus = 'DRY_RUN' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'SUCCESS' | 'FAILED'

export interface ActionRequest {
  type: ActionType
  namespace: string
  targetKind: string
  targetName: string
  parameters?: Record<string, string>
  reason?: string
}

export interface ActionDiff {
  field: string
  beforeValue: string
  afterValue: string
}

export interface ActionPreview {
  clusterId: string
  type: ActionType
  namespace: string
  targetKind: string
  targetName: string
  risk: ActionRiskLevel
  approvalRequired: boolean
  executable: boolean
  beforeState: Record<string, unknown>
  afterState: Record<string, unknown>
  diff: ActionDiff[]
  warnings: string[]
  message: string
  createdAt: string
}

export interface ActionExecutionResult {
  auditLogId: string
  approvalId?: string
  status: ActionStatus
  preview: ActionPreview
  message: string
  executedAt?: string
}

export interface ActionAuditLog {
  id: string
  clusterId: string
  namespace: string
  targetKind: string
  targetName: string
  actionType: ActionType
  actor: string
  actorRole: string
  status: ActionStatus
  risk: ActionRiskLevel
  approvalRequired: boolean
  approvalId?: string
  parameters: Record<string, string>
  beforeState: Record<string, unknown>
  afterState: Record<string, unknown>
  diff: ActionDiff[]
  message: string
  createdAt: string
  executedAt?: string
}

export interface ActionApprovalRequest {
  id: string
  auditLogId: string
  clusterId: string
  namespace: string
  targetKind: string
  targetName: string
  actionType: ActionType
  requester: string
  requesterRole: string
  status: ActionStatus
  requestedAt: string
  decidedBy?: string
  decidedAt?: string
  decisionReason?: string
}
