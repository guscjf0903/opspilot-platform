import type { ResourceStatus } from './kubernetes'

export interface TopologyNode {
  id: string
  kind: string
  namespace?: string
  name: string
  status: ResourceStatus
  reason: string
  message: string
}

export interface TopologyEdge {
  source: string
  target: string
  type: string
}

export interface TopologyGraph {
  clusterId: string
  namespace: string
  rootNodeId?: string
  collectedAt: string
  nodes: TopologyNode[]
  edges: TopologyEdge[]
}
