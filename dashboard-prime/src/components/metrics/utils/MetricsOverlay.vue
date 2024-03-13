<script setup>
import { computed } from 'vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue';

const props = defineProps({
  hasData: {
    type: Boolean,
    required: true,
  },
  loading: {
    type: Boolean,
    required: true,
  },
  noDataMsg: {
    type: String,
    default: 'No Data. Yet...',
    required: false,
  },
  noDataIcon: {
    type: String,
    default: 'fas fa-dragon',
    required: false,
  },
  loadedOnce: {
    type: Boolean,
    default: true,
    required: false,
  },
  chartNotGeneratedMsg: {
    type: String,
    default: 'Generate the chart using controls above!',
    required: false,
  },
  chartNotGeneratedIcon: {
    type: String,
    default: 'fas fa-chart-line',
    required: false,
  },
})

const doShow = computed(() => {
  return props.loading || !props.hasData;
})
</script>

<template>
  <SkillsOverlay :show="doShow" opacity="50">
    <slot></slot>
    <template #overlay>
      <div v-if="loading">
        <SkillsSpinner :is-loading="loading"></SkillsSpinner>
      </div>
      <div v-if="!loading && !loadedOnce && !hasData" class="alert alert-info">
        <Tag class="p-2 text-base font-light":icon="chartNotGeneratedIcon" :value="chartNotGeneratedMsg" severity="info"></Tag>
      </div>
      <div v-if="!loading && loadedOnce && !hasData" class="alert alert-info">
        <Tag class="mr-1 p-2 text-base font-light" :icon="noDataIcon" :value="noDataMsg" severity="info"></Tag>
      </div>
    </template>
  </SkillsOverlay>
</template>

<style scoped>
</style>