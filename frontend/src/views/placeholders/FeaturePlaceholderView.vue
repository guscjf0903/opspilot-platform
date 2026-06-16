<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUiPreferencesStore } from '../../stores/uiPreferences'

const route = useRoute()
const { t } = useUiPreferencesStore()

const featureName = computed(() => {
  const feature = String(route.params.feature ?? 'feature')

  return feature.charAt(0).toUpperCase() + feature.slice(1)
})
</script>

<template>
  <section class="page">
    <p class="eyebrow">{{ t('placeholder.eyebrow') }}</p>
    <h1>{{ featureName }}</h1>
    <article class="panel placeholder-panel">
      <h2>{{ t('placeholder.title') }}</h2>
      <p>{{ t('placeholder.description') }}</p>
    </article>
  </section>
</template>
