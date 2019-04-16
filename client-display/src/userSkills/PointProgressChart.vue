<template>
  <div
    :class="{'disabled': !hasData}"
    class="card position-relative">
    <div
      v-if="!hasData"
      class="disabled-overlay" />
    <div
      v-if="!hasData"
      class="text-center user-skills-no-data-icon-text">
      <h1 class="">Locked</h1>
      <div class="user-skills-no-data-icon-subtext">*** Earn more points to unlock points progress chart! ***</div>
    </div>
    <div class="card-header">
      <h6 class="card-title mb-0 float-left">Point History</h6>
    </div>
    <div class="card-body mb-0 pb-0">
      <apexchart
        v-if="chartOptions"
        :options="chartOptions"
        :series="chartSeries"
        height="200" type="area" />
    </div>
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
        return this.pointsHistory && this.pointsHistory.length;
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
        } else {
          // Throw some sample data for the 'no data' chart
          dataArray[0].data = [
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
          ];
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

  .card.disabled .card-header,
  .card.disabled .card-body {
    opacity: 0.4;
  }

  .disabled-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #666666;
    opacity: 0;
    z-index: 999;
  }

  .user-skills-no-data-icon-text {
    font-weight: 700;
    background-color: #eaeaea;
    color: #f10d1a;
    opacity: 0.8;
    position: absolute;
    left: 0;
    top: 50%;
    z-index: 1000;
    text-align: center;
    width: 100%;
    transform: translateY(-50%);
  }

  .user-skills-no-data-icon-subtext {
    font-size: 15px;
    font-size: 0.9em;
    color: grey;
    display: block;
  }
</style>
