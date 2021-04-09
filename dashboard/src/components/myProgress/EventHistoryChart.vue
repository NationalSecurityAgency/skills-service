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
  <div>
    <metrics-card :title="mutableTitle" data-cy="eventHistoryChart">
      <template v-slot:afterTitle>
        <span class="text-muted ml-2">|</span>
        <time-length-selector ref="timeLengthSelector" :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
      </template>
      <multiselect v-model="projects.selected"
                  :options="projects.available"
                  label="projectName"
                  :multiple="true"
                  track-by="projectId"
                  :hide-selected="true"
                  :max="5"
                  data-cy="eventHistoryChartProjectSelector"/>
      <metrics-overlay :loading="loading" :has-data="hasData" :no-data-msg="noDataMessage">
        <apexchart type="line" height="350"
                  :ref="chartId"
                  :options="chartOptions"
                  :series="series">
        </apexchart>
      </metrics-overlay>
    </metrics-card>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';
  import MetricsCard from '../metrics/utils/MetricsCard';
  import MetricsOverlay from '../metrics/utils/MetricsOverlay';
  import numberFormatter from '@//filters/NumberFilter';
  import TimeLengthSelector from '../metrics/common/TimeLengthSelector';
  import dayjs from '../../DayJsCustomizer';
  import MetricsService from '../metrics/MetricsService';

  export default {
    name: 'EventHistoryChart',
    components: {
      MetricsCard, MetricsOverlay, TimeLengthSelector, Multiselect,
    },
    props: {
      availableProjects: {
        type: Array,
        required: true,
      },
      title: {
        type: String,
        required: false,
        default: 'Your Daily Usage History',
      },
    },
    data() {
      return {
        chartId: this.title.replace(/\s+/g, ''),
        loading: true,
        hasData: false,
        mutableTitle: this.title,
        projects: {
          selected: [],
          available: [],
        },
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
        toolbarOffset: 0,
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
              text: 'Skill Events Reported',
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
      // assign consistent color and dash array options so they don't change when selecting different projects
      const colorOptions = ['#2E93fA', '#66DA26', '#546E7A', '#FF9800'];
      this.projects.available = this.availableProjects.map((proj, idx) => (
        {
          ...proj,
          dashArray: (idx % 4) * 3, // alternate dashArray between 0, 3, 6, 9
          color: colorOptions[(idx % 4)],
        }));
      const numProjectsToSelect = Math.min(this.availableProjects.length, 4);
      const availableSortedByMostPoints = this.projects.available.sort((a, b) => b.points - a.points);
      this.projects.selected = availableSortedByMostPoints.slice(0, numProjectsToSelect);
      // loadData() is not called here because of the watch on `projects.selected`, which is triggered by the assignment above

      // add listener for window resize events
      window.addEventListener('resize', this.updateToolbarOffset);
    },
    updated() {
      this.$nextTick(() => {
        this.updateToolbarOffset();
      });
    },
    computed: {
      enoughOverallProjects() {
        return this.projects.available && this.projects.available.length > 0;
      },
      enoughProjectsSelected() {
        return this.projects.selected && this.projects.selected.length > 0;
      },
      noDataMessage() {
        if (!this.enoughOverallProjects) {
          return 'There are no projects available.';
        }
        if (!this.enoughProjectsSelected) {
          return 'Please select at least one project from the list above.';
        }
        return 'There are no events for the selected project(s) and time period.';
      },
    },
    watch: {
      'projects.selected': function rebuild() {
        this.props.projIds = this.projects.selected.map((project) => project.projectId);
        this.loadData();
      },
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
        if (this.enoughOverallProjects && this.enoughProjectsSelected) {
          MetricsService.loadMyMetrics('allProjectsSkillEventsOverTimeMetricsBuilder', this.props)
            .then((response) => {
              if (response && response.length > 0 && this.notAllZeros(response)) {
                this.hasData = true;
                this.series = response.map((item) => {
                  const ret = {};
                  ret.project = this.projects.available.find(({ projectId }) => projectId === item.project);
                  ret.name = ret.project.projectName;
                  ret.data = item.countsByDay.map((it) => [it.timestamp, it.num]);
                  return ret;
                });
                const dashArray = this.series.map((item) => item.project.dashArray);
                const colors = this.series.map((item) => item.project.color);
                this.$refs[this.chartId].updateOptions({
                  stroke: {
                    dashArray,
                    colors,
                  },
                  colors,
                });
              } else {
                this.series = [];
                this.hasData = false;
              }
              this.loading = false;
            });
        } else {
          this.hasData = false;
          this.loading = false;
        }
      },
      notAllZeros(data) {
        return data.filter((item) => item.countsByDay.find((it) => it.num > 0)).length > 0;
      },
      updateToolbarOffset() {
        const toolbarElem = document.getElementsByClassName('apexcharts-toolbar')[0];
        const timeLengthSelectorElem = this.$refs.timeLengthSelector;
        if (toolbarElem && timeLengthSelectorElem) {
          const toolbarBottom = toolbarElem.getBoundingClientRect().bottom;
          const timeLengthSelectorBottom = timeLengthSelectorElem.$el.getBoundingClientRect().bottom;
          const diff = timeLengthSelectorBottom - toolbarBottom;
          if (diff !== 0) {
            this.toolbarOffset += diff;
            this.$refs[this.chartId].updateOptions({
              chart: {
                toolbar: {
                  offsetY: this.toolbarOffset,
                },
              },
            });
          }
        }
      },
    },
    beforeDestroy() {
      window.removeEventListener('resize', this.updateToolbarOffset);
    },
  };
</script>

<style scoped>
</style>
