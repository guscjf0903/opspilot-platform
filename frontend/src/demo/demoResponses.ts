import type { InternalAxiosRequestConfig } from 'axios'
import type {
  ActionApprovalRequest,
  ActionAuditLog,
  ActionExecutionResult,
  ActionPreview,
  ActionRequest,
} from '../types/actions'
import type { IncidentAnalysisReport, IncidentAnalysisRequest } from '../types/aiAnalysis'
import type { CostRecommendation, CostSummary, NamespaceCost, WorkloadCost } from '../types/cost'
import type { DashboardSummary } from '../types/dashboard'
import type {
  KafkaConsumerGroupLag,
  KafkaConsumerGroupSummary,
  KafkaOverview,
  KafkaTopicSummary,
} from '../types/kafka'
import type {
  ClusterSummary,
  DeploymentSummary,
  EventSummary,
  NamespaceSummary,
  NodeSummary,
  PodSummary,
} from '../types/kubernetes'
import type { ResourceMetrics } from '../types/metrics'
import type { TopologyGraph } from '../types/topology'
import type {
  CostDailySnapshot,
  KubernetesWorkloadUsageSnapshot,
  TrendSnapshotCollectionResult,
} from '../types/trends'

const now = '2026-06-16T01:30:00Z'
const clusterId = 'local'
const namespace = 'sample-app'

const clusters: ClusterSummary[] = [
  {
    id: clusterId,
    name: 'OpsPilot Demo Cluster',
    provider: 'demo-snapshot',
    status: 'degraded',
    endpoint: 'snapshot://github-pages/demo',
  },
]

const namespaces: NamespaceSummary[] = [
  {
    kind: 'Namespace',
    name: 'sample-app',
    status: 'warning',
    reason: 'WarningEvents',
    message: 'payment-api readiness probe failure and order-consumer lag are present in the demo snapshot.',
    lastUpdatedAt: now,
  },
  {
    kind: 'Namespace',
    name: 'staging',
    status: 'warning',
    reason: 'IdleWorkload',
    message: 'staging contains idle workloads for cost optimization demonstration.',
    lastUpdatedAt: now,
  },
]

const deployments: DeploymentSummary[] = [
  {
    kind: 'Deployment',
    namespace,
    name: 'payment-api',
    status: 'critical',
    reason: 'MinimumReplicasUnavailable',
    message: 'Only 1 of 2 replicas is available after the latest rollout.',
    desiredReplicas: 2,
    availableReplicas: 1,
    readyReplicas: 1,
    updatedReplicas: 2,
    lastUpdatedAt: now,
  },
  {
    kind: 'Deployment',
    namespace,
    name: 'catalog-api',
    status: 'warning',
    reason: 'OverRequested',
    message: 'CPU request is much higher than recent p95 usage.',
    desiredReplicas: 2,
    availableReplicas: 2,
    readyReplicas: 2,
    updatedReplicas: 2,
    lastUpdatedAt: now,
  },
  {
    kind: 'Deployment',
    namespace,
    name: 'order-consumer',
    status: 'warning',
    reason: 'KafkaConsumerLag',
    message: 'orders.created consumer group lag is increasing.',
    desiredReplicas: 1,
    availableReplicas: 1,
    readyReplicas: 1,
    updatedReplicas: 1,
    lastUpdatedAt: now,
  },
  {
    kind: 'Deployment',
    namespace,
    name: 'frontend',
    status: 'healthy',
    reason: 'Available',
    message: 'All replicas are available.',
    desiredReplicas: 1,
    availableReplicas: 1,
    readyReplicas: 1,
    updatedReplicas: 1,
    lastUpdatedAt: now,
  },
]

const pods: PodSummary[] = [
  {
    kind: 'Pod',
    namespace,
    name: 'payment-api-6f9d7d8b7c-v4n2k',
    status: 'critical',
    reason: 'Unhealthy',
    message: 'Readiness probe failed: HTTP 503 on /ready.',
    phase: 'Running',
    nodeName: 'ip-10-0-12-34.ap-northeast-2.compute.internal',
    restartCount: 4,
    images: ['public.ecr.aws/demo/payment-api:2026.06.16'],
    lastUpdatedAt: now,
  },
  {
    kind: 'Pod',
    namespace,
    name: 'payment-api-6f9d7d8b7c-t8q7m',
    status: 'healthy',
    reason: 'Ready',
    message: 'Pod is ready.',
    phase: 'Running',
    nodeName: 'ip-10-0-15-82.ap-northeast-2.compute.internal',
    restartCount: 0,
    images: ['public.ecr.aws/demo/payment-api:2026.06.16'],
    lastUpdatedAt: now,
  },
  {
    kind: 'Pod',
    namespace,
    name: 'catalog-api-58d56f7b7d-nm2hc',
    status: 'warning',
    reason: 'OverRequested',
    message: 'CPU p95 is below 30% of request.',
    phase: 'Running',
    nodeName: 'ip-10-0-15-82.ap-northeast-2.compute.internal',
    restartCount: 0,
    images: ['public.ecr.aws/demo/catalog-api:2026.06.16'],
    lastUpdatedAt: now,
  },
  {
    kind: 'Pod',
    namespace,
    name: 'order-consumer-84f7b8f4bd-k2p9q',
    status: 'warning',
    reason: 'KafkaConsumerLag',
    message: 'Consumer is running but lag remains high.',
    phase: 'Running',
    nodeName: 'ip-10-0-12-34.ap-northeast-2.compute.internal',
    restartCount: 2,
    images: ['public.ecr.aws/demo/order-consumer:2026.06.16'],
    lastUpdatedAt: now,
  },
]

