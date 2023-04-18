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
  <metrics-card :title="`Top 20 ${tag.label} User Counts`" data-cy="numUsersByTag">
    <div class="row">
      <div class="col-xl-6 col-lg-5 col-md-12 col-sm-12 mb-3">
        <metrics-card title="In Progress" data-cy="usersInProgressByTag">
          <metrics-overlay :loading="loading" :has-data="inProgressSeries.length > 0" no-data-msg="No users currently working on this skill.">
            <apexchart v-if="!loading" type="bar" height="350" :options="chartOptions" :series="inProgressSeries"></apexchart>
          </metrics-overlay>
        </metrics-card>
      </div>
      <div class="col-xl-6 col-lg-5 col-md-12 col-sm-12 mb-3">
        <metrics-card title="Achieved" data-cy="usersAchievedByTag">
          <metrics-overlay :loading="loading" :has-data="achievedSeries.length > 0" no-data-msg="No achievements yet for this skill.">
            <apexchart v-if="!loading" type="bar" height="350" :options="chartOptions" :series="achievedSeries"></apexchart>
          </metrics-overlay>
        </metrics-card>
      </div>
    </div>
  </metrics-card>
</template>

<script>
  import numberFormatter from '@/filters/NumberFilter';
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';

  export default {
    name: 'UsersByTagChart',
    components: { MetricsOverlay, MetricsCard },
    props: ['tag'],
    data() {
      return {
        inProgressSeries: [],
        achievedSeries: [],
        chartOptions: {
          chart: {
            height: 250,
            width: 250,
            type: 'bar',
            toolbar: {
              show: true,
              offsetX: 0,
              offsetY: -60,
            },
          },
          tooltip: {
            y: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
          },
          plotOptions: {
            bar: {
              horizontal: true,
              barHeight: '30%',
              dataLabels: {
                position: 'bottom',
              },
              distributed: true,
            },
          },
          stroke: {
            show: true,
            width: 2,
            colors: ['transparent'],
          },
          xaxis: {
            categories: [],
            title: {
              text: '# of Users',
            },
            labels: {
              style: {
                fontSize: '13px',
                fontWeight: 600,
              },
            },
          },
          yaxis: {
            categories: [],
            title: {
              text: this.tag.label,
            },
          },
          dataLabels: {
            enabled: false,
          },
          legend: {
            show: false,
          },
        },
        loading: true,
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        this.loading = true;
        MetricsService.loadChart(this.$route.params.projectId, 'skillAchievementsByTagBuilder', { skillId: this.$route.params.skillId, userTagKey: this.tag.key })
          .then((dataFromServer) => {
            this.chartOptions.labels = Object.keys(dataFromServer);
            const inProgressData = [];
            const achievedData = [];

            this.chartOptions.labels.forEach((label) => {
              inProgressData.push({ x: label, y: dataFromServer[label].numberInProgress });
              achievedData.push({ x: label, y: dataFromServer[label].numberAchieved });
            });

            const totalInProgressData = inProgressData.map((value) => value.y).filter((value) => value > 0);
            const totalAchievedData = achievedData.map((value) => value.y).filter((value) => value > 0);

            if (inProgressData.length > 0 && totalInProgressData.length > 0) {
              this.inProgressSeries = [{ data: inProgressData, name: 'In Progress' }];
            }

            if (achievedData.length > 0 && totalAchievedData.length > 0) {
              this.achievedSeries = [{ data: achievedData, name: 'Achieved' }];
            }
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
