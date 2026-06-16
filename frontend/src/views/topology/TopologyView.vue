<script setup lang="ts">
import {
  MarkerType,
  VueFlow,
  type Edge as FlowEdge,
  type Node as FlowNode,
  type NodeMouseEvent,
} from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import { computed, ref, watch, watchEffect } from 'vue'
import { storeToRefs } from 'pinia'
import { useNamespacesQuery } from '../../composables/queries/useKubernetesInventoryQueries'
import { useTopologyQuery } from '../../composables/queries/useTopologyQuery'
import { translateResourceReason, translateResourceStatus } from '../../i18n/messages'
import { useClusterSelectionStore } from '../../stores/clusterSelection'
import { useUiPreferencesStore } from '../../stores/uiPreferences'
import type { ResourceStatus } from '../../types/kubernetes'
import type { TopologyEdge } from '../../types/topology'
import type { TopologyNode } from '../../types/topology'

interface TopologyNodeData extends TopologyNode {}
interface TopologyComponent {
  id: number
  nodeIds: Set<string>
  terminalIds: Set<string>
}
interface LayoutNode {
  visualId: string
  originalId: string
  data: TopologyNodeData
  componentId: number
}
interface LayoutResult {
  nodes: LayoutNode[]
  edges: Array<TopologyEdge & { visualSource: string, visualTarget: string }>
}

const externalKinds = new Set(['KafkaTopic', 'KafkaConsumerGroup'])
const terminalKinds = new Set(['Node', 'PersistentVolume'])
const kindColumn: Record<string, number> = {
  Ingress: 0,
  HorizontalPodAutoscaler: 0,
  Deployment: 0,
  Service: 1,
  ReplicaSet: 1,
  EndpointSlice: 2,
  Pod: 2,
  ConfigMap: 3,
  Secret: 3,
  PersistentVolumeClaim: 3,
  KafkaTopic: 3,
  PersistentVolume: 4,
  Node: 4,
  KafkaConsumerGroup: 4,
}
const kindOrder: Record<string, number> = {
  Ingress: 0,
  Service: 1,
  HorizontalPodAutoscaler: 2,
  Deployment: 3,
  ReplicaSet: 4,
  EndpointSlice: 5,
  Pod: 6,
  ConfigMap: 7,
  Secret: 8,
  PersistentVolumeClaim: 9,
  PersistentVolume: 10,
  Node: 11,
  KafkaTopic: 12,
  KafkaConsumerGroup: 13,
}
const columnGap = 198
const rowGap = 128
const componentGap = 104

const selectionStore = useClusterSelectionStore()
const {
  clusterId,
  topologyNamespace,
  topologyResourceKind,
  topologyUnhealthyOnly,
  topologyIncludeKafka,
} = storeToRefs(selectionStore)
const uiPreferences = useUiPreferencesStore()
const { locale } = storeToRefs(uiPreferences)
const { t } = uiPreferences
const selectedNodeId = ref('')

const namespacesQuery = useNamespacesQuery(clusterId)
const topologyQuery = useTopologyQuery(clusterId, topologyNamespace)