const events: EventSummary[] = [
  {
    kind: 'Event',
    namespace,
    name: 'payment-api-readiness-failed',
    status: 'critical',
    reason: 'Unhealthy',
    message: 'Readiness probe failed: HTTP probe failed with statuscode: 503.',
    type: 'Warning',
    involvedKind: 'Pod',
    involvedName: 'payment-api-6f9d7d8b7c-v4n2k',
    count: 8,
    lastUpdatedAt: now,
  },
  {
    kind: 'Event',
    namespace,
    name: 'order-consumer-backoff',
    status: 'warning',
    reason: 'BackOff',
    message: 'Back-off restarting failed container before lag started increasing.',
    type: 'Warning',
    involvedKind: 'Pod',
    involvedName: 'order-consumer-84f7b8f4bd-k2p9q',
    count: 3,
    lastUpdatedAt: '2026-06-16T01:22:00Z',
  },
]

const nodes: NodeSummary[] = [
  {
    kind: 'Node',
    name: 'ip-10-0-12-34.ap-northeast-2.compute.internal',
    status: 'healthy',
    reason: 'Ready',
    message: 'Node is ready.',
    unschedulable: false,
    kubeletVersion: 'v1.30.0-eks-demo',
    lastUpdatedAt: now,
  },
  {
    kind: 'Node',
    name: 'ip-10-0-15-82.ap-northeast-2.compute.internal',
    status: 'healthy',
    reason: 'Ready',
    message: 'Node is ready.',
    unschedulable: false,
    kubeletVersion: 'v1.30.0-eks-demo',
    lastUpdatedAt: now,
  },
]

const dashboard: DashboardSummary = {
  clusterStatus: 'warning',
  collectedAt: now,
  counts: {
    namespaceCount: 2,
    nodeCount: 2,
    deploymentCount: deployments.length,
    podCount: pods.length,
    criticalWorkloadCount: 1,
    warningWorkloadCount: 2,
    recentWarningEventCount: events.length,
  },
  unhealthyWorkloads: [
    {
      kind: 'Deployment',
      namespace,
      name: 'payment-api',
      status: 'critical',
      reason: 'MinimumReplicasUnavailable',
      message: 'Readiness probe failure after rollout.',
    },
    {
      kind: 'Deployment',
      namespace,
      name: 'order-consumer',
      status: 'warning',
      reason: 'KafkaConsumerLag',
      message: 'Consumer group lag is increasing.',
    },
  ],
  recentWarningEvents: events,
  restartCountTopPods: [
    {
      namespace,
      name: 'payment-api-6f9d7d8b7c-v4n2k',
      status: 'critical',
      reason: 'Unhealthy',
      restartCount: 4,
    },
    {
      namespace,
      name: 'order-consumer-84f7b8f4bd-k2p9q',
      status: 'warning',
      reason: 'BackOff',
      restartCount: 2,
    },
  ],
  namespaces: [
    { name: 'sample-app', status: 'warning', deploymentCount: 4, podCount: 4, unhealthyWorkloadCount: 3 },
    { name: 'staging', status: 'warning', deploymentCount: 2, podCount: 2, unhealthyWorkloadCount: 1 },
  ],
  nodes,
  nodeUsage: {
    status: 'available',
    reason: 'PROMETHEUS_SNAPSHOT',
    metrics: [
      {
        name: 'ip-10-0-12-34',
        cpuUsageCores: 0.62,
        memoryUsageBytes: 1889785610,
        cpuUsagePercent: 31,
        memoryUsagePercent: 58,
        status: 'healthy',
      },
      {
        name: 'ip-10-0-15-82',
        cpuUsageCores: 0.48,
        memoryUsageBytes: 1610612736,
        cpuUsagePercent: 24,
        memoryUsagePercent: 49,
        status: 'healthy',
      },
    ],
  },
  kafkaLag: {
    status: 'available',
    reason: 'KAFKA_ADMIN_SNAPSHOT',
    metrics: [
      {
        name: 'orders.created / order-consumer',
        value: 12840,
        unit: 'messages',
        status: 'warning',
        description: 'KafkaConsumerLag',
      },
    ],
  },
  cost: {
    status: 'available',
    reason: 'REQUEST_BASED_LOCAL_ESTIMATE',
    metrics: [
      {
        name: 'sample-app',
        value: 38.2,
        unit: 'USD/month',
        status: 'warning',
        description: 'Estimated monthly cost with rightsizing candidates.',
      },
    ],
  },
}

