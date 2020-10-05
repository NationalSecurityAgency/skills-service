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
  <div class="card" data-cy="userCountsBySubjectMetric">
    <div class="card-header">
      <h5>Number of users for each level for each subject</h5>
    </div>
    <div class="card-body">
      <b-overlay v-if="loading" :show="loading" opacity=".5">
        <apexchart v-if="loading" type="bar" height="350" :options="{}" :series="[]"></apexchart>
      </b-overlay>
      <b-overlay v-if="!loading" :show="isEmpty" opacity=".5">
        <apexchart v-if="!loading" type="bar" height="350" :options="chartOptions" :series="series"></apexchart>
        <template v-slot:overlay>
          <div class="alert alert-info">
            <i class="fas fa-user-clock"></i> Users have not achieved any levels, yet...</div>
        </template>
      </b-overlay>
    </div>
  </div>
</template>

<script>
  import MetricsService from '../MetricsService';

  export default {
    name: 'UserCountsBySubjectMetric',
    data() {
      return {
        loading: true,
        isEmpty: true,
        series: [],
        chartOptions: {
          chart: {
            type: 'bar',
            height: 350,
            toolbar: {
              show: true,
              offsetX: 0,
              offsetY: -60,
            },
          },
          plotOptions: {
            bar: {
              horizontal: false,
              columnWidth: '55%',
              endingShape: 'rounded',
            },
          },
          dataLabels: {
            enabled: false,
          },
          stroke: {
            show: true,
            width: 2,
            colors: ['transparent'],
          },
          xaxis: {
            categories: [],
          },
          yaxis: {
            title: {
              text: '# of users',
            },
          },
          fill: {
            opacity: 1,
          },
          tooltip: {
            y: {
              formatter(val) {
                return `${val}`;
              },
            },
          },
        },
      };
    },
    mounted() {
      MetricsService.loadChart(this.$route.params.projectId, 'NumUsersPerSubjectPerLevelChartBuilder')
        .then((res) => {
          this.updateChart(res);
          this.loading = false;
        });
    },
    methods: {
      updateChart(res) {
        const series = [];
        const sortedSubjects = res.sort((subj) => subj.subject);
        this.chartOptions.xaxis.categories = sortedSubjects.map((subj) => subj.subject);
        const allLevels = sortedSubjects.map((subj) => subj.numUsersPerLevels.length);
        if (allLevels) {
          const maxLevel = Math.max(...allLevels);
          for (let i = 1; i <= maxLevel; i += 1) {
            const data = sortedSubjects.map((subj) => {
              const found = subj.numUsersPerLevels.find((item) => item.level === i);
              const numUsers = found ? found.numberUsers : 0;
              if (numUsers > 0) {
                this.isEmpty = false;
              }
              return numUsers;
            });
            series.push({
              name: `Level ${i}`,
              data,
            });
          }
        }
        this.series = series;
      },
    },
  };
</script>

<style scoped>

</style>
