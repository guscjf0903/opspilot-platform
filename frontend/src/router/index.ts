import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import AppShell from '../components/layout/AppShell.vue'
import ActionsView from '../views/actions/ActionsView.vue'
import CostView from '../views/cost/CostView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import KafkaView from '../views/kafka/KafkaView.vue'
import FeaturePlaceholderView from '../views/placeholders/FeaturePlaceholderView.vue'
import TopologyView from '../views/topology/TopologyView.vue'
import TrendsView from '../views/trends/TrendsView.vue'
import WorkloadDetailView from '../views/workloads/WorkloadDetailView.vue'
import WorkloadsView from '../views/workloads/WorkloadsView.vue'

export const router = createRouter({
  history: import.meta.env.VITE_ROUTER_HISTORY === 'hash'
    ? createWebHashHistory(import.meta.env.BASE_URL)
    : createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: AppShell,
      children: [
        {
          path: '',
          name: 'dashboard',
          component: DashboardView,
        },
        {
          path: 'workloads',
          name: 'workloads',
          component: WorkloadsView,
        },
        {
          path: 'workloads/:namespace/:kind/:name',
          name: 'workload-detail',
          component: WorkloadDetailView,
        },
        {
          path: 'topology',
          name: 'topology',
          component: TopologyView,
        },
        {
          path: 'cost',
          name: 'cost',
          component: CostView,
        },
        {
          path: 'trends',
          name: 'trends',
          component: TrendsView,
        },
        {
          path: 'kafka',
          name: 'kafka',
          component: KafkaView,
        },
        {
          path: 'actions',
          name: 'actions',
          component: ActionsView,
        },
        {
          path: ':feature(incidents|settings)',
          name: 'feature-placeholder',
          component: FeaturePlaceholderView,
        },
      ],
    },
  ],
})
