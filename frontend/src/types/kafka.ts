import type { ResourceStatus } from './kubernetes'

export interface KafkaOverview {
  clusterId: string
  available: boolean
  status: ResourceStatus
  reason: string
  brokerCount: number
  topicCount: number
  consumerGroupCount: number
  totalLag: number
  laggingConsumerGroupCount: number
  collectedAt: string
}

export interface KafkaPartitionSummary {
  topic: string
  partition: number
  leaderId?: number
  replicas: number[]
  inSyncReplicas: number[]
  underReplicated: boolean
  offline: boolean
  status: ResourceStatus
}

export interface KafkaTopicSummary {
  name: string
  partitionCount: number
  replicationFactor: number
  underReplicatedPartitionCount: number
  offlinePartitionCount: number
  status: ResourceStatus
  partitions: KafkaPartitionSummary[]
}

export interface KafkaConsumerGroupSummary {
  groupId: string
  state: string
  memberCount: number
  topicCount: number
  partitionCount: number
  totalLag: number
  status: ResourceStatus
  reason: string
}

export interface KafkaPartitionLag {
  topic: string
  partition: number
  currentOffset?: number
  logEndOffset?: number
  lag: number
}

export interface KafkaConsumerGroupLag {
  groupId: string
  status: ResourceStatus
  reason: string
  totalLag: number
  collectedAt: string
  partitions: KafkaPartitionLag[]
}
