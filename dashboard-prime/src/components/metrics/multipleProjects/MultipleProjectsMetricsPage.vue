<script setup>
import { ref, onMounted } from 'vue';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import SupervisorService from "@/components/utils/SupervisorService.js";
import TrainingProfileComparator from "@/components/metrics/multipleProjects/TrainingProfileComparator.vue";
import MultipleProjUsersInCommon from "@/components/metrics/multipleProjects/MultipleProjUsersInCommon.vue";

const loading = ref(true);
const projects = ref([]);

onMounted(() => {
  loadProjects();
});

const loadProjects = () => {
  SupervisorService.getAllProjects()
      .then((res) => {
        projects.value = res;
      }).finally(() => {
    loading.value = false;
  });
};
</script>

<template>
  <div>
    <sub-page-header title="Metrics"/>

    <skills-spinner :is-loading="loading" />
    <div v-if="!loading">
      <training-profile-comparator class="mb-3" :available-projects="projects"/>
      <multiple-proj-users-in-common :available-projects="projects"/>
    </div>
  </div>
</template>

<style scoped>

</style>