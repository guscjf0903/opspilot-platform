import { useQuery } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'
import { getDashboard } from '../../api/dashboardApi'

export function useDashboardQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['dashboard', clusterId.value, namespace.value || 'all']),
    queryFn: () => getDashboard(clusterId.value, namespace.value),
    refetchInterval: 30_000,
  })
}
