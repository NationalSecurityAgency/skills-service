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
  <metrics-card :title="title" data-cy="userTagChart">
    <metrics-overlay :loading="isLoading" :has-data="!isEmpty" no-data-icon="fa fa-info-circle" no-data-msg="No user data yet...">
      <apexchart :type="this.chartType" height="350"  :options="chartOptions" :series="series"></apexchart>
    </metrics-overlay>
  </metrics-card>
</template>

<script>
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';
  import UserTagChartMixin from './UserTagChartMixin';

  const PIE = 'pie';
  const BAR = 'bar';

  export default {
    name: 'UserTagChart',
    mixins: [UserTagChartMixin],
    components: { MetricsOverlay, MetricsCard },
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
        MetricsService.loadChart(this.$route.params.projectId, 'numUsersPerTagBuilder', { tagKey: this.tagKey })
          .then((dataFromServer) => {
            if (dataFromServer) {
              const series = [];
              const labels = [];
              dataFromServer.forEach((data) => {
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
              this.chartOptions = { labels };
              this.isEmpty = dataFromServer.find((item) => item.count > 0) === undefined;
            }
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>
</style>
