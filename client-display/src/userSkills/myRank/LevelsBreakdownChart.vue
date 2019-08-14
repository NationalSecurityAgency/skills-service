<template>
  <div :class="{'disabled': showOverlay}" class="card level-breakdown-container h-100">

    <div v-if="showOverlay" class="disabled-overlay" />
    <div v-if="showOverlay" class="overlay-msg">
      <div  class="row justify-content-center">
        <div class="col-5 text-center border rounded bg-light p-2">
          <vue-simple-spinner v-if="loading" line-bg-color="#333" line-fg-color="#17a2b8" message="Loading Chart ..."/>
          <div v-else>
            No one achieved <span class="text-info">Level 1</span> yet... You could be the <u>first one</u>!
          </div>
        </div>
      </div>
    </div>

    <div class="card-header">
      <h6 class="card-title mb-0 float-left">Level Breakdown</h6>
    </div>
    <div class="card-body m-0 p-0 mr-1 mt-1">
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
          data: [{ x: 'Level 1', y: 0 }, { x: 'Level 2', y: 0 }, { x: 'Level 3', y: 0 }, { x: 'Level 4', y: 0 }, { x: 'Level 5', y: 0 }],
        }],
        chartOptions: {
          annotations: {
            // points: [],
            points: [{
              x: 'Level 0',
              seriesIndex: 0,
              label: {
                borderColor: '#775DD0',
                offsetY: 0,
                style: {
                  color: '#fff',
                  background: '#775DD0',
                },
                text: 'You are Level 0!',
              },
            }],
          },
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
      showOverlay() {
        return this.loadingLogic() || !this.hasData();
      },
      loading() {
        return this.loadingLogic();
      },
    },
    methods: {
      loadingLogic() {
        return this.usersPerLevel === null;
      },
      hasData() {
        const foundMoreThan0 = this.chartSeries[0].data.find(item => item.y > 0);
        return foundMoreThan0;
      },
      computeChartSeries() {
        const series = [{
          name: '# of Users',
          data: [],
        }];
        if (this.usersPerLevel) {
          this.usersPerLevel.forEach((level) => {
              const datum = { x: `Level ${level.level}`, y: level.numUsers };
              series[0].data.push(datum);
              if (level.level === this.myLevel) {
                // const label = {
                //   x: datum.x,
                //   text: `You are ${datum.x}!`,
                // };
                // this.chartOptions.annotations.points = [label];
                this.chartOptions.annotations.points[0].x = datum.x;
                this.chartOptions.annotations.points[0].label.text = `You are ${datum.x}!`;
              }
            });
        }
        this.chartOptions = { ...this.chartOptions }; // Trigger reactivity
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

  .overlay-msg {
    font-weight: 700;
    opacity: 0.8;
    position: absolute;
    left: 0;
    top: 50%;
    z-index: 1000;
    text-align: center;
    width: 100%;
    transform: translateY(-50%);
  }
</style>
