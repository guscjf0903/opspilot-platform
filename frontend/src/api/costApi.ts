import { httpClient } from './httpClient'
import type { CostRecommendation, CostSummary, NamespaceCost, WorkloadCost } from '../types/cost'

function namespaceParams(namespace: string) {
  return {
    params: {
      namespace: namespace || undefined,
    },
  }
}

export async function getCostSummary(clusterId: string, namespace: string): Promise<CostSummary> {
  const response = await httpClient.get<CostSummary>(
    `/api/clusters/${clusterId}/cost/summary`,
    namespaceParams(namespace),
  )

  return response.data
}

export async function getNamespaceCosts(clusterId: string, namespace: string): Promise<NamespaceCost[]> {
  const response = await httpClient.get<NamespaceCost[]>(
    `/api/clusters/${clusterId}/cost/namespaces`,
    namespaceParams(namespace),
  )

  return response.data
}

export async function getWorkloadCosts(clusterId: string, namespace: string): Promise<WorkloadCost[]> {
  const response = await httpClient.get<WorkloadCost[]>(
    `/api/clusters/${clusterId}/cost/workloads`,
    namespaceParams(namespace),
  )

  return response.data
}

export async function getCostRecommendations(
  clusterId: string,
  namespace: string,
): Promise<CostRecommendation[]> {
  const response = await httpClient.get<CostRecommendation[]>(
    `/api/clusters/${clusterId}/cost/recommendations`,
    namespaceParams(namespace),
  )

  return response.data
}
