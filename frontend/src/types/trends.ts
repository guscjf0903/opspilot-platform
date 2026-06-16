import type { ResourceStatus } from './kubernetes'

export interface KubernetesWorkloadUsageSnapshot {
  id: string
  clusterId: string
  namespace: string
  workloadKind: string
  workloadName: string
  status: ResourceStatus
  desiredReplicas: number
  availableReplicas: number
  readyReplicas: number
  cpuRequestCores?: number
  memoryRequestBytes?: number
  cpuUsageAvgCores?: number
  cpuUsageP95Cores?: number
  memoryUsageAvgBytes?: number
  memoryUsageP95Bytes?: number
  cpuUsagePercent?: number
  memoryUsagePercent?: number
  metricsAvailable: boolean
  metricsReason: string
  rangeMinutes: number
  collectedAt: string
}

export interface CostDailySnapshot {
  id: string
  clusterId: string
  namespace: string
  workloadKind: string
  workloadName: string
  status: ResourceStatus
  currency: string
  estimatedDailyCost: number
  estimatedMonthlyCost: number
  cpuMonthlyCost: number
  memoryMonthlyCost: number
  estimatedMonthlySaving: number
  estimationMode: string
  snapshotDate: string
  collectedAt: string
}

export interface TrendSnapshotCollectionResult {
  clusterId: string
  kubernetesSnapshotCount: number
  costSnapshotCount: number
  collectedAt: string
}