const resourceKinds = computed(() =>
  [...new Set((topologyQuery.data.value?.nodes ?? []).map((node) => node.kind))].sort(),
)
const visibleTopologyNodes = computed(() =>
  (topologyQuery.data.value?.nodes ?? []).filter((node) => {
    if (!topologyIncludeKafka.value && externalKinds.has(node.kind)) {
      return false
    }
    if (topologyResourceKind.value && node.kind !== topologyResourceKind.value) {
      return false
    }

    return !topologyUnhealthyOnly.value || node.status !== 'healthy'
  }),
)
const visibleNodeIds = computed(() => new Set(visibleTopologyNodes.value.map((node) => node.id)))
const visibleTopologyEdges = computed(() =>
  (topologyQuery.data.value?.edges ?? [])
    .filter((edge) => visibleNodeIds.value.has(edge.source) && visibleNodeIds.value.has(edge.target)),
)
const topologyLayout = computed<LayoutResult>(() =>
  layoutTopology(visibleTopologyNodes.value, visibleTopologyEdges.value),
)
const flowNodes = computed<FlowNode<TopologyNodeData>[]>(() => {
  let nextBaseY = 28
  const componentBaseY = new Map<number, number>()
  const componentHeights = new Map<number, number>()

  const sortedComponents = [...new Set(topologyLayout.value.nodes.map((node) => node.componentId))].sort(
    (first, second) => first - second,
  )
  sortedComponents.forEach((componentId) => {
    const componentNodes = topologyLayout.value.nodes.filter((node) => node.componentId === componentId)
    const maxRowsByColumn = new Map<number, number>()
    componentNodes.forEach((node) => {
      const column = kindColumn[node.data.kind] ?? 5
      maxRowsByColumn.set(column, (maxRowsByColumn.get(column) ?? 0) + 1)
    })
    const maxRows = Math.max(1, ...maxRowsByColumn.values())
    componentBaseY.set(componentId, nextBaseY)
    componentHeights.set(componentId, maxRows * rowGap + 34)
    nextBaseY += (componentHeights.get(componentId) ?? rowGap) + componentGap
  })

  const columnRowsByComponent = new Map<number, Map<number, number>>()

  return [...topologyLayout.value.nodes]
    .sort(layoutNodeComparator)
    .map((layoutNode) => {
      const column = kindColumn[layoutNode.data.kind] ?? 5
      const columnRows = getOrCreateColumnRows(columnRowsByComponent, layoutNode.componentId)
      const row = columnRows.get(column) ?? 0
      columnRows.set(column, row + 1)

      return {
        id: layoutNode.visualId,
        position: {
          x: 32 + column * columnGap,
          y: (componentBaseY.get(layoutNode.componentId) ?? 28) + row * rowGap,
        },
        data: layoutNode.data,
        class: `topology-node status-${layoutNode.data.status}${selectedNodeId.value === layoutNode.visualId ? ' selected' : ''}`,
      }
    })
})
const flowEdges = computed<FlowEdge[]>(() =>
  topologyLayout.value.edges
    .map((edge, index) => ({
      id: `${edge.visualSource}-${edge.visualTarget}-${edge.type}-${index}`,
      source: edge.visualSource,
      target: edge.visualTarget,
      label: edge.type,
      type: 'smoothstep',
      class: `topology-edge edge-${edge.type}`,
      markerEnd: MarkerType.ArrowClosed,
    })),
)
const selectedNode = computed(() =>
  flowNodes.value.find((node) => node.id === selectedNodeId.value)?.data,
)
const unhealthyCount = computed(
  () => (topologyQuery.data.value?.nodes ?? []).filter((node) => node.status !== 'healthy').length,
)

watchEffect(() => {
  const namespaces = namespacesQuery.data.value ?? []

  if (namespaces.length > 0 && !namespaces.some((item) => item.name === topologyNamespace.value)) {
    selectionStore.selectTopologyNamespace(namespaces[0].name)
  }
})

watch(flowNodes, (nodes) => {
  if (selectedNodeId.value && !nodes.some((node) => node.id === selectedNodeId.value)) {
    selectedNodeId.value = ''
  }
})

function selectNode(event: NodeMouseEvent) {
  selectedNodeId.value = event.node.id
}

function statusClass(status: ResourceStatus) {
  return `resource-status status-${status}`
}

function statusLabel(status: ResourceStatus) {
  return translateResourceStatus(locale.value, status)
}

function reasonLabel(reason: string) {
  return translateResourceReason(locale.value, reason)
}

