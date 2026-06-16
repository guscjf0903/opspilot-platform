import type {
  KafkaConsumerGroupLag,
  KafkaConsumerGroupSummary,
  KafkaOverview,
  KafkaTopicSummary,
} from '../types/kafka'
import { httpClient } from './httpClient'

export async function getKafkaOverview(clusterId: string): Promise<KafkaOverview> {
  const response = await httpClient.get<KafkaOverview>(`/api/clusters/${clusterId}/kafka/overview`)

  return response.data
}

export async function getKafkaTopics(clusterId: string): Promise<KafkaTopicSummary[]> {
  const response = await httpClient.get<KafkaTopicSummary[]>(`/api/clusters/${clusterId}/kafka/topics`)

  return response.data
}

export async function getKafkaConsumerGroups(clusterId: string): Promise<KafkaConsumerGroupSummary[]> {
  const response = await httpClient.get<KafkaConsumerGroupSummary[]>(
    `/api/clusters/${clusterId}/kafka/consumer-groups`,
  )

  return response.data
}

export async function getKafkaTopConsumerGroupLags(
  clusterId: string,
  limit = 5,
): Promise<KafkaConsumerGroupLag[]> {
  const response = await httpClient.get<KafkaConsumerGroupLag[]>(`/api/clusters/${clusterId}/kafka/lag`, {
    params: { limit },
  })

  return response.data
}
