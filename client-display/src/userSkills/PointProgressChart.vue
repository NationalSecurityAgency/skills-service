<template>
  <div>
    <h4>Point History</h4>
    <apexchart
      v-if="chartOptions && hasData"
      :options="chartOptions"
      :series="chartSeries"
      height="250" type="area" />
  </div>
</template>

<script>
  import VueApexCharts from 'vue-apexcharts';

  export default {
    components: {
      apexchart: VueApexCharts,
    },
    props: {
      pointsHistory: Array,
    },
    data() {
      return {
        chartOptions: {
          chart: {
            type: 'area',
            height: 250,
          },
          dataLabels: {
            enabled: false,
          },
          markers: {
            size: 0,
            style: 'hollow',
          },
          xaxis: {
            type: 'datetime',
            tickAmount: 1,
          },
          fill: {
            type: 'gradient',
            gradient: {
              shadeIntensity: 1,
              opacityFrom: 0.7,
              opacityTo: 0.9,
              stops: [0, 100],
            },
          },
        },
      };
    },
    computed: {
      hasData() {
        return this.chartSeries && this.chartSeries[0].data.length > 0;
      },

      chartSeries() {
        const dataArray = [{
          data: [],
          name: 'Points',
        }];

        if (this.pointsHistory.length > 0) {
          dataArray[0].data = this.pointsHistory
            .map(value => ({
              x: new Date(parseInt(value.dayPerformed, 10)).getTime(),
              y: value.points,
            }));
        }

        return dataArray;
      },
    },
  };
</script>

<style scoped>
  h4 {
    color: #666666;
  }
</style>
