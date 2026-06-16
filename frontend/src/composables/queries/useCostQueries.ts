import { useQuery } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'
import {
  getCostRecommendations,
  getCostSummary,
  getNamespaceCosts,
  getWorkloadCosts,
} from '../../api/costApi'

export function useCostSummaryQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['cost', clusterId.value, namespace.value || 'all', 'summary']),
    queryFn: () => getCostSummary(clusterId.value, namespace.value),
    enabled: computed(() => Boolean(clusterId.value)),
    refetchInterval: 60_000,
  })
}

export function useNamespaceCostsQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['cost', clusterId.value, namespace.value || 'all', 'namespaces']),
    queryFn: () => getNamespaceCosts(clusterId.value, namespace.value),
    enabled: computed(() => Boolean(clusterId.value)),
    refetchInterval: 60_000,
  })
}

export function useWorkloadCostsQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['cost', clusterId.value, namespace.value || 'all', 'workloads']),
    queryFn: () => getWorkloadCosts(clusterId.value, namespace.value),
    enabled: computed(() => Boolean(clusterId.value)),
    refetchInterval: 60_000,
  })
}

export function useCostRecommendationsQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['cost', clusterId.value, namespace.value || 'all', 'recommendations']),
    queryFn: () => getCostRecommendations(clusterId.value, namespace.value),
    enabled: computed(() => Boolean(clusterId.value)),
    refetchInterval: 60_000,
  })
}
