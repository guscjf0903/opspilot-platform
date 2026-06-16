import { useQuery } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'
import { getTopology } from '../../api/topologyApi'

export function useTopologyQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['topology', clusterId.value, namespace.value]),
    queryFn: () => getTopology(clusterId.value, namespace.value),
    enabled: computed(() => Boolean(namespace.value)),
    refetchInterval: 30_000,
  })
}
