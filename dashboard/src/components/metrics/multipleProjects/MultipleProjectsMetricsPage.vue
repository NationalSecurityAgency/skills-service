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
    <sub-page-header title="Metrics" :title-level="1"/>

    <skills-spinner :is-loading="loading" />
    <div v-if="!loading">
      <training-profile-comparator class="mb-4" :available-projects="projects"/>
      <multiple-proj-users-in-common :available-projects="projects"/>
    </div>
  </div>
</template>

<style scoped>

</style>