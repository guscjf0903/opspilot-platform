import { httpClient } from './httpClient'
import type {
  CostDailySnapshot,
  KubernetesWorkloadUsageSnapshot,
  TrendSnapshotCollectionResult,
} from '../types/trends'

interface TrendQueryParams {
  namespace?: string
  workloadName?: string
  from?: string
  to?: string
}

function compactParams(params: TrendQueryParams) {
  return {
    params: Object.fromEntries(
      Object.entries(params).filter(([, value]) => value !== undefined && value !== ''),
    ),
  }
}

export async function collectTrendSnapshots(
  clusterId: string,
  namespace: string,
): Promise<TrendSnapshotCollectionResult> {
  const response = await httpClient.post<TrendSnapshotCollectionResult>(
    `/api/clusters/${clusterId}/trends/snapshots`,
    undefined,
    compactParams({ namespace }),
  )

  return response.data
}

export async function getKubernetesWorkloadUsageTrend(
  clusterId: string,
  params: TrendQueryParams,
): Promise<KubernetesWorkloadUsageSnapshot[]> {
  const response = await httpClient.get<KubernetesWorkloadUsageSnapshot[]>(
    `/api/clusters/${clusterId}/trends/kubernetes/workloads`,
    compactParams(params),
  )

  return response.data
}

export async function getCostDailyTrend(
  clusterId: string,
  params: TrendQueryParams,
): Promise<CostDailySnapshot[]> {
  const response = await httpClient.get<CostDailySnapshot[]>(
    `/api/clusters/${clusterId}/trends/cost/workloads`,
    compactParams(params),
  )

  return response.data
}