const topology: TopologyGraph = {
  clusterId,
  namespace,
  collectedAt: now,
  rootNodeId: 'deployment:sample-app:payment-api',
  nodes: [
    node('Deployment', 'payment-api', 'critical', 'MinimumReplicasUnavailable'),
    node('ReplicaSet', 'payment-api-6f9d7d8b7c', 'critical', 'Unhealthy'),
    node('Pod', 'payment-api-6f9d7d8b7c-v4n2k', 'critical', 'Unhealthy'),
    node('Pod', 'payment-api-6f9d7d8b7c-t8q7m', 'healthy', 'Ready'),
    node('Service', 'payment-api', 'warning', 'EndpointNotReady'),
    node('EndpointSlice', 'payment-api-k9r2m', 'warning', 'EndpointNotReady'),
    node('ConfigMap', 'payment-api-config', 'healthy', 'Referenced'),
    node('Secret', 'payment-api-secret', 'healthy', 'Referenced'),
    node('PersistentVolumeClaim', 'payment-cache', 'healthy', 'Bound'),
    node('Node', 'ip-10-0-12-34.ap-northeast-2.compute.internal', 'healthy', 'Ready'),
    node('KafkaTopic', 'orders.created', 'warning', 'KafkaConsumerLag'),
    node('KafkaConsumerGroup', 'order-consumer', 'warning', 'KafkaConsumerLag'),
    node('Deployment', 'order-consumer', 'warning', 'KafkaConsumerLag'),
    node('Pod', 'order-consumer-84f7b8f4bd-k2p9q', 'warning', 'KafkaConsumerLag'),
    node('Deployment', 'catalog-api', 'warning', 'OverRequested'),
    node('Pod', 'catalog-api-58d56f7b7d-nm2hc', 'warning', 'OverRequested'),
  ],
  edges: [
    edge('Deployment', 'payment-api', 'ReplicaSet', 'payment-api-6f9d7d8b7c', 'owns'),
    edge('ReplicaSet', 'payment-api-6f9d7d8b7c', 'Pod', 'payment-api-6f9d7d8b7c-v4n2k', 'owns'),
    edge('ReplicaSet', 'payment-api-6f9d7d8b7c', 'Pod', 'payment-api-6f9d7d8b7c-t8q7m', 'owns'),
    edge('Service', 'payment-api', 'EndpointSlice', 'payment-api-k9r2m', 'routes_to'),
    edge('EndpointSlice', 'payment-api-k9r2m', 'Pod', 'payment-api-6f9d7d8b7c-t8q7m', 'routes_to'),
    edge('Pod', 'payment-api-6f9d7d8b7c-v4n2k', 'ConfigMap', 'payment-api-config', 'mounts'),
    edge('Pod', 'payment-api-6f9d7d8b7c-v4n2k', 'Secret', 'payment-api-secret', 'mounts'),
    edge('Pod', 'payment-api-6f9d7d8b7c-v4n2k', 'PersistentVolumeClaim', 'payment-cache', 'mounts'),
    edge('Pod', 'payment-api-6f9d7d8b7c-v4n2k', 'Node', 'ip-10-0-12-34.ap-northeast-2.compute.internal', 'scheduled_on'),
    edge('Deployment', 'payment-api', 'KafkaTopic', 'orders.created', 'produces_to'),
    edge('KafkaTopic', 'orders.created', 'KafkaConsumerGroup', 'order-consumer', 'consumes_from'),
    edge('KafkaConsumerGroup', 'order-consumer', 'Deployment', 'order-consumer', 'depends_on'),
    edge('Deployment', 'order-consumer', 'Pod', 'order-consumer-84f7b8f4bd-k2p9q', 'owns'),
    edge('Deployment', 'catalog-api', 'Pod', 'catalog-api-58d56f7b7d-nm2hc', 'owns'),
  ],
}

