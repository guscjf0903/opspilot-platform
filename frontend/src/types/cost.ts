import type { ResourceStatus } from './kubernetes'

export type CostRiskLevel = 'low' | 'medium' | 'high'

export type CostRecommendationType = 'CPU_RIGHTSIZING' | 'MEMORY_RIGHTSIZING' | 'IDLE_WORKLOAD'

export interface CostSummary {
  clusterId: string
  currency: string
  estimatedMonthlyCost: number
  estimatedMonthlySaving: number
  namespaceCount: number
  workloadCount: number
  recommendationCount: number
  estimationMode: string
  collectedAt: string
}

export interface NamespaceCost {
  namespace: string
  currency: string
  estimatedMonthlyCost: number
  estimatedMonthlySaving: number
  workloadCount: number
  recommendationCount: number
}

export interface WorkloadCost {
  namespace: string
  kind: string
  name: string
  status: ResourceStatus
  currency: string
  estimatedMonthlyCost: number
  cpuMonthlyCost: number
  memoryMonthlyCost: number
  cpuRequestCores?: number
  memoryRequestBytes?: number
  cpuUsageCores?: number
  memoryUsageBytes?: number
  cpuUsagePercent?: number
  memoryUsagePercent?: number
  metricsAvailable: boolean
  metricsReason: string
}

export interface CostRecommendation {
  id: string
  type: CostRecommendationType
  namespace: string
  targetKind: string
  targetName: string
  title: string
  current: Record<string, string>
  recommendation: Record<string, string>
  currency: string
  estimatedMonthlySaving: number
  risk: CostRiskLevel
  confidence: number
  reason: string
}
