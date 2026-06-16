import type { EventSummary, NodeSummary, ResourceStatus } from './kubernetes'

export interface DashboardCounts {
  namespaceCount: number
  nodeCount: number
  deploymentCount: number
  podCount: number
  criticalWorkloadCount: number
  warningWorkloadCount: number
  recentWarningEventCount: number
}

export interface DashboardWorkloadSummary {
  kind: string
  namespace: string
  name: string
  status: ResourceStatus
  reason: string
  message: string
}

export interface DashboardRestartSummary {
  namespace: string
  name: string
  status: ResourceStatus
  reason: string
  restartCount: number
}

export interface DashboardNamespaceSummary {
  name: string
  status: ResourceStatus
  deploymentCount: number
  podCount: number
  unhealthyWorkloadCount: number
}

export interface DashboardExternalSignal {
  status: 'available' | 'unavailable'
  reason: string
  metrics: DashboardExternalMetric[]
}

export interface DashboardExternalMetric {
  name: string
  cpuUsageCores?: number
  memoryUsageBytes?: number
  cpuUsagePercent?: number
  memoryUsagePercent?: number
  value?: number
  unit?: string
  status?: ResourceStatus
  description?: string
}

export interface DashboardSummary {
  clusterStatus: ResourceStatus
  selectedNamespace?: string
  collectedAt: string
  counts: DashboardCounts
  unhealthyWorkloads: DashboardWorkloadSummary[]
  recentWarningEvents: EventSummary[]
  restartCountTopPods: DashboardRestartSummary[]
  namespaces: DashboardNamespaceSummary[]
  nodes: NodeSummary[]
  nodeUsage: DashboardExternalSignal
  kafkaLag: DashboardExternalSignal
  cost: DashboardExternalSignal
}