const kafkaOverview: KafkaOverview = {
  clusterId,
  available: true,
  status: 'warning',
  reason: 'KafkaConsumerLag',
  brokerCount: 1,
  topicCount: 3,
  consumerGroupCount: 2,
  totalLag: 12840,
  laggingConsumerGroupCount: 1,
  collectedAt: now,
}

const kafkaTopics: KafkaTopicSummary[] = [
  {
    name: 'orders.created',
    partitionCount: 3,
    replicationFactor: 1,
    underReplicatedPartitionCount: 0,
    offlinePartitionCount: 0,
    status: 'warning',
    partitions: [0, 1, 2].map((partition) => ({
      topic: 'orders.created',
      partition,
      leaderId: 1,
      replicas: [1],
      inSyncReplicas: [1],
      underReplicated: false,
      offline: false,
      status: 'healthy',
    })),
  },
  {
    name: 'payments.approved',
    partitionCount: 1,
    replicationFactor: 1,
    underReplicatedPartitionCount: 0,
    offlinePartitionCount: 0,
    status: 'healthy',
    partitions: [
      {
        topic: 'payments.approved',
        partition: 0,
        leaderId: 1,
        replicas: [1],
        inSyncReplicas: [1],
        underReplicated: false,
        offline: false,
        status: 'healthy',
      },
    ],
  },
]

const kafkaConsumerGroups: KafkaConsumerGroupSummary[] = [
  {
    groupId: 'order-consumer',
    state: 'Stable',
    memberCount: 1,
    topicCount: 1,
    partitionCount: 3,
    totalLag: 12840,
    status: 'warning',
    reason: 'KafkaConsumerLag',
  },
  {
    groupId: 'billing-worker',
    state: 'Stable',
    memberCount: 1,
    topicCount: 1,
    partitionCount: 1,
    totalLag: 0,
    status: 'healthy',
    reason: 'NoLag',
  },
]

const kafkaLag: KafkaConsumerGroupLag[] = [
  {
    groupId: 'order-consumer',
    status: 'warning',
    reason: 'KafkaConsumerLag',
    totalLag: 12840,
    collectedAt: now,
    partitions: [
      { topic: 'orders.created', partition: 0, currentOffset: 425120, logEndOffset: 430400, lag: 5280 },
      { topic: 'orders.created', partition: 1, currentOffset: 391000, logEndOffset: 395260, lag: 4260 },
      { topic: 'orders.created', partition: 2, currentOffset: 288100, logEndOffset: 291400, lag: 3300 },
    ],
  },
]

const costSummary: CostSummary = {
  clusterId,
  currency: 'USD',
  estimatedMonthlyCost: 38.2,
  estimatedMonthlySaving: 16.4,
  namespaceCount: 2,
  workloadCount: 6,
  recommendationCount: 3,
  estimationMode: 'REQUEST_BASED_LOCAL_ESTIMATE',
  collectedAt: now,
}

const namespaceCosts: NamespaceCost[] = [
  { namespace: 'sample-app', currency: 'USD', estimatedMonthlyCost: 38.2, estimatedMonthlySaving: 16.4, workloadCount: 4, recommendationCount: 3 },
  { namespace: 'staging', currency: 'USD', estimatedMonthlyCost: 12.3, estimatedMonthlySaving: 9.8, workloadCount: 2, recommendationCount: 1 },
]

const workloadCosts: WorkloadCost[] = [
  workloadCost('payment-api', 'critical', 16.8, 0.8, 0.42, 0.52),
  workloadCost('catalog-api', 'warning', 12.6, 1.5, 0.18, 0.35),
  workloadCost('order-consumer', 'warning', 5.7, 0.5, 0.31, 0.48),
  workloadCost('frontend', 'healthy', 3.1, 0.25, 0.22, 0.29),
]

const costRecommendations: CostRecommendation[] = [
  {
    id: 'rec-catalog-cpu',
    type: 'CPU_RIGHTSIZING',
    namespace,
    targetKind: 'Deployment',
    targetName: 'catalog-api',
    title: 'catalog-api CPU request 조정',
    current: { cpuRequest: '1500m', cpuUsage: '180m p95', cpuUsageRatio: '12%' },
    recommendation: { cpuRequest: '400m' },
    currency: 'USD',
    estimatedMonthlySaving: 8.4,
    risk: 'low',
    confidence: 0.86,
    reason: '최근 p95 CPU 사용량이 request 대비 낮아 request 하향 후보입니다.',
  },
  {
    id: 'rec-staging-idle',
    type: 'IDLE_WORKLOAD',
    namespace: 'staging',
    targetKind: 'Deployment',
    targetName: 'report-worker',
    title: 'staging/report-worker scale-down',
    current: { action: 'replicas=1 유지', usageRatio: '< 5%' },
    recommendation: { action: '업무 시간 외 scale-to-zero 검토' },
    currency: 'USD',
    estimatedMonthlySaving: 6.1,
    risk: 'low',
    confidence: 0.78,
    reason: 'staging workload가 장기간 낮은 사용률을 보입니다.',
  },
]

