<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import { storeToRefs } from 'pinia'
import {
  useDeploymentsQuery,
  useEventsQuery,
  useNamespacesQuery,
  useNodesQuery,
  usePodsQuery,
} from '../../composables/queries/useKubernetesInventoryQueries'
import { translateResourceReason, translateResourceStatus } from '../../i18n/messages'
import { useClusterSelectionStore } from '../../stores/clusterSelection'
import { useUiPreferencesStore } from '../../stores/uiPreferences'
import type { ResourceStatus } from '../../types/kubernetes'

const selectionStore = useClusterSelectionStore()
const { clusterId, namespace } = storeToRefs(selectionStore)
const uiPreferences = useUiPreferencesStore()
const { locale } = storeToRefs(uiPreferences)
const { t } = uiPreferences

const namespacesQuery = useNamespacesQuery(clusterId)
const deploymentsQuery = useDeploymentsQuery(clusterId, namespace)
const podsQuery = usePodsQuery(clusterId, namespace)
const eventsQuery = useEventsQuery(clusterId, namespace)
const nodesQuery = useNodesQuery(clusterId)

const warningEvents = computed(() =>
  (eventsQuery.data.value ?? []).filter((event) => event.status !== 'healthy').slice(0, 8),
)

const hasInventoryError = computed(
  () =>
    namespacesQuery.isError.value ||
    deploymentsQuery.isError.value ||
    podsQuery.isError.value ||
    nodesQuery.isError.value,
)

watchEffect(() => {
  const namespaces = namespacesQuery.data.value ?? []

  if (namespaces.length > 0 && !namespaces.some((item) => item.name === namespace.value)) {
    selectionStore.selectNamespace(namespaces[0].name)
  }
})

function statusClass(status: ResourceStatus) {
  return `resource-status status-${status}`
}

function statusLabel(status: ResourceStatus) {
  return translateResourceStatus(locale.value, status)
}

function reasonLabel(reason: string) {
  return translateResourceReason(locale.value, reason)
}

