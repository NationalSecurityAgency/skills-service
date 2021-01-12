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
  <div class="container-fluid mt-2">
    <b-row class="my-4">
      <b-col cols="12" lg="6" xl="3" class="d-flex mb-2">
        <info-snapshot-card class="flex-grow-1" />
      </b-col>
      <b-col cols="12" lg="6" xl="3" class="d-flex mb mb-2">
        <num-skills class="flex-grow-1" />
      </b-col>
      <b-col cols="12" lg="6" xl="3" class="d-flex mb-2">
        <last-earned-card class="flex-grow-1" />
      </b-col>
      <b-col cols="12" lg="6" xl="3" class="d-flex mb mb-2">
        <badges-num-card class="flex-grow-1" />
      </b-col>
    </b-row>

    <b-row class="my-4">
      <b-col class="charts-content">
        <event-history-chart :projects="projects"></event-history-chart>
      </b-col>
    </b-row>
    <b-row class="my-4">
      <b-col v-for="proj in projects" :key="proj.name"
             cols="12" lg="6" xl="4"
            class="mb-2">
        <router-link :to="{ name:'MyProjectSkills', params: { projectId: proj.projectId } }" tag="div" class="project-link">
          <project-link-card :proj="proj"/>
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
        projects: [{
          name: 'DolphinCommute',
          projectId: 'DolphinCommute',
          level: 1,
          totalPts: 34000,
          currentPts: 15000,
          totalUsers: 28399,
          rank: 38,
        }, {
          name: 'DonkeySquirrel',
          projectId: 'DonkeySquirrel',
          level: 0,
          totalPts: 12560,
          currentPts: 15,
          totalUsers: 10,
          rank: 3,
        }, {
          name: 'MonkeyPlop',
          projectId: 'MonkeyPlop',
          level: 3,
          totalPts: 19000,
          currentPts: 16022,
          totalUsers: 59,
          rank: 38,
        }, {
          name: 'Boatfall',
          projectId: 'Boatfall',
          level: 2,
          totalPts: 8525,
          currentPts: 856,
          totalUsers: 379,
          rank: 78,
        }, {
          name: 'SkillTree Dashboard',
          projectId: 'Inception',
          level: 2,
          totalPts: 8525,
          currentPts: 856,
          totalUsers: 379,
          rank: 78,
        }],
      };
    },
    mounted() {
      this.loading = false;
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
</style>
