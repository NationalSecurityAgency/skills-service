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
  <metrics-card title="Achievements over time" data-cy="numUsersAchievedOverTimeMetric">
    <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="This chart needs at least 1 day of user activity.">
      <apexchart type="area" height="350" :options="chartOptions" :series="series"></apexchart>
    </metrics-overlay>
  </metrics-card>
</template>

<script>
  import numberFormatter from '@//filters/NumberFilter';
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';

  export default {
    name: 'SkillAchievedByUsersOverTime',
    components: { MetricsOverlay, MetricsCard },
    props: ['skillName'],
    data() {
      return {
        series: [],
        chartOptions: {
          chart: {
            height: 250,
            type: 'area',
            toolbar: {
              show: false,
            },
          },
          dataLabels: {
            enabled: false,
          },
          stroke: {
            curve: 'smooth',
          },
          fill: {
            type: 'gradient',
            gradient: {
              shade: 'light',
              gradientToColors: ['#17a2b8', '#28a745'],
              shadeIntensity: 1,
              type: 'horizontal',
              opacityFrom: 0.3,
              opacityTo: 0.8,
              stops: [0, 100, 100, 100],
            },
          },
          grid: {
            padding: {
              right: 30,
              left: 20,
            },
          },
          xaxis: {
            type: 'datetime',
          },
          yaxis: {
            labels: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
            title: {
              text: '# Users',
            },
          },
          legend: {
            position: 'top',
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
        MetricsService.loadChart(this.$route.params.projectId, 'numUserAchievedOverTimeChartBuilder', { skillId: this.$route.params.skillId })
          .then((dataFromServer) => {
            if (dataFromServer.achievementCounts) {
              const datSeries = dataFromServer.achievementCounts.map((item) => [item.timestamp, item.num]);
              this.hasData = datSeries.length > 0;
              if (this.hasData) {
                const dayAgo = dataFromServer.achievementCounts[0].timestamp - (1000 * 60 * 60 * 24);
                datSeries.unshift([dayAgo, 0]);
              }
              this.series = [{
                name: '# Users Achieved',
                data: datSeries,
              }];
            }
            this.loading = false;
          });
      },
      generateDayWiseTimeSeries(xValStart, count, yrange) {
        let baseXVal = xValStart;
        let baseYVal = 0;
        let i = 0;
        const series = [];
        while (i < count) {
          const x = baseXVal;
          const y = baseYVal;
          series.push([x, y]);

          baseXVal += 86400000;
          const randomValue = Math.floor(Math.random() * (yrange.max - yrange.min + 1)) + yrange.min;
          baseYVal += randomValue;
          i += 1;
        }
        return series;
      },
    },
  };
</script>

<style scoped>

</style>