const incidentReport: IncidentAnalysisReport = {
  analysisId: 'demo-analysis-payment-api',
  clusterId,
  namespace,
  targetKind: 'Deployment',
  targetName: 'payment-api',
  severity: 'critical',
  summary: 'payment-api는 최신 rollout 이후 readiness probe 실패로 일부 Pod가 Service endpoint에서 제외되었습니다.',
  impactScope: ['sample-app/payment-api', 'Service/payment-api', 'orders.created producer path'],
  rootCauseCandidates: [
    {
      title: '새 버전의 /ready endpoint가 DB connection check 실패를 critical로 반환',
      confidence: 0.82,
      evidenceIds: ['event-readiness', 'metric-cpu', 'topology-endpoint'],
    },
    {
      title: 'consumer lag 증가는 payment-api 장애 이후 주문 처리 지연과 시간상 연결',
      confidence: 0.68,
      evidenceIds: ['kafka-lag', 'event-restart'],
    },
  ],
  evidence: [
    {
      id: 'event-readiness',
      type: 'event',
      title: 'Readiness probe failed',
      message: 'HTTP probe failed with statuscode: 503 on /ready.',
      status: 'critical',
      timestamp: now,
    },
    {
      id: 'metric-cpu',
      type: 'metric',
      title: 'CPU 사용량은 request보다 낮음',
      message: 'CPU saturation보다 application readiness 조건 실패 가능성이 높습니다.',
      status: 'warning',
      timestamp: now,
    },
    {
      id: 'topology-endpoint',
      type: 'topology',
      title: 'EndpointSlice에서 실패 Pod 제외',
      message: 'Service가 healthy Pod 하나만 endpoint로 받고 있습니다.',
      status: 'critical',
      timestamp: now,
    },
    {
      id: 'kafka-lag',
      type: 'kafka',
      title: 'order-consumer lag 증가',
      message: 'orders.created total lag가 12,840 messages로 증가했습니다.',
      status: 'warning',
      timestamp: now,
    },
    {
      id: 'event-restart',
      type: 'event',
      title: 'order-consumer restart',
      message: 'lag 증가 직전 consumer Pod restart가 관측되었습니다.',
      status: 'warning',
      timestamp: '2026-06-16T01:22:00Z',
    },
  ],
  recommendations: [
    {
      action: 'payment-api rollout undo dry-run',
      risk: 'medium',
      reason: 'readiness 실패가 rollout 이후 발생했으므로 이전 ReplicaSet으로 rollback 가능성을 확인합니다.',
    },
    {
      action: 'order-consumer replica scale-out 검토',
      risk: 'low',
      reason: 'lag 해소가 필요하면 consumer replica를 임시로 늘리는 dry-run을 수행합니다.',
    },
  ],
  nextChecks: [
    'payment-api /ready check가 의존하는 DB connection pool 상태 확인',
    '최근 image tag와 이전 stable tag의 config diff 확인',
    'orders.created partition별 lag가 특정 partition에 쏠렸는지 확인',
  ],
  provider: 'demo-fixture',
  model: 'snapshot',
  tokenUsage: {
    inputTokens: 0,
    cachedInputTokens: 0,
    outputTokens: 0,
    reasoningOutputTokens: 0,
    totalTokens: 0,
  },
  latencyMs: 120,
  createdAt: now,
}

const actionAuditLogs: ActionAuditLog[] = [
  {
    id: 'audit-demo-rollback',
    clusterId,
    namespace,
    targetKind: 'Deployment',
    targetName: 'payment-api',
    actionType: 'ROLLOUT_UNDO',
    actor: 'viewer@example.com',
    actorRole: 'VIEWER',
    status: 'DRY_RUN',
    risk: 'HIGH',
    approvalRequired: true,
    approvalId: 'approval-demo-rollback',
    parameters: {},
    beforeState: { revision: '12', image: 'payment-api:2026.06.16' },
    afterState: { revision: '11', image: 'payment-api:2026.06.15' },
    diff: [{ field: 'image', beforeValue: 'payment-api:2026.06.16', afterValue: 'payment-api:2026.06.15' }],
    message: '데모 모드 미리보기입니다. 실제 Kubernetes 객체는 변경되지 않았습니다.',
    createdAt: now,
  },
]

