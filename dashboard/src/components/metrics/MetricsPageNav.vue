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
  <div class="mb-4">
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