function workloadRoute(kind: string, name: string) {
  return `/workloads/${namespace.value}/${kind.toLowerCase()}/${name}`
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ t('workloads.eyebrow') }}</p>
        <h1>{{ t('workloads.title') }}</h1>
        <p class="subtitle">{{ t('workloads.subtitle') }}</p>
      </div>

      <label class="namespace-filter">
        <span>Namespace</span>
        <select
          :value="namespace"
          :disabled="namespacesQuery.isPending.value"
          @change="selectionStore.selectNamespace(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="item in namespacesQuery.data.value ?? []" :key="item.name" :value="item.name">
            {{ item.name }}
          </option>
        </select>
      </label>
    </div>

    <article v-if="hasInventoryError" class="panel state-panel status-critical">
      <h2>{{ t('workloads.apiUnavailable') }}</h2>
      <p>{{ t('workloads.apiUnavailableDescription') }}</p>
    </article>

    <article v-else-if="namespacesQuery.isPending.value" class="panel state-panel">
      <h2>{{ t('workloads.loadingInventory') }}</h2>
      <p>{{ t('workloads.loadingInventoryDescription') }}</p>
    </article>

    <template v-else>
      <div class="inventory-summary">
        <article class="summary-card">
          <p>Namespaces</p>
          <strong>{{ namespacesQuery.data.value?.length ?? 0 }}</strong>
          <small>{{ t('workloads.clusterScope') }}</small>
        </article>
        <article class="summary-card">
          <p>Deployments</p>
          <strong>{{ deploymentsQuery.data.value?.length ?? 0 }}</strong>
          <small>{{ namespace }}</small>
        </article>
        <article class="summary-card">
          <p>Pods</p>
          <strong>{{ podsQuery.data.value?.length ?? 0 }}</strong>
          <small>{{ namespace }}</small>
        </article>
        <article class="summary-card">
          <p>Nodes</p>
          <strong>{{ nodesQuery.data.value?.length ?? 0 }}</strong>
          <small>{{ t('workloads.clusterScope') }}</small>
        </article>
      </div>

      <article class="panel inventory-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">{{ t('workloads.controllers') }}</p>
            <h2>Deployments</h2>
          </div>
        </div>
        <p v-if="deploymentsQuery.isPending.value" class="empty-state">
          {{ t('workloads.loadingDeployments') }}
        </p>
        <p v-else-if="!deploymentsQuery.data.value?.length" class="empty-state">
          {{ t('workloads.noDeployments') }}
        </p>
        <table v-else>
          <thead>
            <tr>
              <th>{{ t('table.name') }}</th>
              <th>{{ t('table.status') }}</th>
              <th>{{ t('table.available') }}</th>
              <th>{{ t('table.reason') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="deployment in deploymentsQuery.data.value" :key="deployment.name">
              <td>
                <RouterLink class="table-resource-link" :to="workloadRoute(deployment.kind, deployment.name)">
                  {{ deployment.name }}
                </RouterLink>
              </td>
              <td><span :class="statusClass(deployment.status)">{{ statusLabel(deployment.status) }}</span></td>
              <td>{{ deployment.availableReplicas }}/{{ deployment.desiredReplicas }}</td>
              <td>{{ reasonLabel(deployment.reason) }}</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel inventory-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">{{ t('workloads.runtimeUnits') }}</p>
            <h2>Pods</h2>
          </div>
        </div>
        <p v-if="podsQuery.isPending.value" class="empty-state">{{ t('workloads.loadingPods') }}</p>
        <p v-else-if="!podsQuery.data.value?.length" class="empty-state">
          {{ t('workloads.noPods') }}
        </p>
        <table v-else>
          <thead>
            <tr>
              <th>{{ t('table.name') }}</th>
              <th>{{ t('table.status') }}</th>
              <th>{{ t('table.phase') }}</th>
              <th>{{ t('table.restarts') }}</th>
              <th>Node</th>
              <th>{{ t('table.reason') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="pod in podsQuery.data.value" :key="pod.name">
              <td>
                <RouterLink class="table-resource-link" :to="workloadRoute(pod.kind, pod.name)">
                  {{ pod.name }}
                </RouterLink>
              </td>
              <td><span :class="statusClass(pod.status)">{{ statusLabel(pod.status) }}</span></td>
              <td>{{ pod.phase ?? '-' }}</td>
              <td>{{ pod.restartCount }}</td>
              <td>{{ pod.nodeName ?? '-' }}</td>
              <td>{{ reasonLabel(pod.reason) }}</td>
            </tr>
          </tbody>
        </table>
      </article>

      <div class="panel-grid inventory-bottom-grid">
        <article class="panel inventory-panel">
          <p class="eyebrow">{{ t('workloads.clusterCapacity') }}</p>
          <h2>Nodes</h2>
          <p v-if="!nodesQuery.data.value?.length" class="empty-state">{{ t('workloads.noNodes') }}</p>
          <ul v-else class="resource-list">
            <li v-for="node in nodesQuery.data.value" :key="node.name">
              <span>{{ node.name }}</span>
              <span :class="statusClass(node.status)">{{ statusLabel(node.status) }}</span>
            </li>
          </ul>
        </article>

        <article class="panel inventory-panel">
          <p class="eyebrow">{{ t('workloads.recentSignals') }}</p>
          <h2>{{ t('workloads.warningEvents') }}</h2>
          <p v-if="!warningEvents.length" class="empty-state">{{ t('workloads.noWarningEvents') }}</p>
          <ul v-else class="resource-list event-list">
            <li v-for="event in warningEvents" :key="event.name">
              <div>
                <strong>{{ reasonLabel(event.reason) }}</strong>
                <small>{{ event.involvedKind }}/{{ event.involvedName }}</small>
              </div>
              <span :class="statusClass(event.status)">{{ statusLabel(event.status) }}</span>
            </li>
          </ul>
        </article>
      </div>
    </template>
  </section>
</template>
