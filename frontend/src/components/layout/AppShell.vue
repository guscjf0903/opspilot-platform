<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiPreferencesStore } from '../../stores/uiPreferences'

const uiPreferences = useUiPreferencesStore()
const { locale, theme } = storeToRefs(uiPreferences)
const { setLocale, setTheme, t } = uiPreferences
const isDemoMode = import.meta.env.VITE_DEMO_MODE === 'true'
const environmentLabel = computed(() => isDemoMode ? '데모 스냅샷' : t('shell.localProfile'))
const clusterLabel = computed(() => isDemoMode ? 'demo-snapshot' : 'local-kind')

const navigationItems = computed(() => [
  { label: t('navigation.dashboard'), to: '/' },
  { label: t('navigation.topology'), to: '/topology' },
  { label: t('navigation.workloads'), to: '/workloads' },
  { label: t('navigation.kafka'), to: '/kafka' },
  { label: t('navigation.incidents'), to: '/incidents' },
  { label: t('navigation.cost'), to: '/cost' },
  { label: t('navigation.trends'), to: '/trends' },
  { label: t('navigation.actions'), to: '/actions' },
  { label: t('navigation.settings'), to: '/settings' },
])
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">OP</span>
        <div>
          <strong>OpsPilot</strong>
          <small>{{ t('brand.subtitle') }}</small>
        </div>
      </div>

      <nav :aria-label="t('navigation.label')">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <span class="environment-dot"></span>
        {{ environmentLabel }}
      </div>
    </aside>

    <main class="main-content">
      <header class="topbar">
        <div>
          <small>{{ t('shell.cluster') }}</small>
          <strong>{{ clusterLabel }}</strong>
        </div>
        <div class="topbar-actions">
          <span v-if="isDemoMode" class="phase-badge">읽기 전용 데모</span>
          <div class="preference-group" role="group" :aria-label="t('preferences.language')">
            <button
              type="button"
              class="preference-button"
              :class="{ active: locale === 'ko' }"
              :aria-pressed="locale === 'ko'"
              @click="setLocale('ko')"
            >
              {{ t('preferences.korean') }}
            </button>
            <button
              type="button"
              class="preference-button"
              :class="{ active: locale === 'en' }"
              :aria-pressed="locale === 'en'"
              @click="setLocale('en')"
            >
              {{ t('preferences.english') }}
            </button>
          </div>
          <div class="preference-group" role="group" :aria-label="t('preferences.theme')">
            <button
              type="button"
              class="preference-button"
              :class="{ active: theme === 'light' }"
              :aria-pressed="theme === 'light'"
              @click="setTheme('light')"
            >
              {{ t('preferences.light') }}
            </button>
            <button
              type="button"
              class="preference-button"
              :class="{ active: theme === 'dark' }"
              :aria-pressed="theme === 'dark'"
              @click="setTheme('dark')"
            >
              {{ t('preferences.dark') }}
            </button>
          </div>
          <span class="phase-badge">{{ t('shell.phase') }}</span>
        </div>
      </header>

      <RouterView />
    </main>
  </div>
</template>
