export type MetricsAvailabilityStatus = 'available' | 'unavailable'

export interface MetricPoint {
  timestamp: string
  value: number
}

export interface MetricSeries {
  name: string
  unit: 'cores' | 'bytes' | string
  points: MetricPoint[]
}

export interface ResourceMetricSummary {
  cpuUsageCores?: number
  memoryUsageBytes?: number
  cpuRequestCores?: number
  memoryRequestBytes?: number
  cpuCapacityCores?: number
  memoryCapacityBytes?: number
  cpuUsagePercent?: number
  memoryUsagePercent?: number
}

export interface ResourceMetrics {
  status: MetricsAvailabilityStatus
  reason: string
  clusterId: string
  namespace?: string
  kind: string
  name: string
  collectedAt: string
  rangeMinutes: number
  cpu: MetricSeries
  memory: MetricSeries
  summary: ResourceMetricSummary
}
