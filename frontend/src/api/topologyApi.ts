import { httpClient } from './httpClient'
import type { TopologyGraph } from '../types/topology'

export async function getTopology(clusterId: string, namespace: string): Promise<TopologyGraph> {
  const response = await httpClient.get<TopologyGraph>(
    `/api/clusters/${clusterId}/namespaces/${namespace}/topology`,
  )

  return response.data
}
