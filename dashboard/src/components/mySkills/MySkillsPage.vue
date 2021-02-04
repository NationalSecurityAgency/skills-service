/*
Copyright 2020 SkillTree

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
<template>
  <div v-if="!loading" class="container-fluid mt-2">
    <b-row class="my-4">
      <b-col cols="12" lg="6" xl="3" class="d-flex mb-2">
        <info-snapshot-card :total-projects="mySkillsSummary.totalProjects" :num-projects-contributed="mySkillsSummary.numProjectsContributed" class="flex-grow-1 my-skills-card" />
      </b-col>
      <b-col cols="12" lg="6" xl="3" class="d-flex mb-2">
        <num-skills :total-skills="mySkillsSummary.totalSkills" :num-achieved-skills="mySkillsSummary.numAchievedSkills" class="flex-grow-1 my-skills-card" />
      </b-col>
      <b-col cols="12" lg="6" xl="3" class="d-flex mb-2">
        <last-earned-card :num-achieved-skills-last-month="mySkillsSummary.numAchievedSkillsLastMonth" :num-achieved-skills-last-week="mySkillsSummary.numAchievedSkillsLastWeek" :most-recent-achieved-skill="mySkillsSummary.mostRecentAchievedSkill" class="flex-grow-1 my-skills-card" />
      </b-col>
      <b-col cols="12" lg="6" xl="3" class="d-flex mb-2">
        <badges-num-card :total-badges="mySkillsSummary.totalBadges" :num-achieved-badges="mySkillsSummary.numAchievedBadges" :num-achieved-gem-badges="mySkillsSummary.numAchievedGemBadges" :num-achieved-global-badges="mySkillsSummary.numAchievedGlobalBadges" class="flex-grow-1 my-skills-card" />
      </b-col>
    </b-row>
    <b-row class="my-4">
      <b-col class="charts-content">
        <event-history-chart v-if="!loading" :availableProjects="projects"></event-history-chart>
      </b-col>
    </b-row>
    <b-row class="my-4">
      <b-col v-for="proj in projects" :key="proj.projectName"
             cols="12" lg="6" xl="4"
            class="mb-2">
        <router-link :to="{ name:'MyProjectSkills', params: { projectId: proj.projectId } }" tag="div" class="project-link" :data-cy="`project-link-${proj.projectId}`">
          <project-link-card :proj="proj" class="my-skills-card"/>
        </router-link>
      </b-col>
    </b-row>

  </div>
</template>

<script>
  import ProjectLinkCard from './ProjectLinkCard';
  import InfoSnapshotCard from './InfoSnapshotCard';
  import NumSkills from './NumSkills';
  import BadgesNumCard from './BadgesNumCard';
  import LastEarnedCard from './LastEarnedCard';
  import EventHistoryChart from './EventHistoryChart';
  import MySkillsService from './MySkillsService';

  export default {
    name: 'MySkillsPage',
    components: {
      LastEarnedCard,
      BadgesNumCard,
      NumSkills,
      InfoSnapshotCard,
      ProjectLinkCard,
      EventHistoryChart,
    },
    data() {
      return {
        loading: true,
        mySkillsSummary: null,
        projects: [],
      };
    },
    mounted() {
      this.loadProjects();
    },
    methods: {
      loadProjects() {
        MySkillsService.loadMySkillsSummary()
          .then((res) => {
            this.mySkillsSummary = res;
            this.projects = this.mySkillsSummary.projectSummaries;
          }).finally(() => {
            this.loading = false;
          });
      },
    },
    watch: {
      series() {
        this.seriesInternal = [{
          name: this.title,
          data: this.series,
        }];
      },
    },
  };
</script>

<style scoped>
.project-link :hover {
  cursor: pointer;
}
.charts-content {
  /* this little hack is required to prevent apexcharts from wrapping onto a new line;
  the gist is that they calculate width dynamically and do not work properly with the width of 0*/
  min-width: 1rem !important;
}
.my-skills-card {
  min-width: 17rem !important;
}
</style>
