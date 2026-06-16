import { useQuery } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'
import { getDeployments, getEvents, getNamespaces, getNodes, getPods } from '../../api/kubernetesApi'

export function useNamespacesQuery(clusterId: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['kubernetes', clusterId.value, 'namespaces']),
    queryFn: () => getNamespaces(clusterId.value),
  })
}

export function useDeploymentsQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['kubernetes', clusterId.value, namespace.value, 'deployments']),
    queryFn: () => getDeployments(clusterId.value, namespace.value),
    enabled: computed(() => Boolean(namespace.value)),
  })
}

export function usePodsQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['kubernetes', clusterId.value, namespace.value, 'pods']),
    queryFn: () => getPods(clusterId.value, namespace.value),
    enabled: computed(() => Boolean(namespace.value)),
  })
}

export function useEventsQuery(clusterId: Ref<string>, namespace: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['kubernetes', clusterId.value, namespace.value, 'events']),
    queryFn: () => getEvents(clusterId.value, namespace.value),
    enabled: computed(() => Boolean(namespace.value)),
  })
}

export function useNodesQuery(clusterId: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['kubernetes', clusterId.value, 'nodes']),
    queryFn: () => getNodes(clusterId.value),
  })
}
