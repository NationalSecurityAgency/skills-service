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
  <div>
    <sub-page-header title="Metrics"/>
    <skills-spinner :is-loading="loading" />
    <div v-if="!loading">
      <div class="row mb-2">
        <div class="col">
          <stats-card title="Achieved" :statNum="numUsersAchieved" icon="fa fa-trophy text-info" data-cy="numUserAchievedStatCard">
            Number of users that achieved this skill
          </stats-card>
        </div>
        <div class="col">
          <stats-card title="In Progress" :statNum="numUsersInProgress" icon="fa fa-running text-primary" data-cy="inProgressStatCard">
            Number of Users with some points earned toward the skill
          </stats-card>
        </div>
        <div class="col">
          <stats-card title="Last Achieved" :statNum="lastAchieved" :calculate-time-from-now="true"
                      icon="fa fa-clock text-warning"  data-cy="lastAchievedStatCard">
            This skill was last achieved on <span class="text-success">{{ lastAchieved | date}}</span>.
          </stats-card>
        </div>

      </div>

      <b-card header="Users that Achieved this Skill">
        <skill-achieved-by-users-over-time/>
      </b-card>
      <b-card header="Applied Skill Events over time" class="mt-2">
        <applied-skill-events-over-time />
      </b-card>
    </div>
  </div>
</template>

<script>
  import SubPageHeader from '@//components/utils/pages/SubPageHeader';
  import SkillAchievedByUsersOverTime from './SkillAchievedByUsersOverTime';
  import AppliedSkillEventsOverTime from './AppliedSkillEventsOverTime';
  import StatsCard from '../utils/StatsCard';
  import MetricsService from '../MetricsService';
  import SkillsSpinner from '../../utils/SkillsSpinner';

  export default {
    name: 'SingleSkillMetricPage',
    components: {
      SkillsSpinner,
      StatsCard,
      AppliedSkillEventsOverTime,
      SkillAchievedByUsersOverTime,
      SubPageHeader,
    },
    data() {
      return {
        loading: true,
        numUsersAchieved: 0,
        numUsersInProgress: 0,
        lastAchieved: 0,
      };
    },
    mounted() {
      MetricsService.loadChart(this.$route.params.projectId, 'singleSkillCountsChartBuilder', { skillId: this.$route.params.skillId })
        .then((dataFromServer) => {
          this.numUsersAchieved = dataFromServer.numUsersAchieved;
          this.lastAchieved = dataFromServer.lastAchieved;
          this.numUsersInProgress = dataFromServer.numUsersInProgress;
          this.loading = false;
        });
    },
  };
</script>

<style scoped>

</style>
