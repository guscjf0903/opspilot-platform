import type { DashboardSummary } from '../types/dashboard'
import { httpClient } from './httpClient'

export async function getDashboard(clusterId: string, namespace: string): Promise<DashboardSummary> {
  const response = await httpClient.get<DashboardSummary>(`/api/clusters/${clusterId}/dashboard`, {
    params: namespace ? { namespace } : undefined,
  })

  return response.data
}
