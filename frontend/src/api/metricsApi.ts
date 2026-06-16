import { httpClient } from './httpClient'
import type { ResourceMetrics } from '../types/metrics'

export async function getWorkloadMetrics(
  clusterId: string,
  namespace: string,
  kind: string,
  name: string,
  rangeMinutes: number,
): Promise<ResourceMetrics> {
  const response = await httpClient.get<ResourceMetrics>(
    `/api/clusters/${clusterId}/namespaces/${namespace}/workloads/${kind}/${name}/metrics`,
    {
      params: { rangeMinutes },
    },
  )

  return response.data
}

export async function getNodeMetrics(
  clusterId: string,
  nodeName: string,
  rangeMinutes: number,
): Promise<ResourceMetrics> {
  const response = await httpClient.get<ResourceMetrics>(`/api/clusters/${clusterId}/nodes/${nodeName}/metrics`, {
    params: { rangeMinutes },
  })

  return response.data
}
