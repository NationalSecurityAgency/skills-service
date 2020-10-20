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
  <div class="card" data-cy="projectOverallLevelsChart">
    <div class="card-header">
      Overall Levels
    </div>
    <div class="card-body p-0">
      <metrics-spinner v-if="isLoading"/>
      <apexchart v-if="!isLoading" type="bar" height="350" :options="chartOptions" :series="series" />
      <div v-if="!isLoading && isEmpty" class="card-img-overlay d-flex">
        <div class="my-auto mx-auto text-center">
          <div class="alert alert-info"><i class="fa fa-info-circle"/> No one reached <b-badge>Level 1</b-badge> yet...</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import numberFormatter from '@/filters/NumberFilter';
  import MetricsService from '../MetricsService';
  import MetricsSpinner from '../utils/MetricsSpinner';

  export default {
    name: 'OverallLevelBreakdownMetric',
    components: { MetricsSpinner },
    data() {
      return {
        isLoading: true,
        isEmpty: false,
        series: [],
        chartOptions: {
          chart: {
            type: 'bar',
            height: 350,
            toolbar: {
              show: true,
              offsetX: -20,
              offsetY: -40,
            },
          },
          plotOptions: {
            bar: {
              barHeight: '90%',
              endingShape: 'rounded',
              distributed: true,
              horizontal: true,
              dataLabels: {
                position: 'bottom',
              },
            },
          },
          dataLabels: {
            enabled: true,
            textAnchor: 'start',
            style: {
              colors: ['#17a2b8'],
              fontSize: '14px',
              fontFamily: 'Helvetica, Arial, sans-serif',
              fontWeight: 'bold',
            },
            formatter(val, opt) {
              return `${opt.w.globals.labels[opt.dataPointIndex]}: ${numberFormatter(val)} users`;
            },
            offsetX: 0,
            dropShadow: {
              enabled: true,
            },
            background: {
              enabled: true,
              foreColor: '#ffffff',
              padding: 10,
              borderRadius: 2,
              borderWidth: 1,
              borderColor: '#686565',
              opacity: 1,
              dropShadow: {
                enabled: false,
              },
            },
          },
          stroke: {
            show: true,
            width: 2,
            colors: ['transparent'],
          },
          xaxis: {
            categories: [],
            labels: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
          },
          yaxis: {
            labels: {
              show: false,
            },
          },
          grid: {
            borderColor: '#cfeaf3',
            position: 'front',
          },
          legend: {
            show: false,
          },
          fill: {
            opacity: 1,
          },
          tooltip: {
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
      MetricsService.loadChart(this.$route.params.projectId, 'numUsersPerLevelChartBuilder')
        .then((response) => {
          // sort by level
          const sorted = response.sort((item) => item.value).reverse();

          this.isEmpty = response.find((item) => item.count > 0) === undefined;
          this.chartOptions.xaxis.categories = sorted.map((item) => item.value);
          this.series = [{
            name: 'Number of Users',
            data: sorted.map((item) => item.count),
          }];
          this.isLoading = false;
        });
    },
  };
</script>

<style scoped>
.card-body {
  min-height: 350px;
}
</style>
