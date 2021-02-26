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
  <metrics-card title="User Counts" data-cy="numUsersPostAchievement">
    <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No achievements yet for this skill.">
      <apexchart type="pie" height="350" :options="chartOptions" :series="series"></apexchart>
    </metrics-overlay>
  </metrics-card>
</template>

<script>
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';

  export default {
    name: 'PostAchievementUsersPieChart',
    components: { MetricsOverlay, MetricsCard },
    props: ['skillName'],
    data() {
      return {
        series: [],
        chartOptions: {
          chart: {
            height: 250,
            width: 250,
            type: 'pie',
            toolbar: {
              show: true,
              offsetX: 0,
              offsetY: -60,
            },
          },
          colors: ['#17a2b8', '#28a745'],
          labels: ['stopped after achieving', 'performed Skill at least once after achieving'],
          dataLabels: {
            enabled: false,
          },
          legend: {
            position: 'top',
            horizontalAlign: 'left',
          },
        },
        loading: true,
        hasData: false,
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        this.loading = true;
        MetricsService.loadChart(this.$route.params.projectId, 'usagePostAchievementMetricsBuilder', { skillId: this.$route.params.skillId })
          .then((dataFromServer) => {
            // need to check if the properties are defined so that 0 doesn't return false
            if (Object.prototype.hasOwnProperty.call(dataFromServer, 'totalUsersAchieved')
              && Object.prototype.hasOwnProperty.call(dataFromServer, 'usersPostAchievement')) {
              const usersWhoStoppedAfterAchieving = dataFromServer.totalUsersAchieved - dataFromServer.usersPostAchievement;
              this.hasData = true;
              this.series = [usersWhoStoppedAfterAchieving, dataFromServer.usersPostAchievement];
            }
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
