<script setup>
import { onMounted, ref } from 'vue';

import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import EventHistoryChart from '@/components/myProgress/usage/EventHistoryChart.vue';
import MyProgressService from '@/components/myProgress/MyProgressService.js';
import MyProgressTitle from '@/components/myProgress/MyProgressTitle.vue'

const loading = ref(true);
const projects = ref([]);

onMounted(() => {
  loadProjects();
});
const loadProjects = () => {
  MyProgressService.loadMyProgressSummary()
      .then((myProgressSummary) => {
        projects.value = myProgressSummary.projectSummaries;
      }).finally(() => {
    loading.value = false;
  });
}
</script>

<template>
<div>
  <my-progress-title title="My Usage" />
  <SkillsSpinner :is-loading="loading"/>
  <div class="my-4 w-min-17rem">
    <EventHistoryChart v-if="!loading" :availableProjects="projects" />
  </div>
</div>
</template>

<style scoped>

</style>