function layoutTopology(nodes: TopologyNode[], edges: TopologyEdge[]): LayoutResult {
  const nodesById = new Map(nodes.map((node) => [node.id, node]))
  const components = getNonTerminalComponents(nodes, edges)
  const layouts: LayoutNode[] = []
  const layoutEdges: LayoutResult['edges'] = []

  components.forEach((component) => {
    component.nodeIds.forEach((nodeId) => {
      const node = nodesById.get(nodeId)
      if (node) {
        layouts.push({ visualId: node.id, originalId: node.id, data: node, componentId: component.id })
      }
    })
    component.terminalIds.forEach((nodeId) => {
      const node = nodesById.get(nodeId)
      if (node) {
        const visualId = terminalVisualId(node.id, component.id)
        layouts.push({
          visualId,
          originalId: node.id,
          data: { ...node, id: visualId },
          componentId: component.id,
        })
      }
    })
  })

  edges.forEach((edge) => {
    const edgeComponents = components.filter((component) =>
      component.nodeIds.has(edge.source)
        || component.nodeIds.has(edge.target)
        || component.terminalIds.has(edge.source)
        || component.terminalIds.has(edge.target),
    )

    edgeComponents.forEach((component) => {
      const visualSource = getVisualNodeId(edge.source, component)
      const visualTarget = getVisualNodeId(edge.target, component)
      if (visualSource && visualTarget && visualSource !== visualTarget) {
        layoutEdges.push({ ...edge, visualSource, visualTarget })
      }
    })
  })

  return { nodes: layouts, edges: layoutEdges }
}

function getNonTerminalComponents(nodes: TopologyNode[], edges: TopologyEdge[]) {
  const nodesById = new Map(nodes.map((node) => [node.id, node]))
  const nonTerminalIds = nodes
    .filter((node) => !terminalKinds.has(node.kind))
    .map((node) => node.id)
  const adjacency = new Map(nonTerminalIds.map((nodeId) => [nodeId, new Set<string>()]))

  edges.forEach((edge) => {
    const source = nodesById.get(edge.source)
    const target = nodesById.get(edge.target)
    if (!source || !target || terminalKinds.has(source.kind) || terminalKinds.has(target.kind)) {
      return
    }
    adjacency.get(source.id)?.add(target.id)
    adjacency.get(target.id)?.add(source.id)
  })

  const visited = new Set<string>()
  const components: TopologyComponent[] = []

  nonTerminalIds.forEach((nodeId) => {
    if (visited.has(nodeId)) {
      return
    }
    const componentNodes = new Set<string>()
    const pending = [nodeId]
    while (pending.length) {
      const current = pending.pop()
      if (!current || visited.has(current)) {
        continue
      }
      visited.add(current)
      componentNodes.add(current)
      adjacency.get(current)?.forEach((next) => {
        if (!visited.has(next)) {
          pending.push(next)
        }
      })
    }

    components.push({
      id: components.length,
      nodeIds: componentNodes,
      terminalIds: getAttachedTerminalIds(componentNodes, edges, nodesById),
    })
  })

  nodes
    .filter((node) => terminalKinds.has(node.kind))
    .filter((node) => !components.some((component) => component.terminalIds.has(node.id)))
    .forEach((node) => {
      components.push({
        id: components.length,
        nodeIds: new Set(),
        terminalIds: new Set([node.id]),
      })
    })

  return components.sort(componentComparator(nodesById)).map((component, index) => ({
    ...component,
    id: index,
  }))
}

function getAttachedTerminalIds(
  componentNodes: Set<string>,
  edges: TopologyEdge[],
  nodesById: Map<string, TopologyNode>,
) {
  const terminalIds = new Set<string>()

  edges.forEach((edge) => {
    const source = nodesById.get(edge.source)
    const target = nodesById.get(edge.target)
    if (!source || !target) {
      return
    }
    if (componentNodes.has(source.id) && terminalKinds.has(target.kind)) {
      terminalIds.add(target.id)
    }
    if (componentNodes.has(target.id) && terminalKinds.has(source.kind)) {
      terminalIds.add(source.id)
    }
  })

  return terminalIds
}