const actionApprovals: ActionApprovalRequest[] = [
  {
    id: 'approval-demo-rollback',
    auditLogId: 'audit-demo-rollback',
    clusterId,
    namespace,
    targetKind: 'Deployment',
    targetName: 'payment-api',
    actionType: 'ROLLOUT_UNDO',
    requester: 'viewer@example.com',
    requesterRole: 'VIEWER',
    status: 'PENDING_APPROVAL',
    requestedAt: now,
  },
]

const trendWorkloads: KubernetesWorkloadUsageSnapshot[] = workloadCosts.map((cost, index) => ({
  id: `usage-${cost.name}`,
  clusterId,
  namespace: cost.namespace,
  workloadKind: cost.kind,
  workloadName: cost.name,
  status: cost.status,
  desiredReplicas: 2,
  availableReplicas: cost.status === 'critical' ? 1 : 2,
  readyReplicas: cost.status === 'critical' ? 1 : 2,
  cpuRequestCores: cost.cpuRequestCores,
  memoryRequestBytes: cost.memoryRequestBytes,
  cpuUsageAvgCores: cost.cpuUsageCores,
  cpuUsageP95Cores: (cost.cpuUsageCores ?? 0) * 1.4,
  memoryUsageAvgBytes: cost.memoryUsageBytes,
  memoryUsageP95Bytes: (cost.memoryUsageBytes ?? 0) * 1.2,
  cpuUsagePercent: cost.cpuUsagePercent,
  memoryUsagePercent: cost.memoryUsagePercent,
  metricsAvailable: true,
  metricsReason: 'PROMETHEUS_SNAPSHOT',
  rangeMinutes: 60,
  collectedAt: `2026-06-${12 + index}T01:30:00Z`,
}))

const trendCosts: CostDailySnapshot[] = workloadCosts.map((cost, index) => ({
  id: `cost-${cost.name}`,
  clusterId,
  namespace: cost.namespace,
  workloadKind: cost.kind,
  workloadName: cost.name,
  status: cost.status,
  currency: cost.currency,
  estimatedDailyCost: cost.estimatedMonthlyCost / 30,
  estimatedMonthlyCost: cost.estimatedMonthlyCost,
  cpuMonthlyCost: cost.cpuMonthlyCost,
  memoryMonthlyCost: cost.memoryMonthlyCost,
  estimatedMonthlySaving: cost.name === 'catalog-api' ? 8.4 : 0,
  estimationMode: 'REQUEST_BASED_LOCAL_ESTIMATE',
  snapshotDate: `2026-06-${12 + index}`,
  collectedAt: `2026-06-${12 + index}T01:30:00Z`,
}))

export function resolveDemoResponse(config: InternalAxiosRequestConfig) {
  const method = (config.method ?? 'get').toUpperCase()
  const url = String(config.url ?? '')
  const path = url.split('?')[0]

  if (method === 'GET' && path === '/actuator/health') return { status: 'UP' }
  if (method === 'GET' && path === '/api/clusters') return clusters
  if (method === 'GET' && path.endsWith('/namespaces')) return namespaces
  if (method === 'GET' && path.endsWith('/deployments')) return deploymentsForPath(path)
  if (method === 'GET' && path.endsWith('/pods')) return podsForPath(path)
  if (method === 'GET' && path.endsWith('/events')) return eventsForPath(path)
  if (method === 'GET' && path.endsWith('/nodes')) return nodes
  if (method === 'GET' && path.endsWith('/dashboard')) return dashboardForParams(config.params)
  if (method === 'GET' && path.endsWith('/topology')) return topology
  if (method === 'GET' && path.includes('/metrics')) return metricsForPath(path)
  if (method === 'GET' && path.endsWith('/kafka/overview')) return kafkaOverview
  if (method === 'GET' && path.endsWith('/kafka/topics')) return kafkaTopics
  if (method === 'GET' && path.endsWith('/kafka/consumer-groups')) return kafkaConsumerGroups
  if (method === 'GET' && path.endsWith('/kafka/lag')) return kafkaLag
  if (method === 'GET' && path.endsWith('/cost/summary')) return costSummaryForParams(config.params)
  if (method === 'GET' && path.endsWith('/cost/namespaces')) return filterNamespaceCosts(config.params)
  if (method === 'GET' && path.endsWith('/cost/workloads')) return filterWorkloadCosts(config.params)
  if (method === 'GET' && path.endsWith('/cost/recommendations')) return filterCostRecommendations(config.params)
  if (method === 'GET' && path.endsWith('/actions/audit-logs')) return filterActionLogs(config.params)
  if (method === 'GET' && path.endsWith('/actions/approvals')) return filterApprovals(config.params)
  if (method === 'POST' && path.endsWith('/actions/dry-run')) return actionPreview(config.data)
  if (method === 'POST' && path.endsWith('/actions/execute')) return actionResult(config.data)
  if (method === 'POST' && path.includes('/actions/approvals/')) return approvalDecision(path)
  if (method === 'POST' && path.endsWith('/analysis/incidents')) return analysisForRequest(config.data)
  if (method === 'GET' && path.includes('/analysis/incidents/')) return incidentReport
  if (method === 'GET' && path.endsWith('/trends/kubernetes/workloads')) return trendWorkloads
  if (method === 'GET' && path.endsWith('/trends/cost/workloads')) return trendCosts
  if (method === 'POST' && path.endsWith('/trends/snapshots')) {
    return {
      clusterId,
      kubernetesSnapshotCount: trendWorkloads.length,
      costSnapshotCount: trendCosts.length,
      collectedAt: now,
    } satisfies TrendSnapshotCollectionResult
  }

  return {}
}

