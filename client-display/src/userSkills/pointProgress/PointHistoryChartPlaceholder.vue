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
  <apexchart :options="chartOption" :series="series" height="200" type="area"/>
</template>

<script>
  import VueApexCharts from 'vue-apexcharts';

  export default {
    name: 'PointHistoryChartPlaceholder',
    components: {
      apexchart: VueApexCharts,
    },
    data() {
      return {
        chartOption: {
          chart: {
            type: 'area',
            toolbar: {
              offsetY: -30,
            },
          },
          stroke: {
            colors: ['#dee2e6'],
          },
          dataLabels: {
            enabled: false,
          },
          xaxis: {
            type: 'datetime',
            tickAmount: 1,
            labels: {
              style: {
                colors: this.$store.state.themeModule.charts.axisLabelColor,
              },
            },
          },
        },
        series: [{
          data: [
            [1553227200000, 0],
            [1553313600000, 50],
            [1553400000000, 450],
            [1553486400000, 475],
            [1553572800000, 475],
            [1553659200000, 475],
            [1553745600000, 800],
            [1553832000000, 1200],
            [1553918400000, 1250],
            [1554004800000, 1250],
            [1554091200000, 1500],
            [1554177600000, 2000],
            [1554264000000, 2100],
            [1554350400000, 2200],
          ],
          name: 'Points',
        }],
      };
    },
  };
</script>

<style scoped>

</style>
