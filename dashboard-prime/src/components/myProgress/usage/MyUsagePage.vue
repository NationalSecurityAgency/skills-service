<script setup>
import { onMounted, ref } from 'vue';

import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import EventHistoryChart from '@/components/myProgress/usage/EventHistoryChart.vue';
import MyProgressService from '@/components/myProgress/MyProgressService.js';

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
  <SubPageHeader title="My Usage" aria-label="My Usage" class="pt-4" />
  <SkillsSpinner :is-loading="loading"/>
  <div class="my-4 w-min-17rem">
    <EventHistoryChart v-if="!loading" :availableProjects="projects" />
  </div>
</div>
</template>

<style scoped>

</style>