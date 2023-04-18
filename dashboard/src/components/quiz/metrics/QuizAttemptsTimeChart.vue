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
  <b-card header="Runs over Time">
    <metrics-overlay :loading="loading" :has-data="hasData && numItems > 1" no-data-msg="This chart needs at least 2 days worth of runs">
      <apexchart type="line" :height="chartOptions.chart.height" :options="chartOptions" :series="series"></apexchart>
    </metrics-overlay>
  </b-card>
</template>

<script>
  import numberFormatter from '@/filters/NumberFilter';
  import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay';
  import QuizService from '@/components/quiz/QuizService';

  export default {
    name: 'QuizAttemptsTimeChart',
    components: { MetricsOverlay },
    data() {
      return {
        loading: false,
        quizId: this.$route.params.quizId,
        hasData: false,
        numItems: 0,
        series: [],
        chartOptions: {
          chart: {
            height: 350,
            type: 'line',
            id: 'quizAttemptsTimeChart',
            toolbar: {
              show: true,
              offsetY: -52,
              autoSelected: 'zoom',
              tools: {
                pan: false,
              },
            },
          },
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
              text: '# of Runs',
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
        QuizService.getUsageOverTime(this.quizId)
          .then((res) => {
            this.hasData = res && res.length > 0;
            this.numItems = this.hasData ? res.length : 0;
            this.series = [{
              data: res.map((item) => [item.value, item.count]),
              name: 'Runs',
            }];
          })
          .finally(() => {
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
