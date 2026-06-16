import { useQuery } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'
import {
  getKafkaConsumerGroups,
  getKafkaOverview,
  getKafkaTopics,
  getKafkaTopConsumerGroupLags,
} from '../../api/kafkaApi'

export function useKafkaOverviewQuery(clusterId: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['kafka', clusterId.value, 'overview']),
    queryFn: () => getKafkaOverview(clusterId.value),
    refetchInterval: 15_000,
  })
}

export function useKafkaTopicsQuery(clusterId: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['kafka', clusterId.value, 'topics']),
    queryFn: () => getKafkaTopics(clusterId.value),
    refetchInterval: 15_000,
  })
}

export function useKafkaConsumerGroupsQuery(clusterId: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['kafka', clusterId.value, 'consumer-groups']),
    queryFn: () => getKafkaConsumerGroups(clusterId.value),
    refetchInterval: 15_000,
  })
}

export function useKafkaTopConsumerGroupLagsQuery(clusterId: Ref<string>, limit = 5) {
  return useQuery({
    queryKey: computed(() => ['kafka', clusterId.value, 'lag', limit]),
    queryFn: () => getKafkaTopConsumerGroupLags(clusterId.value, limit),
    refetchInterval: 15_000,
  })
}