function getVisualNodeId(nodeId: string, component: TopologyComponent) {
  if (component.nodeIds.has(nodeId)) {
    return nodeId
  }
  if (component.terminalIds.has(nodeId)) {
    return terminalVisualId(nodeId, component.id)
  }

  return null
}

function terminalVisualId(nodeId: string, componentId: number) {
  return `${nodeId}::component-${componentId}`
}

function componentComparator(nodesById: Map<string, TopologyNode>) {
  return (first: TopologyComponent, second: TopologyComponent) => {
    const firstNode = representativeNode(first, nodesById)
    const secondNode = representativeNode(second, nodesById)
    return nodeSortKey(firstNode).localeCompare(nodeSortKey(secondNode))
  }
}

function representativeNode(component: TopologyComponent, nodesById: Map<string, TopologyNode>) {
  return [...component.nodeIds, ...component.terminalIds]
    .map((nodeId) => nodesById.get(nodeId))
    .filter((node): node is TopologyNode => Boolean(node))
    .sort(topologyNodeComparator)[0]
}

function layoutNodeComparator(first: LayoutNode, second: LayoutNode) {
  if (first.componentId !== second.componentId) {
    return first.componentId - second.componentId
  }

  return topologyNodeComparator(first.data, second.data)
}

function topologyNodeComparator(first: TopologyNode, second: TopologyNode) {
  return nodeSortKey(first).localeCompare(nodeSortKey(second))
}

function nodeSortKey(node?: TopologyNode) {
  if (!node) {
    return '999:'
  }

  return `${String(kindOrder[node.kind] ?? 99).padStart(3, '0')}:${node.name}`
}

function getOrCreateColumnRows(rowsByComponent: Map<number, Map<number, number>>, componentId: number) {
  const existing = rowsByComponent.get(componentId)
  if (existing) {
    return existing
  }

  const nextRows = new Map<number, number>()
  rowsByComponent.set(componentId, nextRows)
  return nextRows
}
</script>

