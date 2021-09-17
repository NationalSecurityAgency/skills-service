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
  <metrics-card :title="titleInternal" data-cy="userTagChart">
    <skills-spinner :is-loading="isLoading" class="mb-5"/>
    <div v-if="!isLoading">
      <metrics-overlay :loading="isLoading" :has-data="!isEmpty" no-data-msg="No data yet...">
        <apexchart :type="this.chartType" :height="`${heightInPx}px`"  :options="chartOptions" :series="series"></apexchart>
      </metrics-overlay>
    </div>
  </metrics-card>
</template>

<script>
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import UserTagChartMixin from './UserTagChartMixin';
  import SkillsSpinner from '../../utils/SkillsSpinner';
  import MetricsOverlay from '../utils/MetricsOverlay';

  const PIE = 'pie';
  const BAR = 'bar';

  export default {
    name: 'UserTagChart',
    mixins: [UserTagChartMixin],
    components: {
      MetricsOverlay,
      SkillsSpinner,
      MetricsCard,
    },
    props: {
      tagKey: {
        type: String,
        required: true,
      },
      chartType: {
        type: String,
        required: true,
        validator: (value) => ([PIE, BAR].indexOf(value) >= 0),
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
        chartOptions: {},
        heightInPx: 350,
        titleInternal: this.title,
      };
    },
    mounted() {
      if (this.chartType === PIE) {
        this.chartOptions = this.pieChartOptions;
      }
      if (this.chartType === BAR) {
        this.chartOptions = this.barChartOptions;
      }
      this.loadData();
    },
    methods: {
      loadData() {
        this.isLoading = true;

        const params = {
          tagKey: this.tagKey,
          currentPage: 1,
          pageSize: 20,
          sortDesc: true,
          tagFilter: '',
        };
        const self = this;
        MetricsService.loadChart(this.$route.params.projectId, 'numUsersPerTagBuilder', params)
          .then((dataFromServer) => {
            if (dataFromServer) {
              const series = [];
              const labels = [];
              const { items } = dataFromServer;
              items.forEach((data) => {
                series.push(data.count);
                labels.push(data.value);
              });
              if (this.chartType === PIE) {
                this.series = series;
              }
              if (this.chartType === BAR) {
                this.series = [{
                  name: 'Number of Users',
                  data: series,
                }];
              }
              this.chartOptions = Object.assign(this.chartOptions, { labels });
              this.isEmpty = items.find((item) => item.count > 0) === undefined;

              if (items.length > 10) {
                this.heightInPx = 600;
              }
              if (dataFromServer.totalNumItems > params.pageSize) {
                self.titleInternal = `${self.titleInternal} (Top ${params.pageSize})`;
              }
            }
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>
</style>
