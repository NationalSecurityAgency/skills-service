<script setup>
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import ProjectMetricsSmallNav from "@/components/metrics/projectNav/ProjectMetricsSmallNav.vue";
import ProjectMetricsNavCards from "@/components/metrics/projectNav/ProjectMetricsNavCards.vue";

const navCards = [{
  title: 'Achievements',
  subtitle: 'Explore users\' achievements',
  description: 'Browse users\' achieved overall levels, subject level achievements as well as earned badges',
  icon: 'fa fa-trophy skills-color-achievements',
  pathName: 'UsersAndLevelsMetrics',
}, {
  title: 'Subjects',
  subtitle: 'Achievements by Subjects',
  description: 'Detailed breakdown how users are earning skills under each subject',
  icon: 'fa fa-cubes skills-color-subjects',
  pathName: 'SubjectMetricsPage',
}, {
  title: 'Skills',
  subtitle: 'Understand Skills Usage',
  description: 'Find top-achieved skills and overlooked skills. Learn how much each skill is utilized within your application.',
  icon: 'fa fa-graduation-cap skills-color-skills',
  pathName: 'SkillsMetricsPage',
}];
</script>

<template>
  <div class="mb-3">
    <sub-page-header title="Metrics">
      <project-metrics-small-nav :nav-cards="navCards" />
    </sub-page-header>

    <div style="width: 99%;">
      <router-view id="mainContent3" tabindex="-1" aria-label="Main content area, click tab to navigate" ></router-view>
      <project-metrics-nav-cards :nav-cards="navCards"/>
    </div>
  </div>
</template>

<style scoped></style>
