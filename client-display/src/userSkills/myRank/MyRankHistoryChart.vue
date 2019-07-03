<template>
  <apexchart
      v-if="chartOptions"
      :options="chartOptions"
      :series="chartSeries"
      height="250" type="area" />
</template>

<script>
  import VueApexCharts from 'vue-apexcharts';

  export default {
    components: {
      apexchart: VueApexCharts,
    },
    data() {
      return {
        chartSeries: [{
          data: [
            [1553227200000, 0],
            [1553313600000, 50],
            [1553400000000, 45],
            [1553486400000, 47],
            [1553572800000, 35],
            [1553659200000, 35],
            [1553745600000, 35],
            [1553832000000, 35],
            [1553918400000, 32],
            [1554004800000, 35],
            [1554091200000, 28],
            [1554177600000, 28],
            [1554264000000, 25],
            [1554350400000, 24],
            [1554436800000, 22],
            [1554523200000, 34],
            [1554609600000, 25],
            [1554696000000, 25],
            [1554782400000, 25],
            [1554868800000, 24],
            [1554955200000, 20],
          ],
        }],
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
            labels: {
              style: {
                colors: this.$store.state.themeModule.charts.axisLabelColor,
              },
            },
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
  };
</script>

<style scoped>

</style>
