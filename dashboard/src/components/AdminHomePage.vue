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
import Navigation from '@/components/utils/Navigation.vue'
import { computed } from 'vue'
import { useAccessState } from '@/stores/UseAccessState.js'

const accessState = useAccessState()
const isRoot = computed(() => {
  return accessState.isRoot;
});
const isSupervisor = computed(() => {
  return accessState.isSupervisor;
});


const items = computed(() => {
  const res = [];
  res.push({
    name: 'Projects',
    iconClass: 'fa-tasks skills-color-projects',
    page: 'AdminHomePage',
  });
  res.push({
    name: 'Quizzes and Surveys',
    iconClass: 'fa-spell-check skills-color-subjects',
    page: 'QuizzesAndSurveys',
  });
  res.push({
    name: 'Admin Groups',
    iconClass: 'fa-solid fa-users skills-color-access',
    page: 'AdminGroups',
  });
  res.push({
    name: 'Global Badges',
    iconClass: 'fa-globe-americas skills-color-badges',
    page: 'GlobalBadges',
  });

  if (isSupervisor.value || isRoot.value) {
    res.push({
      name: 'Metrics',
      iconClass: 'fa-chart-bar skills-color-metrics',
      page: 'MultipleProjectsMetricsPage',
    });
  }

  if (isRoot.value) {
    res.push({
      name: 'Contact Admins',
      iconClass: 'fas fa-mail-bulk',
      page: 'ContactAdmins',
    });

    res.push({
      name: 'Activity History',
      iconClass: 'fas fa-users-cog text-success',
      page: 'UserActions',
    });
  }

  return res;
});
</script>

<template>
  <div>
    <navigation :nav-items="items"
        data-cy="navigationmenu"
        role="navigation">
    </navigation>

  </div>
</template>

<style scoped></style>
