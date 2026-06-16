import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useClusterSelectionStore = defineStore('cluster-selection', () => {
  const clusterId = ref('local')
  const namespace = ref('sample-app')
  const dashboardNamespace = ref('')
  const topologyNamespace = ref('sample-app')
  const topologyResourceKind = ref('')
  const topologyUnhealthyOnly = ref(false)
  const topologyIncludeKafka = ref(true)

  function selectNamespace(selectedNamespace: string) {
    namespace.value = selectedNamespace
  }

  function selectDashboardNamespace(selectedNamespace: string) {
    dashboardNamespace.value = selectedNamespace
  }

  function selectTopologyNamespace(selectedNamespace: string) {
    topologyNamespace.value = selectedNamespace
  }

  function selectTopologyResourceKind(selectedKind: string) {
    topologyResourceKind.value = selectedKind
  }

  function setTopologyUnhealthyOnly(enabled: boolean) {
    topologyUnhealthyOnly.value = enabled
  }

  function setTopologyIncludeKafka(enabled: boolean) {
    topologyIncludeKafka.value = enabled
  }

  return {
    clusterId,
    namespace,
    dashboardNamespace,
    topologyNamespace,
    topologyResourceKind,
    topologyUnhealthyOnly,
    topologyIncludeKafka,
    selectNamespace,
    selectDashboardNamespace,
    selectTopologyNamespace,
    selectTopologyResourceKind,
    setTopologyUnhealthyOnly,
    setTopologyIncludeKafka,
  }
})
