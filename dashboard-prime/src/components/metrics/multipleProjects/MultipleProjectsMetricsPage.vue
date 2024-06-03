<script setup>
import { ref, onMounted } from 'vue';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import SupervisorService from "@/components/utils/SupervisorService.js";

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
  </div>
</template>

<style scoped>

</style>