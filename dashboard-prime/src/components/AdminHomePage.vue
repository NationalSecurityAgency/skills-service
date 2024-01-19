<script setup>
import Navigation from "@/components/utils/Navigation.vue";
import {ref} from "vue";

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

// if (this.isSupervisor || this.isRoot) {
//   items.push({
//     name: 'Global Badges',
//     iconClass: 'fa-globe-americas skills-color-badges',
//     page: 'GlobalBadges',
//   });
//   items.push({
//     name: 'Metrics',
//     iconClass: 'fa-chart-bar skills-color-metrics',
//     page: 'MultipleProjectsMetricsPage',
//   });
// }
//
// if (this.isRoot) {
//   items.push({
//     name: 'Contact Admins',
//     iconClass: 'fas fa-mail-bulk',
//     page: 'ContactAdmins',
//   });
//
//   items.push({
//     name: 'Activity History',
//     iconClass: 'fas fa-users-cog text-success',
//     page: 'UserActions',
//   });
// }


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
