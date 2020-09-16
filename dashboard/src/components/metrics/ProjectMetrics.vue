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
  <div class="card">
    <div class="card-header">
      <h5>Distinct number of users over time</h5>
    </div>
    <div class="card-body">
      <apexchart type="area" height="350" :options="chartOptions" :series="distinctUsersOverTime"></apexchart>
    </div>
  </div>
</template>

<script>
  import MetricsService from './MetricsService';
  import numberFormatter from '@//filters/NumberFilter';

  export default {
    name: 'ProjectMetrics',
    data() {
      return {
        isLoading: true,
        distinctUsersOverTime: [],
        chartOptions: {
          chart: {
            type: 'area',
            stacked: false,
            height: 350,
            zoom: {
              type: 'x',
              enabled: true,
              autoScaleYaxis: true,
            },
            toolbar: {
              autoSelected: 'zoom',
            },
          },
          dataLabels: {
            enabled: false,
          },
          markers: {
            size: 0,
          },
          fill: {
            type: 'gradient',
            gradient: {
              shadeIntensity: 1,
              inverseColors: false,
              opacityFrom: 0.5,
              opacityTo: 0,
              stops: [0, 90, 100],
            },
          },
          yaxis: {
            labels: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
            title: {
              text: 'Distinct # of Users',
            },
          },
          xaxis: {
            type: 'datetime',
          },
          tooltip: {
            shared: false,
            y: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
          },
        },
        navCards: [{
          title: 'Achievements',
          subtitle: 'Explore users\' achievements',
          description: 'Browse users\' achieved overall levels, subject level achievements as well as earned badges',
          icon: 'fa fa-trophy text-warning',
          pathName: 'UsersAndLevelsMetrics',
        }, {
          title: 'Subjects',
          subtitle: 'Achievements by Subjects',
          description: 'Detailed breakdown how users are earning skills under each subject',
          icon: 'fa fa-cubes text-primary',
          pathName: 'SubjectMetricsPage',
        }, {
          title: 'Skills',
          subtitle: 'Understand Skills Usage',
          description: 'Find top-achieved skills and overlooked skills. Learn how much each skill is utilized within your applicaiton.',
          icon: 'fa fa-graduation-cap text-info',
          pathName: 'SkillsMetricsPage',
        }],
      };
    },
    mounted() {
      MetricsService.loadChart(this.$route.params.projectId, 'distinctUsersOverTimeForProject')
        .then((response) => {
          this.distinctUsersOverTime = [{
            data: response.map((item) => [item.value, item.count]),
            name: 'Points',
          }];
        });
    },
  };
</script>

<style scoped>

</style>