function node(kind: string, name: string, status: TopologyGraph['nodes'][number]['status'], reason: string) {
  return {
    id: `${kind.toLowerCase()}:${namespace}:${name}`,
    kind,
    namespace,
    name,
    status,
    reason,
    message: `${kind} ${name} is ${status} in the demo snapshot.`,
  }
}

function edge(sourceKind: string, sourceName: string, targetKind: string, targetName: string, type: string) {
  return {
    source: `${sourceKind.toLowerCase()}:${namespace}:${sourceName}`,
    target: `${targetKind.toLowerCase()}:${namespace}:${targetName}`,
    type,
  }
}

function workloadCost(
  name: string,
  status: WorkloadCost['status'],
  estimatedMonthlyCost: number,
  cpuRequestCores: number,
  cpuUsagePercent: number,
  memoryUsagePercent: number,
): WorkloadCost {
  const memoryRequestBytes = 536870912
  return {
    namespace,
    kind: 'Deployment',
    name,
    status,
    currency: 'USD',
    estimatedMonthlyCost,
    cpuMonthlyCost: estimatedMonthlyCost * 0.58,
    memoryMonthlyCost: estimatedMonthlyCost * 0.42,
    cpuRequestCores,
    memoryRequestBytes,
    cpuUsageCores: cpuRequestCores * cpuUsagePercent,
    memoryUsageBytes: memoryRequestBytes * memoryUsagePercent,
    cpuUsagePercent: cpuUsagePercent * 100,
    memoryUsagePercent: memoryUsagePercent * 100,
    metricsAvailable: true,
    metricsReason: 'PROMETHEUS_SNAPSHOT',
  }
}

function namespaceFromPath(path: string) {
  return decodeURIComponent(path.match(/\/namespaces\/([^/]+)/)?.[1] ?? namespace)
}

function deploymentsForPath(path: string) {
  return deployments.filter((deployment) => deployment.namespace === namespaceFromPath(path))
}

function podsForPath(path: string) {
  return pods.filter((pod) => pod.namespace === namespaceFromPath(path))
}

function eventsForPath(path: string) {
  return events.filter((event) => event.namespace === namespaceFromPath(path))
}

function selectedNamespace(params: unknown) {
  if (!params || typeof params !== 'object') return ''
  const value = (params as Record<string, unknown>).namespace
  return typeof value === 'string' ? value : ''
}

function dashboardForParams(params: unknown): DashboardSummary {
  const selected = selectedNamespace(params)
  return selected ? { ...dashboard, selectedNamespace: selected } : dashboard
}

function costSummaryForParams(params: unknown): CostSummary {
  const selected = selectedNamespace(params)
  const workloads = filterWorkloadCosts(params)
  const recommendations = filterCostRecommendations(params)
  return selected
    ? {
        ...costSummary,
        estimatedMonthlyCost: workloads.reduce((sum, item) => sum + item.estimatedMonthlyCost, 0),
        estimatedMonthlySaving: recommendations.reduce((sum, item) => sum + item.estimatedMonthlySaving, 0),
        namespaceCount: 1,
        workloadCount: workloads.length,
        recommendationCount: recommendations.length,
      }
    : costSummary
}

function filterNamespaceCosts(params: unknown) {
  const selected = selectedNamespace(params)
  return selected ? namespaceCosts.filter((item) => item.namespace === selected) : namespaceCosts
}

function filterWorkloadCosts(params: unknown) {
  const selected = selectedNamespace(params)
  return selected ? workloadCosts.filter((item) => item.namespace === selected) : workloadCosts
}

function filterCostRecommendations(params: unknown) {
  const selected = selectedNamespace(params)
  return selected ? costRecommendations.filter((item) => item.namespace === selected) : costRecommendations
}

