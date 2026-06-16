import { httpClient } from './httpClient'
import type {
  ActionApprovalRequest,
  ActionAuditLog,
  ActionExecutionResult,
  ActionPreview,
  ActionRequest,
  ActionStatus,
} from '../types/actions'

export interface ActionAuditLogQuery {
  namespace?: string
  targetName?: string
}

const localActionHeaders = {
  'X-OpsPilot-Actor': 'local-user@example.com',
  'X-OpsPilot-Role': 'OPERATOR',
}

export async function dryRunAction(clusterId: string, request: ActionRequest): Promise<ActionPreview> {
  const response = await httpClient.post<ActionPreview>(
    `/api/clusters/${clusterId}/actions/dry-run`,
    request,
    { headers: localActionHeaders },
  )

  return response.data
}

export async function executeAction(clusterId: string, request: ActionRequest): Promise<ActionExecutionResult> {
  const response = await httpClient.post<ActionExecutionResult>(
    `/api/clusters/${clusterId}/actions/execute`,
    request,
    { headers: localActionHeaders },
  )

  return response.data
}

export async function getActionAuditLogs(
  clusterId: string,
  query: ActionAuditLogQuery = {},
): Promise<ActionAuditLog[]> {
  const response = await httpClient.get<ActionAuditLog[]>(
    `/api/clusters/${clusterId}/actions/audit-logs`,
    { params: query },
  )

  return response.data
}

export async function getActionApprovals(
  clusterId: string,
  status?: ActionStatus,
): Promise<ActionApprovalRequest[]> {
  const response = await httpClient.get<ActionApprovalRequest[]>(
    `/api/clusters/${clusterId}/actions/approvals`,
    { params: status ? { status } : undefined },
  )

  return response.data
}

export async function approveAction(
  clusterId: string,
  approvalId: string,
  reason?: string,
): Promise<ActionApprovalRequest> {
  const response = await httpClient.post<ActionApprovalRequest>(
    `/api/clusters/${clusterId}/actions/approvals/${approvalId}/approve`,
    { reason },
    { headers: localActionHeaders },
  )

  return response.data
}

export async function rejectAction(
  clusterId: string,
  approvalId: string,
  reason?: string,
): Promise<ActionApprovalRequest> {
  const response = await httpClient.post<ActionApprovalRequest>(
    `/api/clusters/${clusterId}/actions/approvals/${approvalId}/reject`,
    { reason },
    { headers: localActionHeaders },
  )

  return response.data
}
