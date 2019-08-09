<template>
  <div class="card level-breakdown-container h-100">
    <div class="card-header">
      <h6 class="card-title mb-0 float-left">Level Breakdown</h6>
    </div>
    <div class="card-body m-0 p-0 mr-1 mt-1">
      <div v-if="loading" style="position: absolute; top:30%; width: 100%; z-index: 1000">
        <div  class="row justify-content-center">
          <div class="col-5 text-center border rounded bg-light p-2">
            <vue-simple-spinner line-bg-color="#333" line-fg-color="#17a2b8" message="Loading Chart ..."/>
          </div>
        </div>
      </div>
      <apexchart
        :options="chartOptions"
        :series="chartSeries"
        height="330" type="bar"/>
    </div>
  </div>
</template>

<script>
  import VueApexCharts from 'vue-apexcharts';
  import Spinner from 'vue-simple-spinner';

  export default {
    name: 'LevelsBreakdownChart',
    components: {
      apexchart: VueApexCharts,
      'vue-simple-spinner': Spinner,
    },
    props: {
      usersPerLevel: Array,
      myLevel: Number,
    },
    data() {
      return {
        chartSeries: [{
          name: '# of Users',
          data: [ { x: 'Level 1', y: 0 }, { x: 'Level 2', y: 0 }, { x: 'Level 3', y: 0 }, { x: 'Level 4', y: 0 }, { x: 'Level 5', y: 0 }],
        }],
        chartOptions: {
          // annotations: {
          //   points: [{
          //     x: 'Level 0',
          //     seriesIndex: 0,
          //     label: {
          //       borderColor: '#775DD0',
          //       offsetY: 0,
          //       style: {
          //         color: '#fff',
          //         background: '#775DD0',
          //       },
          //       text: 'You are Level 0!',
          //     },
          //   }],
          // },
          plotOptions: {
            bar: {
              columnWidth: '50%',
              endingShape: 'rounded',
            },
          },
          dataLabels: {
            enabled: false,
          },
          grid: {
            row: {
              colors: ['#fff', '#f2f2f2'],
            },
          },
          xaxis: {
            labels: {
              rotate: -45,
              style: {
                colors: this.$store.state.themeModule.charts.axisLabelColor,
              },
            },
          },
          yaxis: {
            title: {
              text: '# of Users',
              style: {
                color: this.$store.state.themeModule.charts.axisLabelColor,
              },
            },
            labels: {
              style: {
                color: this.$store.state.themeModule.charts.axisLabelColor,
              },
            },
          },
          fill: {
            type: 'gradient',
            gradient: {
              shade: 'dark',
              type: 'horizontal',
              shadeIntensity: 0.25,
              gradientToColors: undefined,
              inverseColors: true,
              opacityFrom: 0.85,
              opacityTo: 0.85,
              stops: [50, 0, 100],
            },
          },
        },
      };
    },
    // mounted() {
    //   this.computeRankingDistributionChartSeries();
    // },
    watch: {
      usersPerLevel() {
        this.computeChartSeries();
      },
    },
    computed: {
      loading() {
        return this.usersPerLevel === null;
      },
    },
    methods: {
      computeChartSeries() {
        const series = [{
          name: '# of Users',
          data: [],
        }];
        if (this.usersPerLevel) {
          this.usersPerLevel.forEach((level) => {
              const datum = { x: `Level ${level.level}`, y: level.numUsers };
              series[0].data.push(datum);
              // if (level.level === this.myLevel) {
              //   this.chartOptions.annotations.points[0].x = datum.x;
              //   this.chartOptions.annotations.points[0].text = `You are ${datum.x}!`;
              // }
            });
        }
        // this.chartOptions = { ...this.chartOptions }; // Trigger reactivity
        this.chartSeries = series;
      },
    },
  };
</script>

<style>
  .level-breakdown-container .apexcharts-menu-icon {
    position: relative !important;
    top: -2.3rem !important;
  }
</style>

<style scoped>

</style>
