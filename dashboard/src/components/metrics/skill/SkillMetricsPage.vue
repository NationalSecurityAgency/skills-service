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

    <!-- on FF charts end up pushing column to the next row; this is a workaround -->
    <div v-if="!loading" style="width: 99%;">
      <div class="row mb-3">
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
            <span v-if="lastAchieved">This skill was last achieved on <span class="text-success">{{ lastAchieved | date}}</span></span>
            <span v-else>This skill was <span class="text-info">never</span> achieved.</span>
          </stats-card>
        </div>

      </div>

      <skill-achieved-by-users-over-time class="mb-3"/>
      <skill-events-over-time class="mb-3"/>
      <metrics-card title="Post Achievement Metrics" data-cy="postAchievementContainers">
        <div class="row">
            <div class="col-xl-3 col-lg-4 col-md-12 col-sm-12 mb-3">
              <post-achievement-users-pie-chart class="h-100"/>
            </div>
            <div class="col-xl-9 col-lg-8 col-md-12 col-sm-12 mb-3">
              <binned-post-achievement-usage class="h-100"/>
            </div>
        </div>
      </metrics-card>
    </div>
  </div>
</template>

<script>
  import SubPageHeader from '@//components/utils/pages/SubPageHeader';
  import SkillAchievedByUsersOverTime from './SkillAchievedByUsersOverTime';
  import SkillEventsOverTime from './SkillEventsOverTime';
  import StatsCard from '../utils/StatsCard';
  import MetricsService from '../MetricsService';
  import SkillsSpinner from '../../utils/SkillsSpinner';
  import PostAchievementUsersPieChart from './PostAchievementUsersPieChart';
  import BinnedPostAchievementUsage from './BinnedPostAchievementUsage';
  import MetricsCard from '../utils/MetricsCard';

  export default {
    name: 'SkillMetricsPage',
    components: {
      SkillsSpinner,
      StatsCard,
      SkillEventsOverTime,
      SkillAchievedByUsersOverTime,
      SubPageHeader,
      PostAchievementUsersPieChart,
      BinnedPostAchievementUsage,
      MetricsCard,
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
