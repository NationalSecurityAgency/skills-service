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
  <metrics-card title="Applied skill events per day" data-cy="appliedSkillEventsOverTimeMetric">
    <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="This chart needs at least 2 days of user activity.">
      <apexchart type="line" height="350" :options="chartOptions" :series="series"></apexchart>
    </metrics-overlay>
    <div class="text-muted small">Please Note: Only 'applied' events contribute to users' points and achievements. An event will not be applied if that skill has already reached its maximum points or has unfulfilled dependencies.</div>
  </metrics-card>
</template>

<script>
  import numberFormatter from '@//filters/NumberFilter';
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';

  export default {
    name: 'AppliedSkillEventsOverTime',
    components: { MetricsOverlay, MetricsCard },
    props: ['skillName'],
    data() {
      return {
        loading: true,
        hasData: false,
        series: [],
        chartOptions: {
          chart: {
            height: 250,
            type: 'line',
            id: 'areachart-2',
            toolbar: {
              show: false,
            },
          },
          dataLabels: {
            enabled: false,
          },
          stroke: {
            curve: 'smooth',
            colors: ['#28a745'],
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
            min: 0,
            labels: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
            title: {
              text: '# of Applied Skill Events',
            },
          },
          legend: {
            position: 'top',
          },
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        this.loading = true;
        MetricsService.loadChart(this.$route.params.projectId, 'skillEventsOverTimeChartBuilder', { skillId: this.$route.params.skillId })
          .then((dataFromServer) => {
            if (dataFromServer.countsByDay && dataFromServer.countsByDay.length > 1) {
              this.hasData = true;
              const datSeries = dataFromServer.countsByDay.map((item) => [item.timestamp, item.num]);
              this.series = [{
                name: '# Events',
                data: datSeries,
              }];
            }
            this.loading = false;
          });
      },
      generateDayWiseTimeSeries(xValStart, count, yrange) {
        let baseXVal = xValStart;
        let i = 0;
        const series = [];
        while (i < count) {
          const x = baseXVal;
          const randomValue = Math.floor(Math.random() * (yrange.max - yrange.min + 1)) + yrange.min;
          const y = randomValue;
          series.push([x, y]);

          baseXVal += 86400000;
          // console.log(`${xValStartGrowing} <> ${x}`);
          i += 1;
        }
        return series;
      },
    },
  };
</script>

<style scoped>

</style>
