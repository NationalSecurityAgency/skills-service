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
  <metrics-card :title="mutableTitle" data-cy="event-history-chart">
    <template v-slot:afterTitle>
      <span class="text-muted ml-2">|</span>
      <time-length-selector :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
    </template>
    <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="There are no projects selected">
      <apexchart type="line" height="350"
                 :ref="chartId"
                 :options="chartOptions"
                 :series="series">
      </apexchart>
    </metrics-overlay>
  </metrics-card>
</template>

<script>
  import MetricsCard from '../metrics/utils/MetricsCard';
  import MetricsOverlay from '../metrics/utils/MetricsOverlay';
  import numberFormatter from '@//filters/NumberFilter';
  import TimeLengthSelector from '../metrics/common/TimeLengthSelector';
  import dayjs from '../../DayJsCustomizer';
  import MetricsService from '../metrics/MetricsService';

  export default {
    name: 'EventHistoryChart',
    components: { MetricsCard, MetricsOverlay, TimeLengthSelector },
    props: {
      projects: {
        type: Array,
        required: true,
      },
      title: {
        type: String,
        required: false,
        default: 'Events per day',
      },
    },
    data() {
      return {
        chartId: this.title.replace(/\s+/g, ''),
        loading: true,
        hasData: false,
        mutableTitle: this.title,
        props: {
          start: dayjs().subtract(30, 'day').valueOf(),
        },
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
        series: [],
        chartOptions: {
          chart: {
            height: 350,
            type: 'line',
            zoom: {
              type: 'x',
              enabled: true,
              autoScaleYaxis: true,
            },
            toolbar: {
              autoSelected: 'zoom',
              offsetY: -52,
            },
          },
          dataLabels: {
            enabled: false,
          },
          stroke: {
            curve: 'smooth',
          },
          markers: {
            size: 0,
            hover: {
              sizeOffset: 6,
            },
          },
          yaxis: {
            labels: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
            title: {
              text: 'Events Reported',
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
          grid: {
            borderColor: '#f1f1f1',
          },
        },
      };
    },
    mounted() {
      const hasProjects = this.projects && this.projects.length > 0;
      this.hasData = hasProjects;
      this.loadData();
    },
    methods: {
      updateTimeRange(timeEvent) {
        if (this.$store.getters.config) {
          const oldestDaily = dayjs().subtract(this.$store.getters.config.maxDailyUserEvents, 'day');
          if (timeEvent.startTime < oldestDaily) {
            this.mutableTitle = 'Events per week';
          } else {
            this.mutableTitle = this.title;
          }
        }
        this.props.start = timeEvent.startTime.valueOf();
        this.loadData();
      },
      loadData() {
        this.loading = true;
        MetricsService.loadMyMetrics('allProjectsSkillEventsOverTimeMetricsBuilder', this.props)
          .then((response) => {
            if (response && response.length > 0) {
              this.hasData = true;
              this.series = response.map((item) => {
                const ret = {};
                ret.name = this.projects.find(({ projectId }) => projectId === item.project).projectName;
                ret.data = item.countsByDay.map((it) => [it.timestamp, it.num]);
                return ret;
              });
              // alternate dashArray between 0, 3, 6, 9
              this.$refs[this.chartId].updateOptions({
                stroke: {
                  dashArray: Array(this.series.length).fill().map((_, idx) => (idx % 4) * 3),
                },
              });
            } else {
              this.series = [];
              this.hasData = false;
            }
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>
.charts-content {
  /* this little hack is required to prevent apexcharts from wrapping onto a new line;
  the gist is that they calculate width dynamically and do not work properly with the width of 0*/
  min-width: 1rem !important;
}
</style>
