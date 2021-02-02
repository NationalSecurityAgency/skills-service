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
  <metrics-card :title=title data-cy="appliedSkillEventsOverTimeMetric">
    <template v-slot:afterTitle>
      <span class="text-muted ml-2">|</span>
      <time-length-selector :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
    </template>
    <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="This chart needs at least 2 days of user activity.">
      <apexchart type="line" height="350" :options="chartOptions" :series="series"></apexchart>
    </metrics-overlay>
    <div class="text-muted small">Please Note: Only 'applied' events contribute to users' points and achievements. An event will not be applied if that skill has already reached its maximum points or has unfulfilled dependencies.</div>
  </metrics-card>
</template>

<script>
  import TimeLengthSelector from '../common/TimeLengthSelector';
  import numberFormatter from '@//filters/NumberFilter';
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';
  import dayjs from '../../../DayJsCustomizer';

  export default {
    name: 'SkillEventsOverTime',
    components: { MetricsOverlay, MetricsCard, TimeLengthSelector },
    props: ['skillName'],
    data() {
      return {
        title: 'Skill events',
        loading: true,
        hasData: false,
        series: [],
        start: dayjs().subtract(30, 'day').valueOf(),
        timeSelectorOptions: [
          {
            length: 30,
            unit: 'days',
          },
          {
            length: 6,
            unit: 'months',
          },
          {
            length: 1,
            unit: 'year',
          },
        ],
        chartOptions: {
          chart: {
            height: 250,
            type: 'line',
            id: 'areachart-2',
            toolbar: {
              show: false,
            },
          },
          colors: ['#28a745', '#008ffb'],
          dataLabels: {
            enabled: false,
          },
          stroke: {
            curve: 'smooth',
            colors: ['#28a745', '#008ffb'],
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
        MetricsService.loadChart(this.$route.params.projectId, 'skillEventsOverTimeChartBuilder', { skillId: this.$route.params.skillId, start: this.start })
          .then((dataFromServer) => {
            let appliedEvents = [];
            let allEvents = [];
            if (dataFromServer.countsByDay && dataFromServer.countsByDay.length > 1) {
              appliedEvents = dataFromServer.countsByDay.map((item) => [item.timestamp, item.num]);
            }
            if (dataFromServer.allEvents && dataFromServer.allEvents.length > 0) {
              allEvents = dataFromServer.allEvents.map((item) => [item.timestamp, item.num]);
            }

            const s = [];
            let hasAppliedSkillEvents = false;
            if (appliedEvents && appliedEvents.length > 0) {
              s.push({
                name: 'Applied Skill Events',
                data: appliedEvents,
              });
              hasAppliedSkillEvents = true;
            }

            let hasAllEvents = false;
            if (allEvents && allEvents.length > 0) {
              s.push({
                name: 'All Skill Events',
                data: allEvents,
              });
              hasAllEvents = true;
            }

            // eslint-disable-next-line
            this.hasData = Boolean(hasAllEvents | hasAppliedSkillEvents);
            this.series = s;
            this.loading = false;
          });
      },
      updateTimeRange(timeEvent) {
        this.start = timeEvent.startTime.valueOf();
        this.loadData();
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
