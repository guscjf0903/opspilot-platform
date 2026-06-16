import { httpClient } from './httpClient'
import type {
  ClusterSummary,
  DeploymentSummary,
  EventSummary,
  NamespaceSummary,
  NodeSummary,
  PodSummary,
} from '../types/kubernetes'

export async function getClusters(): Promise<ClusterSummary[]> {
  const response = await httpClient.get<ClusterSummary[]>('/api/clusters')

  return response.data
}

export async function getNamespaces(clusterId: string): Promise<NamespaceSummary[]> {
  const response = await httpClient.get<NamespaceSummary[]>(`/api/clusters/${clusterId}/namespaces`)

  return response.data
}

export async function getDeployments(clusterId: string, namespace: string): Promise<DeploymentSummary[]> {
  const response = await httpClient.get<DeploymentSummary[]>(
    `/api/clusters/${clusterId}/namespaces/${namespace}/deployments`,
  )

  return response.data
}

export async function getPods(clusterId: string, namespace: string): Promise<PodSummary[]> {
  const response = await httpClient.get<PodSummary[]>(
    `/api/clusters/${clusterId}/namespaces/${namespace}/pods`,
  )

  return response.data
}

export async function getEvents(clusterId: string, namespace: string): Promise<EventSummary[]> {
  const response = await httpClient.get<EventSummary[]>(
    `/api/clusters/${clusterId}/namespaces/${namespace}/events`,
  )

  return response.data
}

export async function getNodes(clusterId: string): Promise<NodeSummary[]> {
  const response = await httpClient.get<NodeSummary[]>(`/api/clusters/${clusterId}/nodes`)

  return response.data
}
