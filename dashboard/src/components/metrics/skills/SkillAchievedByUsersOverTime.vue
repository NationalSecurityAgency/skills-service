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
    <apexchart type="line" height="350" :options="chartOptions" :series="series"></apexchart>
  </div>
</template>

<script>
  export default {
    name: 'SkillAchievedByUsersOverTime',
    props: ['skillName'],
    data() {
      return {

        series: [{
          name: 'This Skills',
          data: this.generateDayWiseTimeSeries(new Date('11 Mar 2020').getTime(), 91, {
            min: 10,
            max: 60,
          }),
        }, {
          name: 'Skill Achieved by Most Users',
          data: this.generateDayWiseTimeSeries(new Date('11 Feb 2020').getTime(), 120, {
            min: 10,
            max: 100,
          }),
        }, {
          name: 'Average Skill',
          data: this.generateDayWiseTimeSeries(new Date('11 Jan 2020').getTime(), 151, {
            min: 10,
            max: 20,
          }),
        }],
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
            curve: 'straight',
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
            labels: {
              formatter(val) {
                return (val / 1000000).toFixed(0);
              },
            },
            title: {
              text: '# Users',
            },
          },
          legend: {
            position: 'top',
          },
        },
      };
    },
    methods: {
      generateDayWiseTimeSeries(xValStart, count, yrange) {
        let baseXVal = xValStart;
        let baseYVal = 0;
        let i = 0;
        const series = [];
        while (i < count) {
          const x = baseXVal;
          const y = baseYVal;
          series.push([x, y]);

          baseXVal += 86400000;
          // console.log(`${xValStartGrowing} <> ${x}`);
          const randomValue = Math.floor(Math.random() * (yrange.max - yrange.min + 1)) + yrange.min;
          baseYVal += randomValue;
          i += 1;
        }
        return series;
      },
    },
  };
</script>

<style scoped>

</style>
