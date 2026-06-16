import { useQuery } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'
import { getNodeMetrics, getWorkloadMetrics } from '../../api/metricsApi'

export function useWorkloadMetricsQuery(
  clusterId: Ref<string>,
  namespace: Ref<string>,
  kind: Ref<string>,
  name: Ref<string>,
  rangeMinutes: Ref<number>,
  enabled: Ref<boolean>,
) {
  return useQuery({
    queryKey: computed(() => [
      'metrics',
      clusterId.value,
      namespace.value,
      kind.value,
      name.value,
      rangeMinutes.value,
    ]),
    queryFn: () => getWorkloadMetrics(clusterId.value, namespace.value, kind.value, name.value, rangeMinutes.value),
    enabled: computed(
      () =>
        enabled.value &&
        Boolean(clusterId.value) &&
        Boolean(namespace.value) &&
        Boolean(kind.value) &&
        Boolean(name.value),
    ),
    refetchInterval: 30_000,
  })
}

export function useNodeMetricsQuery(
  clusterId: Ref<string>,
  nodeName: Ref<string>,
  rangeMinutes: Ref<number>,
  enabled: Ref<boolean>,
) {
  return useQuery({
    queryKey: computed(() => ['metrics', clusterId.value, 'node', nodeName.value, rangeMinutes.value]),
    queryFn: () => getNodeMetrics(clusterId.value, nodeName.value, rangeMinutes.value),
    enabled: computed(() => enabled.value && Boolean(clusterId.value) && Boolean(nodeName.value)),
    refetchInterval: 30_000,
  })
}
