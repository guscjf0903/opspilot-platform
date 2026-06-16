import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'
import {
  approveAction,
  dryRunAction,
  executeAction,
  getActionApprovals,
  getActionAuditLogs,
  rejectAction,
} from '../../api/actionsApi'
import type { ActionRequest, ActionStatus } from '../../types/actions'

export function useActionAuditLogsQuery(
  clusterId: Ref<string>,
  namespace?: Ref<string>,
  targetName?: Ref<string>,
) {
  return useQuery({
    queryKey: computed(() => [
      'actions',
      clusterId.value,
      'audit-logs',
      namespace?.value || 'all',
      targetName?.value || 'all',
    ]),
    queryFn: () =>
      getActionAuditLogs(clusterId.value, {
        namespace: namespace?.value || undefined,
        targetName: targetName?.value || undefined,
      }),
    enabled: computed(() => Boolean(clusterId.value)),
    refetchInterval: 30_000,
  })
}

export function useActionApprovalsQuery(clusterId: Ref<string>, status?: Ref<ActionStatus | ''>) {
  return useQuery({
    queryKey: computed(() => ['actions', clusterId.value, 'approvals', status?.value || 'all']),
    queryFn: () => getActionApprovals(clusterId.value, status?.value || undefined),
    enabled: computed(() => Boolean(clusterId.value)),
    refetchInterval: 30_000,
  })
}

export function useDryRunActionMutation(clusterId: Ref<string>) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: ActionRequest) => dryRunAction(clusterId.value, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['actions', clusterId.value] }),
  })
}

export function useExecuteActionMutation(clusterId: Ref<string>) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: ActionRequest) => executeAction(clusterId.value, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['actions', clusterId.value] })
      queryClient.invalidateQueries({ queryKey: ['kubernetes', clusterId.value] })
    },
  })
}

export function useApproveActionMutation(clusterId: Ref<string>) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ approvalId, reason }: { approvalId: string; reason?: string }) =>
      approveAction(clusterId.value, approvalId, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['actions', clusterId.value] }),
  })
}

export function useRejectActionMutation(clusterId: Ref<string>) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ approvalId, reason }: { approvalId: string; reason?: string }) =>
      rejectAction(clusterId.value, approvalId, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['actions', clusterId.value] }),
  })
}
