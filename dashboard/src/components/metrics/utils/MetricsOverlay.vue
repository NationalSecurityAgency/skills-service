/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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

</script>

<template>
  <SkillsOverlay :show="props.loading || !props.hasData" opacity="50">
    <slot></slot>
    <template #overlay>
      <div v-if="loading">
        <SkillsSpinner :is-loading="loading"></SkillsSpinner>
      </div>
      <div v-if="!loading && !loadedOnce && !hasData" class="alert alert-info">
        <Tag class="p-2 text-base font-light":icon="chartNotGeneratedIcon" :value="chartNotGeneratedMsg" severity="info"></Tag>
      </div>
      <div v-if="!loading && loadedOnce && !hasData" class="alert alert-info">
        <slot name="no-data">
          <Tag class="mr-1 p-2 text-base font-light" :icon="noDataIcon" :value="noDataMsg" severity="info"></Tag>
        </slot>
      </div>
    </template>
  </SkillsOverlay>
</template>

<style scoped>
</style>