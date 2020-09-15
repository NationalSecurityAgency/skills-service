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
  <div class="card">
    <div class="card-header">
      <h5>Number of users for each level for each subject</h5>
    </div>
    <div class="card-body">
      <apexchart type="bar" height="350" :options="chartOptions" :series="series"></apexchart>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'UserCountsBySubjectMetric',
    data() {
      return {

        series: [{
          name: 'Level 1',
          data: [44, 55, 57, 56, 61, 58],
        }, {
          name: 'Level 2',
          data: [76, 85, 101, 98, 87, 105],
        }, {
          name: 'Level 3',
          data: [35, 41, 36, 26, 45, 48],
        }, {
          name: 'Level 4',
          data: [63, 60, 66, 91, 114, 94],
        }, {
          name: 'Level 5',
          data: [35, 41, 36, 52, 53, 41],
        }],
        chartOptions: {
          chart: {
            type: 'bar',
            height: 350,
          },
          plotOptions: {
            bar: {
              horizontal: false,
              columnWidth: '55%',
              endingShape: 'rounded',
            },
          },
          dataLabels: {
            enabled: false,
          },
          stroke: {
            show: true,
            width: 2,
            colors: ['transparent'],
          },
          xaxis: {
            categories: ['Subject 1', 'Subject 2', 'Subject 3', 'Subject 4', 'Subject 5', 'Subject 6'],
          },
          yaxis: {
            title: {
              text: '# of users',
            },
          },
          fill: {
            opacity: 1,
          },
          tooltip: {
            y: {
              formatter(val) {
                return `${val}`;
              },
            },
          },
        },

      };
    },
  };
</script>

<style scoped>

</style>