function filterActionLogs(params: unknown) {
  if (!params || typeof params !== 'object') return actionAuditLogs
  const query = params as Record<string, unknown>
  return actionAuditLogs.filter((log) => {
    const namespaceMatches = !query.namespace || log.namespace === query.namespace
    const targetMatches = !query.targetName || log.targetName === query.targetName
    return namespaceMatches && targetMatches
  })
}

function filterApprovals(params: unknown) {
  if (!params || typeof params !== 'object') return actionApprovals
  const status = (params as Record<string, unknown>).status
  return typeof status === 'string' ? actionApprovals.filter((approval) => approval.status === status) : actionApprovals
}

function parsePayload<T>(payload: unknown): T {
  return typeof payload === 'string' ? JSON.parse(payload) as T : payload as T
}

function actionPreview(payload: unknown): ActionPreview {
  const request = parsePayload<ActionRequest>(payload)
  const replicas = request.parameters?.replicas ?? '2'
  const approvalRequired = request.type === 'ROLLOUT_UNDO' || request.namespace === 'production'
  return {
    clusterId,
    type: request.type,
    namespace: request.namespace,
    targetKind: request.targetKind,
    targetName: request.targetName,
    risk: approvalRequired ? 'HIGH' : 'LOW',
    approvalRequired,
    executable: false,
    beforeState: { replicas: request.type === 'SCALE_DEPLOYMENT' ? '2' : 'current' },
    afterState: { replicas: request.type === 'SCALE_DEPLOYMENT' ? replicas : 'preview-only' },
    diff: [
      {
        field: request.type === 'SCALE_DEPLOYMENT' ? 'replicas' : 'operation',
        beforeValue: request.type === 'SCALE_DEPLOYMENT' ? '2' : 'current',
        afterValue: request.type === 'SCALE_DEPLOYMENT' ? replicas : request.type,
      },
    ],
    warnings: ['데모 스냅샷 모드입니다. 실제 Kubernetes 리소스는 변경하지 않습니다.'],
    message: '실제 클러스터를 건드리지 않고 dry-run 미리보기를 생성했습니다.',
    createdAt: now,
  }
}

function actionResult(payload: unknown): ActionExecutionResult {
  const preview = actionPreview(payload)
  return {
    auditLogId: 'audit-demo-execute-preview',
    approvalId: preview.approvalRequired ? 'approval-demo-required' : undefined,
    status: preview.approvalRequired ? 'PENDING_APPROVAL' : 'DRY_RUN',
    preview,
    message: '데모 스냅샷 모드에서는 실행을 차단합니다. 이 결과는 승인과 감사 로그 흐름만 보여줍니다.',
    executedAt: now,
  }
}

function approvalDecision(path: string): ActionApprovalRequest {
  const approved = path.endsWith('/approve')
  return {
    ...actionApprovals[0],
    status: approved ? 'APPROVED' : 'REJECTED',
    decidedBy: 'viewer@example.com',
    decidedAt: now,
    decisionReason: approved ? '데모 모드에서 승인 처리했습니다.' : '데모 모드에서 반려 처리했습니다.',
  }
}

function analysisForRequest(payload: unknown): IncidentAnalysisReport {
  const request = parsePayload<IncidentAnalysisRequest>(payload)
  return {
    ...incidentReport,
    namespace: request.namespace,
    targetKind: request.targetKind,
    targetName: request.targetName,
  }
}

function metricsForPath(path: string): ResourceMetrics {
  const resourceName = decodeURIComponent(path.match(/\/(?:workloads|nodes)\/(?:[^/]+\/)?([^/]+)\/metrics/)?.[1] ?? 'payment-api')
  const points = Array.from({ length: 8 }, (_, index) => ({
    timestamp: new Date(Date.parse(now) - (7 - index) * 5 * 60 * 1000).toISOString(),
    value: 0.15 + index * 0.025,
  }))
  return {
    status: 'available',
    reason: 'PROMETHEUS_SNAPSHOT',
    clusterId,
    namespace,
    kind: path.includes('/nodes/') ? 'Node' : 'Deployment',
    name: resourceName,
    collectedAt: now,
    rangeMinutes: 30,
    cpu: { name: 'cpu', unit: 'cores', points },
    memory: {
      name: 'memory',
      unit: 'bytes',
      points: points.map((point, index) => ({ ...point, value: 180000000 + index * 18000000 })),
    },
    summary: {
      cpuUsageCores: 0.31,
      memoryUsageBytes: 306000000,
      cpuRequestCores: 0.8,
      memoryRequestBytes: 536870912,
      cpuUsagePercent: 38.8,
      memoryUsagePercent: 57,
    },
  }
}
