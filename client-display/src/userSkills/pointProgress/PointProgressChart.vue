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
  <div :class="{'disabled': loading.inProgress}" class="card position-relative" data-cy="pointHistoryChart">
    <div v-if="!hasData" class="disabled-overlay" />
    <div v-if="!hasData || loading.inProgress" class="text-center user-skills-no-data-icon-text text-danger">
        <div class="row justify-content-center">
          <div class="col-5 text-center border rounded bg-light p-2">
            <vue-simple-spinner v-if="loading.inProgress" line-bg-color="#333" line-fg-color="#17a2b8" size="small" message="Loading Chart ..."/>
            <div v-else>
              <div style="font-size: 1rem;" class="text-uppercase"><i class="fa fa-lock"></i> Locked</div>
              <small class="text-black-50">*** <b>2 days</b> of usage will unlock this chart! ***</small>
            </div>
          </div>
        </div>
    </div>
    <div class="card-header">
      <h6 class="card-title mb-0 float-left">Point History</h6>
    </div>
    <div class="card-body m-0 mr-1 p-0 apex-chart-container">
      <apexchart ref="ptChart" id="points-chart" v-if="!loading.inProgress && hasData" :options="chartOptions"
                 :series="chartSeries" height="200" type="area" />
      <point-history-chart-placeholder v-if="loading.inProgress || !hasData" />
    </div>
    <button v-if="chartWasZoomed" @click="resetZoom"
            class="reset-zoom-btn btn btn-outline-primary btn-sm"><i class="fas fa-search-minus"></i> Reset Zoom</button>
  </div>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import VueApexCharts from 'vue-apexcharts';
  import Spinner from 'vue-simple-spinner';
  import PointProgressHelper from '@/userSkills/pointProgress/PointProgressHelper';
  import PointHistoryChartPlaceholder from '@/userSkills/pointProgress/PointHistoryChartPlaceholder';
  import numberFormatter from '../../common/filter/NumberFilter';

  export default {
    components: {
      PointHistoryChartPlaceholder,
      apexchart: VueApexCharts,
      'vue-simple-spinner': Spinner,
    },
    data() {
      const self = this;
      return {
        chartWasZoomed: false,
        chartOptions: {
          chart: {
            type: 'area',
            toolbar: {
              offsetY: -30,
            },
            events: {
              zoomed(chartContext, { xaxis, yaxis }) {
                if (xaxis.min === undefined && xaxis.max === undefined) {
                  self.chartWasZoomed = false;
                } else {
                  self.chartWasZoomed = true;
                }
                this.zoomedInfo = {
                  x: xaxis, y: yaxis,
                };
              },
            },
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
          yaxis: {
            min: function calculateMin(min) {
              return min < 100 ? 0 : min;
            },
            forceNiceScale: true,
            labels: {
              style: {
                colors: [this.$store.state.themeModule.charts.axisLabelColor],
              },
              formatter: function format(val) {
                return numberFormatter(val);
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
        chartSeries: [],
        loading: {
          inProgress: true,
        },
      };
    },
    mounted() {
      this.loadPointsHistory();
    },
    computed: {
      hasData() {
        return this.chartSeries && this.chartSeries.length > 0 && this.chartSeries[0].data && this.chartSeries[0].data.length > 0;
      },
    },
    methods: {
      loadPointsHistory() {
        UserSkillsService.getPointsHistory(this.$route.params.subjectId)
          .then((result) => {
            const seriesData = result.pointsHistory.map((value) => ({
              x: new Date(value.dayPerformed).getTime(),
              y: value.points,
            }));
            this.chartSeries = [{
              data: seriesData,
              name: 'Points',
            }];
            this.chartOptions.xaxis.max = PointProgressHelper.calculateXAxisMaxTimestamp(result);
            if (this.chartOptions.xaxis.max) {
              this.chartWasZoomed = true;
            }

            let lastDay = -1;
            let firstDay = -1;
            if (seriesData && seriesData.length > 0) {
              lastDay = seriesData[seriesData.length - 1].x;
              if (this.chartOptions.xaxis.max) {
                lastDay = this.chartOptions.xaxis.max;
              }
              firstDay = seriesData[0].x;
            }
            if (result.achievements && result.achievements.length > 0) {
              const annotationPoints = result.achievements.map((item) => {
                const timestamp = new Date(item.achievedOn).getTime();
                return {
                  x: timestamp,
                  y: item.points,
                  marker: {
                    size: 8,
                    fillColor: '#fff',
                    strokeColor: 'rgb(68, 114, 186)',
                    radius: 2,
                    cssClass: 'apexcharts-custom-class',
                  },
                  label: {
                    borderColor: 'rgb(68, 114, 186)',
                    offsetX: this.getOffsetX(item.name, firstDay, lastDay, timestamp),
                    offsetY: 2,
                    style: {
                      color: '#fff',
                      background: 'rgb(89, 173, 82)',
                    },
                    text: item.name,
                  },
                };
              });
              this.chartOptions.annotations = {
                points: annotationPoints,
              };
            }
            this.loading.inProgress = false;
          });
      },
      getOffsetX(name, firstDay, lastDay, timestamp) {
        if (firstDay === lastDay) {
          return 0;
        }
        if (firstDay === timestamp) {
          return this.getLeftOffset(name);
        }
        if (lastDay === timestamp) {
          return this.getRightOffset(name);
        }
        return 0;
      },
      getRightOffset(name) {
        return name.length > 7 ? -30 : -20;
      },
      getLeftOffset(name) {
        return -1 * this.getRightOffset(name);
      },
      resetZoom() {
        this.chartWasZoomed = false;
        this.$refs.ptChart.updateOptions({
          xaxis: {
            max: undefined,
            min: undefined,
          },
        });
      },
    },
  };
</script>

<style>

</style>

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
    opacity: 0.8;
    position: absolute;
    left: 0;
    top: 50%;
    z-index: 1000;
    text-align: center;
    width: 100%;
    transform: translateY(-50%);
  }

  .reset-zoom-btn {
    position: absolute;
    bottom: 4rem;
    right: 1rem;
  }
</style>
