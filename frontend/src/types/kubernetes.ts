export type ResourceStatus = 'healthy' | 'warning' | 'critical' | 'unknown'

export interface ResourceSummary {
  kind: string
  name: string
  status: ResourceStatus
  reason: string
  message: string
  lastUpdatedAt?: string
}

export interface ClusterSummary {
  id: string
  name: string
  provider: string
  status: 'connected' | 'disconnected' | 'degraded'
  endpoint: string
}

export interface NamespaceSummary extends ResourceSummary {}

export interface DeploymentSummary extends ResourceSummary {
  namespace: string
  desiredReplicas: number
  availableReplicas: number
  readyReplicas: number
  updatedReplicas: number
}

export interface PodSummary extends ResourceSummary {
  namespace: string
  phase?: string
  nodeName?: string
  restartCount: number
  images: string[]
}

export interface EventSummary extends ResourceSummary {
  namespace: string
  type?: string
  involvedKind?: string
  involvedName?: string
  count: number
}

export interface NodeSummary extends ResourceSummary {
  unschedulable: boolean
  kubeletVersion?: string
}
