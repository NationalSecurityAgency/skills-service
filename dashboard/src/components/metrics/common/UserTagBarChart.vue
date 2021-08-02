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
  <metrics-card :title="title" :no-padding="true" data-cy="userTagBarChart" >
    <metrics-overlay :loading="isLoading" :has-data="!isEmpty" no-data-icon="fa fa-info-circle" no-data-msg="No user data yet...">
      <apexchart type="bar" height="350" :options="chartOptions" :series="series" />
    </metrics-overlay>
  </metrics-card>
</template>

<script>
  import numberFormatter from '@/filters/NumberFilter';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';
  import MetricsCard from '../utils/MetricsCard';

  export default {
    name: 'UserTagBarChart',
    components: { MetricsCard, MetricsOverlay },
    props: {
      tagKey: {
        type: String,
        required: true,
      },
      title: {
        type: String,
        required: false,
        default: 'Users',
      },
    },
    data() {
      return {
        isLoading: true,
        isEmpty: false,
        series: [],
        chartOptions: {
          chart: {
            type: 'bar',
            // height: 350,
            toolbar: {
              show: true,
              offsetX: -20,
              offsetY: -35,
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
      MetricsService.loadChart(this.$route.params.projectId, 'numUsersPerTagBuilder', { tagKey: this.tagKey })
        .then((dataFromServer) => {
          if (dataFromServer) {
            const series = [];
            const labels = [];
            dataFromServer.forEach((data) => {
              series.push(data.count);
              labels.push(data.value);
            });
            this.series = [{
              name: 'Number of Users',
              data: series,
            }];

            this.chartOptions = { labels };
            this.isEmpty = dataFromServer.find((item) => item.count > 0) === undefined;
          }
          this.isLoading = false;
        });
    },
  };
</script>

<style scoped>
</style>
