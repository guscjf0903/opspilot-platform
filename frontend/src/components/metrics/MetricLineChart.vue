<script setup lang="ts">
import { computed, ref } from 'vue'
import type { MetricPoint } from '../../types/metrics'

const props = defineProps<{
  title: string
  unitLabel: string
  points: MetricPoint[]
  formatValue: (value: number) => string
  emptyLabel: string
}>()

const width = 720
const height = 240
const padding = {
  top: 18,
  right: 22,
  bottom: 34,
  left: 54,
}
const hoveredPointIndex = ref<number>()

const parsedPoints = computed(() =>
  props.points
    .map((point) => ({
      timestamp: new Date(point.timestamp).getTime(),
      value: point.value,
    }))
    .filter((point) => Number.isFinite(point.timestamp) && Number.isFinite(point.value)),
)

const latest = computed(() => parsedPoints.value.at(-1)?.value)
const hasEnoughPoints = computed(() => parsedPoints.value.length > 1)
const minTimestamp = computed(() => Math.min(...parsedPoints.value.map((point) => point.timestamp)))
const maxTimestamp = computed(() => Math.max(...parsedPoints.value.map((point) => point.timestamp)))
const maxValue = computed(() => Math.max(...parsedPoints.value.map((point) => point.value), 0))
const yMax = computed(() => (maxValue.value > 0 ? maxValue.value * 1.15 : 1))

const scaledPoints = computed(() => {
  const plotWidth = width - padding.left - padding.right
  const plotHeight = height - padding.top - padding.bottom
  const timestampRange = Math.max(maxTimestamp.value - minTimestamp.value, 1)

  return parsedPoints.value.map((point) => ({
    timestamp: point.timestamp,
    value: point.value,
    x: padding.left + ((point.timestamp - minTimestamp.value) / timestampRange) * plotWidth,
    y: padding.top + plotHeight - (point.value / yMax.value) * plotHeight,
  }))
})

const linePath = computed(() =>
  scaledPoints.value
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`)
    .join(' '),
)

const areaPath = computed(() => {
  if (!scaledPoints.value.length) {
    return ''
  }

  const baseline = height - padding.bottom
  const first = scaledPoints.value[0]
  const last = scaledPoints.value[scaledPoints.value.length - 1]

  return `${linePath.value} L ${last.x.toFixed(1)} ${baseline} L ${first.x.toFixed(1)} ${baseline} Z`
})

const startLabel = computed(() => formatTime(minTimestamp.value))
const endLabel = computed(() => formatTime(maxTimestamp.value))
const maxValueLabel = computed(() => props.formatValue(yMax.value))
const hoveredPoint = computed(() => {
  if (hoveredPointIndex.value === undefined) {
    return undefined
  }

  return scaledPoints.value[hoveredPointIndex.value]
})
const tooltipStyle = computed(() => {
  if (!hoveredPoint.value) {
    return {}
  }

  return {
    left: `${(hoveredPoint.value.x / width) * 100}%`,
    top: `${(hoveredPoint.value.y / height) * 100}%`,
  }
})

function formatTime(timestamp: number) {
  if (!Number.isFinite(timestamp)) {
    return '-'
  }

  return new Intl.DateTimeFormat(undefined, {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(timestamp))
}

function formatTooltipTime(timestamp: number) {
  if (!Number.isFinite(timestamp)) {
    return '-'
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(timestamp))
}

function handlePointerMove(event: PointerEvent) {
  const svg = event.currentTarget as SVGSVGElement
  const rect = svg.getBoundingClientRect()
  const pointerX = ((event.clientX - rect.left) / rect.width) * width
  let nearestIndex = 0
  let nearestDistance = Number.POSITIVE_INFINITY

  scaledPoints.value.forEach((point, index) => {
    const distance = Math.abs(point.x - pointerX)

    if (distance < nearestDistance) {
      nearestDistance = distance
      nearestIndex = index
    }
  })

  hoveredPointIndex.value = nearestIndex
}

function clearHoveredPoint() {
  hoveredPointIndex.value = undefined
}
</script>

<template>
  <div class="metric-chart">
    <div class="metric-chart-header">
      <div>
        <strong>{{ title }}</strong>
        <small>{{ unitLabel }}</small>
      </div>
      <b>{{ latest === undefined ? '-' : formatValue(latest) }}</b>
    </div>

    <p v-if="!hasEnoughPoints" class="empty-state">{{ emptyLabel }}</p>
    <svg
      v-else
      class="metric-chart-svg"
      :viewBox="`0 0 ${width} ${height}`"
      role="img"
      :aria-label="title"
      @pointermove="handlePointerMove"
      @pointerleave="clearHoveredPoint"
    >
      <line
        class="metric-axis"
        :x1="padding.left"
        :y1="height - padding.bottom"
        :x2="width - padding.right"
        :y2="height - padding.bottom"
      />
      <line
        class="metric-axis"
        :x1="padding.left"
        :y1="padding.top"
        :x2="padding.left"
        :y2="height - padding.bottom"
      />
      <text class="metric-axis-label" :x="padding.left" :y="height - 9">{{ startLabel }}</text>
      <text class="metric-axis-label metric-axis-label-end" :x="width - padding.right" :y="height - 9">
        {{ endLabel }}
      </text>
      <text class="metric-axis-label" :x="8" :y="padding.top + 5">{{ maxValueLabel }}</text>
      <path class="metric-area" :d="areaPath" />
      <path class="metric-line" :d="linePath" />
      <line
        v-if="hoveredPoint"
        class="metric-hover-line"
        :x1="hoveredPoint.x"
        :y1="padding.top"
        :x2="hoveredPoint.x"
        :y2="height - padding.bottom"
      />
      <circle v-if="hoveredPoint" class="metric-hover-dot" :cx="hoveredPoint.x" :cy="hoveredPoint.y" r="5" />
    </svg>
    <div v-if="hoveredPoint" class="metric-tooltip" :style="tooltipStyle">
      <strong>{{ formatValue(hoveredPoint.value) }}</strong>
      <small>{{ formatTooltipTime(hoveredPoint.timestamp) }}</small>
    </div>
  </div>
</template>
