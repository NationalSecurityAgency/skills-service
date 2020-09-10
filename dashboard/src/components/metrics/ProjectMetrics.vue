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
<div class="mb-3">
  <sub-page-header title="Metrics"/>
  <div class="card">
    <div class="card-body">
      <apexchart type="area" height="350" :options="chartOptions" :series="distinctUsersOverTime"></apexchart>
    </div>
  </div>
  <div class="row">
    <div v-for="(navItem, index) in navCards" :key="navItem.title" class="col-sm-6 col-md-4 mt-2">
      <metric-nav-card :title="navItem.title" :subtitle="navItem.subtitle" :description="navItem.description"
                       :icon="getMetricCardColorClass(navItem.icon,index)"/>
    </div>
  </div>
</div>
</template>

<script>
  import SubPageHeader from '@//components/utils/pages/SubPageHeader';
  import MetricsService from './MetricsService';
  import numberFormatter from '@//filters/NumberFilter';
  import MetricNavCard from './MetricNavCard';

  export default {
    name: 'ProjectMetrics',
    components: { MetricNavCard, SubPageHeader },
    data() {
      return {
        isLoading: true,
        metricCardIconsColors: ['text-warning', 'text-primary', 'text-info', 'text-danger'],
        distinctUsersOverTime: [],
        navCards: [{
          title: 'Users And Levels',
          subtitle: 'Explore users by Levels',
          description: 'Ability to understand who your experts are and how they utilize your application',
          icon: 'fa fa-trophy',
        }, {
          title: 'Overlooked Skills',
          subtitle: 'Explore users by Levels',
          description: 'Ability to understand who your experts are and how they utilize your application',
          icon: 'fa fa-trophy',
        }, {
          title: 'Time to Achieve',
          subtitle: 'Explore users by Levels',
          description: 'Ability to understand who your experts are and how they utilize your application',
          icon: 'fa fa-trophy',
        }],
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
          title: {
            text: 'Distinct Number of Users',
            align: 'left',
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
      };
    },
    mounted() {
      MetricsService.loadChart(this.$route.params.projectId, 'distinctUsersOverTimeForProject')
        .then((response) => {
          this.distinctUsersOverTime = [{
            data: response.map(item => [item.value, item.count]),
            name: 'Points',
          }];
        });
    },
    methods: {
      getMetricCardColorClass(icon, index) {
        const colorIndex = this.metricCardIconsColors.length < index ? index : index % this.metricCardIconsColors.length;
        const color = this.metricCardIconsColors[colorIndex];
        return `${icon} ${color}`;
      },
    },
  };
</script>

<style scoped>

</style>
