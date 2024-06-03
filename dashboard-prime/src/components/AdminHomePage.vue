<script setup>
import Navigation from '@/components/utils/Navigation.vue';
import { computed, ref } from 'vue';
import { useAccessState } from '@/stores/UseAccessState.js';

const accessState = useAccessState()
const isRoot = computed(() => {
  return accessState.isRoot;
});
const isSupervisor = computed(() => {
  return accessState.isSupervisor;
});

const itemsTmp = [];
itemsTmp.push({
  name: 'Projects',
  iconClass: 'fa-tasks skills-color-projects',
  page: 'AdminHomePage',
});
itemsTmp.push({
  name: 'Quizzes and Surveys',
  iconClass: 'fa-spell-check skills-color-subjects',
  page: 'QuizzesAndSurveys',
});

if (isSupervisor.value || isRoot.value) {
//   items.push({
//     name: 'Global Badges',
//     iconClass: 'fa-globe-americas skills-color-badges',
//     page: 'GlobalBadges',
//   });
  itemsTmp.push({
    name: 'Metrics',
    iconClass: 'fa-chart-bar skills-color-metrics',
    page: 'MultipleProjectsMetricsPage',
  });
}

if (isRoot.value) {
//   items.push({
//     name: 'Contact Admins',
//     iconClass: 'fas fa-mail-bulk',
//     page: 'ContactAdmins',
//   });
//
  itemsTmp.push({
    name: 'Activity History',
    iconClass: 'fas fa-users-cog text-success',
    page: 'UserActions',
  });
}


const items = ref(itemsTmp);
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
