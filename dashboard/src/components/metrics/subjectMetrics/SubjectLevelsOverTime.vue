<template>
<div class="card mt-2">
  <div class="card-header">
    <h5>Number of users for each level over time</h5>
  </div>
  <div class="card-body">
    <b-form-select style="width: 20rem;" id="input-3" class="mb-2"
                   v-model="subjects.selected" :options="subjects.available" required/>
    <apexchart type="area" height="300" :options="chartOptions" :series="series"></apexchart>
  </div>
</div>
</template>

<script>
  export default {
    name: 'SubjectLevelsOverTime',
    data() {
      return {
        subjects: {
          selected: 'Subject 1',
          available: ['Subject 1', 'Subject 2', 'Subject 3', 'Subject 4', 'Subject 5'],
        },
        series: [{
          name: 'Level 1',
          data: this.generateDayWiseTimeSeries(new Date('11 Feb 2020').getTime(), 100, {
            min: 10,
            max: 60,
          }),
        }, {
          name: 'Level 2',
          data: this.generateDayWiseTimeSeries(new Date('11 Mar 2020').getTime(), 71, {
            min: 10,
            max: 60,
          }),
        }, {
          name: 'Level 3',
          data: this.generateDayWiseTimeSeries(new Date('22 Apr 2020').getTime(), 29, {
            min: 10,
            max: 60,
          }),
        }],
        chartOptions: {
          chart: {
            type: 'line',
          },
          colors: ['#008FFB', '#546E7A', '#00E396'],
          yaxis: {
            title: {
              text: '# of users',
            },
          },
          xaxis: {
            type: 'datetime',
          },
          dataLabels: {
            enabled: false,
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
