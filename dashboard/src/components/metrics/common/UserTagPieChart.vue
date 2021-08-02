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
  <metrics-card :title="title" data-cy="userTagPieChart">
    <metrics-overlay :loading="isLoading" :has-data="!isEmpty" no-data-icon="fa fa-info-circle" no-data-msg="No user data yet...">
      <apexchart type="pie" height="350"  :options="chartOptions" :series="series"></apexchart>
    </metrics-overlay>
  </metrics-card>
</template>

<script>
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';

  export default {
    name: 'UserTagPieChart',
    components: { MetricsOverlay, MetricsCard },
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
            // height: 250,
            // width: 250,
            type: 'pie',
            toolbar: {
              show: true,
              offsetX: 0,
              offsetY: -60,
            },
          },
          // colors: ['#17a2b8', '#28a745'],
          labels: [],
          dataLabels: {
            enabled: false,
          },
          legend: {
            position: 'top',
            horizontalAlign: 'left',
          },
        },
      };
    },
    mounted() {
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
              this.series = series;
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
