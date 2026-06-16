import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'
import {
  collectTrendSnapshots,
  getCostDailyTrend,
  getKubernetesWorkloadUsageTrend,
} from '../../api/trendsApi'

export function useKubernetesWorkloadUsageTrendQuery(
  clusterId: Ref<string>,
  namespace: Ref<string>,
  workloadName: Ref<string>,
  from: Ref<string>,
  to: Ref<string>,
) {
  return useQuery({
    queryKey: computed(() => [
      'trends',
      clusterId.value,
      'kubernetes',
      namespace.value || 'all',
      workloadName.value || 'all',
      from.value,
      to.value,
    ]),
    queryFn: () =>
      getKubernetesWorkloadUsageTrend(clusterId.value, {
        namespace: namespace.value,
        workloadName: workloadName.value,
        from: from.value,
        to: to.value,
      }),
    enabled: computed(() => Boolean(clusterId.value)),
    refetchInterval: 60_000,
  })
}

export function useCostDailyTrendQuery(
  clusterId: Ref<string>,
  namespace: Ref<string>,
  workloadName: Ref<string>,
  from: Ref<string>,
  to: Ref<string>,
) {
  return useQuery({
    queryKey: computed(() => [
      'trends',
      clusterId.value,
      'cost',
      namespace.value || 'all',
      workloadName.value || 'all',
      from.value,
      to.value,
    ]),
    queryFn: () =>
      getCostDailyTrend(clusterId.value, {
        namespace: namespace.value,
        workloadName: workloadName.value,
        from: from.value,
        to: to.value,
      }),
    enabled: computed(() => Boolean(clusterId.value)),
    refetchInterval: 60_000,
  })
}

export function useCollectTrendSnapshotsMutation(clusterId: Ref<string>, namespace: Ref<string>) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => collectTrendSnapshots(clusterId.value, namespace.value),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['trends', clusterId.value] }),
  })
}