<template>
  <section class="page topology-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ t('topology.eyebrow') }}</p>
        <h1>{{ t('topology.title') }}</h1>
        <p class="subtitle">{{ t('topology.subtitle') }}</p>
      </div>

      <label class="namespace-filter">
        <span>Namespace</span>
        <select
          :value="topologyNamespace"
          :disabled="namespacesQuery.isPending.value"
          @change="selectionStore.selectTopologyNamespace(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="item in namespacesQuery.data.value ?? []" :key="item.name" :value="item.name">
            {{ item.name }}
          </option>
        </select>
      </label>
    </div>

    <article v-if="topologyQuery.isError.value" class="panel state-panel status-critical">
      <h2>{{ t('topology.apiUnavailable') }}</h2>
      <p>{{ t('topology.apiUnavailableDescription') }}</p>
    </article>

    <article v-else-if="topologyQuery.isPending.value" class="panel state-panel">
      <h2>{{ t('topology.loading') }}</h2>
      <p>{{ t('topology.loadingDescription') }}</p>
    </article>

    <template v-else>
      <div class="topology-summary">
        <article class="summary-card">
          <p>{{ t('topology.nodes') }}</p>
          <strong>{{ topologyQuery.data.value?.nodes.length ?? 0 }}</strong>
          <small>{{ t('topology.collectedResources') }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('topology.edges') }}</p>
          <strong>{{ topologyQuery.data.value?.edges.length ?? 0 }}</strong>
          <small>{{ t('topology.detectedRelations') }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('topology.unhealthyNodes') }}</p>
          <strong :class="unhealthyCount ? 'tone-warning' : 'tone-healthy'">{{ unhealthyCount }}</strong>
          <small>{{ t('topology.highlightedNodes') }}</small>
        </article>
        <article class="summary-card">
          <p>{{ t('topology.namespace') }}</p>
          <strong>{{ topologyNamespace }}</strong>
          <small>{{ t('topology.currentScope') }}</small>
        </article>
      </div>

      <article class="panel topology-filter-panel">
        <div>
          <p class="eyebrow">{{ t('topology.filters') }}</p>
          <h2>{{ t('topology.graphControls') }}</h2>
        </div>
        <label class="namespace-filter">
          <span>{{ t('topology.resourceType') }}</span>
          <select
            :value="topologyResourceKind"
            @change="selectionStore.selectTopologyResourceKind(($event.target as HTMLSelectElement).value)"
          >
            <option value="">{{ t('topology.allResourceTypes') }}</option>
            <option v-for="kind in resourceKinds" :key="kind" :value="kind">{{ kind }}</option>
          </select>
        </label>
        <label class="topology-checkbox">
          <input
            type="checkbox"
            :checked="topologyUnhealthyOnly"
            @change="selectionStore.setTopologyUnhealthyOnly(($event.target as HTMLInputElement).checked)"
          />
          {{ t('topology.unhealthyOnly') }}
        </label>
        <label class="topology-checkbox">
          <input
            type="checkbox"
            :checked="topologyIncludeKafka"
            @change="selectionStore.setTopologyIncludeKafka(($event.target as HTMLInputElement).checked)"
          />
          {{ t('topology.includeKafka') }}
        </label>
      </article>

      <div class="topology-layout">
        <article class="panel topology-canvas-panel">
          <div class="topology-legend" :aria-label="t('topology.legend')">
            <div class="topology-status-legend">
              <span class="tone-healthy">● {{ t('status.healthy') }}</span>
              <span class="tone-warning">● {{ t('status.warning') }}</span>
              <span class="tone-critical">● {{ t('status.critical') }}</span>
              <span class="tone-unknown">● {{ t('status.unknown') }}</span>
            </div>
            <div class="topology-edge-legend">
              <span><i class="edge-swatch edge-swatch-owns"></i> owns</span>
              <span><i class="edge-swatch edge-swatch-routes"></i> routes / selects</span>
              <span><i class="edge-swatch edge-swatch-reference"></i> mounts / scheduled</span>
              <span><i class="edge-swatch edge-swatch-kafka"></i> Kafka dependency</span>
            </div>
          </div>
          <p v-if="!flowNodes.length" class="empty-state">{{ t('topology.noNodes') }}</p>
          <VueFlow
            v-else
            class="topology-flow"
            :nodes="flowNodes"
            :edges="flowEdges"
            :min-zoom="0.25"
            :max-zoom="1.5"
            @node-click="selectNode"
          >
            <template #node-default="{ data }">
              <div class="topology-node-content">
                <small>{{ data.kind }}</small>
                <strong>{{ data.name }}</strong>
                <span :class="statusClass(data.status)">{{ statusLabel(data.status) }}</span>
              </div>
            </template>
          </VueFlow>
        </article>

        <article class="panel topology-detail-panel">
          <p class="eyebrow">{{ t('topology.selection') }}</p>
          <h2>{{ selectedNode?.name ?? t('topology.selectNode') }}</h2>
          <p v-if="!selectedNode" class="empty-state">{{ t('topology.selectNodeDescription') }}</p>
          <dl v-else class="detail-list">
            <div>
              <dt>{{ t('detail.kind') }}</dt>
              <dd>{{ selectedNode.kind }}</dd>
            </div>
            <div>
              <dt>Namespace</dt>
              <dd>{{ selectedNode.namespace ?? '-' }}</dd>
            </div>
            <div>
              <dt>{{ t('table.status') }}</dt>
              <dd><span :class="statusClass(selectedNode.status)">{{ statusLabel(selectedNode.status) }}</span></dd>
            </div>
            <div>
              <dt>{{ t('table.reason') }}</dt>
              <dd>{{ reasonLabel(selectedNode.reason) }}</dd>
            </div>
          </dl>
          <p v-if="selectedNode" class="detail-message">{{ selectedNode.message }}</p>
        </article>
      </div>
    </template>
  </section>
</template>
