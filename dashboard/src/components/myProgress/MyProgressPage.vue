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
  <loading-container :is-loading="loading" class="container-fluid ">
    <no-content2 v-if="!hasProjects" icon="fas fa-hand-spock" class="mt-4"
                 title="No Projects Yet..." :message="noProjectsMessage"/>

    <div v-if="!loading && hasProjects" class="mt-2">
      <b-row>
        <b-col>
          <div class="card p-0 m-0">
            <div class="card-body mt-2 mb-0 p-0">
              <h1 class="h4 text-uppercase text-center">Progress and Rankings</h1>
            </div>
          </div>
        </b-col>
      </b-row>
      <b-row class="my-4">
        <b-col cols="12" md="6" xl="3" class="d-flex mb-2 pl-md-3 pr-md-1">
          <info-snapshot-card :total-projects="projects.length" :num-projects-contributed="myProgressSummary.numProjectsContributed" class="flex-grow-1 my-summary-card" />
        </b-col>
        <b-col cols="12" md="6" xl="3" class="d-flex mb-2 pr-md-3 pl-md-1 pr-xl-1">
          <num-skills :total-skills="myProgressSummary.totalSkills" :num-achieved-skills="myProgressSummary.numAchievedSkills" class="flex-grow-1 my-summary-card" />
        </b-col>
        <b-col cols="12" md="6" xl="3" class="d-flex mb-2 pl-md-3 pr-md-1 pl-xl-1">
          <last-earned-card :num-achieved-skills-last-month="myProgressSummary.numAchievedSkillsLastMonth" :num-achieved-skills-last-week="myProgressSummary.numAchievedSkillsLastWeek" :most-recent-achieved-skill="myProgressSummary.mostRecentAchievedSkill" class="flex-grow-1 my-summary-card" />
        </b-col>
        <b-col cols="12" md="6" xl="3" class="d-flex mb-2 pr-md-3 pl-md-1">
          <badges-num-card :total-badges="myProgressSummary.totalBadges"
                           :num-achieved-badges="myProgressSummary.numAchievedBadges"
                           :num-achieved-gem-badges="myProgressSummary.numAchievedGemBadges"
                           :num-achieved-global-badges="myProgressSummary.numAchievedGlobalBadges"
                           class="flex-grow-1 my-summary-card"/>
        </b-col>
      </b-row>
      <b-row class="my-4">
        <b-col class="my-summary-card">
          <event-history-chart v-if="!loading" :availableProjects="projects"></event-history-chart>
        </b-col>
      </b-row>
      <b-row class="my-4">
        <b-col v-for="(proj, index) in projects" :key="proj.projectName"
               cols="12" md="6" xl="4"
              class="mb-2 px-0">
          <router-link :to="{ name:'MyProjectSkills', params: { projectId: proj.projectId } }" tag="div" class="project-link" :data-cy="`project-link-${proj.projectId}`">
            <project-link-card :proj="proj" class="my-summary-card px-3" :class="projectLinkClass(index)"/>
          </router-link>
        </b-col>
      </b-row>
    </div>
  </loading-container>
</template>

<script>
  import ProjectLinkCard from './ProjectLinkCard';
  import InfoSnapshotCard from './InfoSnapshotCard';
  import NumSkills from './NumSkills';
  import BadgesNumCard from './BadgesNumCard';
  import LastEarnedCard from './LastEarnedCard';
  import EventHistoryChart from './EventHistoryChart';
  import MyProgressService from './MyProgressService';
  import LoadingContainer from '../utils/LoadingContainer';
  import NoContent2 from '../utils/NoContent2';

  export default {
    name: 'MyProgressPage',
    components: {
      LastEarnedCard,
      BadgesNumCard,
      NumSkills,
      InfoSnapshotCard,
      ProjectLinkCard,
      EventHistoryChart,
      LoadingContainer,
      NoContent2,
    },
    data() {
      return {
        loading: true,
        myProgressSummary: null,
        projects: [],
      };
    },
    mounted() {
      this.loadProjects();
    },
    methods: {
      loadProjects() {
        MyProgressService.loadMyProgressSummary()
          .then((res) => {
            this.myProgressSummary = res;
            this.projects = this.myProgressSummary.projectSummaries;
          }).finally(() => {
            this.loading = false;
          });
      },
      projectLinkClass(index) {
        if (index === 0) {
          // first
          return 'pl-3 pr-sm-3 pr-md-1';
        }
        let classes = '';

        // xl - 3 per row
        if (index % 3 === 0) {
          // start of a new xl row
          classes += 'pl-xl-3 pr-xl-1 ';
        } else if ((index + 1) % 3 === 0) {
          // end of an xl row
          classes += 'pl-xl-1 pr-xl-3 ';
        } else {
          // middle of an xl row
          classes += 'px-xl-1 ';
        }

        // md - two per row
        if (index % 2 === 1) {
          // index is odd, end of a md row
          classes += 'pl-md-1 pr-md-3';
        } else {
          // index is even, start of a md row
          classes += 'pl-md-3 pr-md-1';
        }
        return classes;
      },
    },
    computed: {
      hasProjects() {
        return this.projects && this.projects.length > 0;
      },
      noProjectsMessage() {
        if (this.myProgressSummary && this.myProgressSummary.totalProjects > 0) {
          return 'You will see your SkillTree progress and rankings on this page when project(s) have their production mode enabled.';
        }
        return 'Projects can be created from the "Project Admin" view, accessible by clicking on your name at the top-right of the screen.';
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
.my-summary-card {
  min-width: 17rem !important;
}
</style>
