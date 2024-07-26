/*
Copyright 2024 SkillTree

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
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useSkillsDisplayPointHistoryState } from '@/skills-display/stores/UseSkillsDisplayPointHistoryState.js'
import PointProgressHelper from '@/skills-display/components/progress/points/PointProgressHelper.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useRoute } from 'vue-router'
import PointHistoryChartPlaceholder from '@/skills-display/components/progress/points/PointHistoryChartPlaceholder.vue'
import ChartOverlayMsg from '@/skills-display/components/utilities/ChartOverlayMsg.vue'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

const pointHistoryState = useSkillsDisplayPointHistoryState()
const numFormat = useNumberFormat()
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()
const route = useRoute()

const chartSeries = ref([])
const loading = ref(true)
const animationEnded = ref(false)

const pointHistoryChart = () => {
  const chartsModule = themeState.theme?.charts?.pointHistory
  return {
    lineColor: chartsModule?.lineColor ? chartsModule.lineColor : themeState.colors.info,
    gradientStartColor: chartsModule?.gradientStartColor ? [chartsModule.gradientStartColor] : [themeState.colors.pointHistoryGradientStartColor],
    gradientStopColor: chartsModule?.gradientStopColor ? [chartsModule.gradientStopColor] : [themeState.colors.white]
  }
}
const lineColor = (returnArr = true) => {
  const color = pointHistoryChart().lineColor
  return returnArr ? [color] : color
}

const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }

  return themeHelper.isDarkTheme ? 'white' : undefined
}

const chartOptions = {
  chart: {
    type: 'area',
    toolbar: {
      offsetY: -37
    }
  },
  dataLabels: {
    enabled: false
  },
  markers: {
    size: 0,
    style: 'hollow'
  },
  xaxis: {
    type: 'datetime',
    tickAmount: 1,
    labels: {
      style: {
        colors: chartAxisColor()
      }
    }
  },
  yaxis: {
    min: function calculateMin(min) {
      return min < 100 ? 0 : min
    },
    forceNiceScale: true,
    labels: {
      style: {
        colors: [chartAxisColor()]
      },
      formatter: function format(val) {
        return numFormat.pretty(val)
      }
    }
  },
  fill: {
    colors: pointHistoryChart().gradientStartColor,
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.7,
      opacityTo: 0.9,
      stops: [0, 100],
      gradientToColors: pointHistoryChart().gradientStopColor
    }
  },
  stroke: {
    colors: lineColor()
  }
}

onMounted(() => {
  loadPointsHistory()
})
const loadPointsHistory = () => {
  pointHistoryState.loadPointHistory(route.params.subjectId)
    .then(() => {
      const pointHistoryRes = pointHistoryState.getPointHistory(route.params.subjectId)
      const seriesData = pointHistoryRes.pointsHistory?.map((value) => ({
        x: new Date(value.dayPerformed).getTime(),
        y: value.points
      }))
      chartSeries.value = [{
        data: seriesData,
        name: 'Points'
      }]
      chartOptions.xaxis.max = PointProgressHelper.calculateXAxisMaxTimestamp(pointHistoryRes)
      if (chartOptions.xaxis.max) {
        chartWasZoomed.value = true
      }
      let lastDay = -1
      let firstDay = -1
      if (seriesData && seriesData.length > 0) {
        lastDay = seriesData[seriesData.length - 1].x
        if (chartOptions.xaxis.max) {
          lastDay = chartOptions.xaxis.max
        }
        firstDay = seriesData[0].x
      }
      if (pointHistoryRes.achievements && pointHistoryRes.achievements.length > 0) {
        const labelColors = chartLabels()
        const annotationPoints = pointHistoryRes.achievements.map((item) => {
          const timestamp = new Date(item.achievedOn).getTime()
          return {
            x: timestamp,
            y: item.points,
            marker: {
              size: 8,
              fillColor: '#fff',
              strokeColor: lineColor(false),
              radius: 2,
              cssClass: 'apexcharts-custom-class'
            },
            label: {
              borderColor: labelColors.borderColor,
              offsetX: getOffsetX(item.name, firstDay, lastDay, timestamp),
              offsetY: 2,
              style: {
                color: labelColors.foregroundColor,
                background: labelColors.backgroundColor
              },
              text: item.name
            }
          }
        })
        chartOptions.annotations = {
          points: annotationPoints
        }
      }
      loading.value = false
    })
}

const chartLabels = () => {
  const chartsModule = themeState.theme?.charts
  return {
    borderColor: chartsModule?.labelBorderColor ? chartsModule.labelBorderColor : themeState.colors.primary,
    backgroundColor: chartsModule?.labelBackgroundColor ? chartsModule.labelBackgroundColor : themeState.colors.success,
    foregroundColor: chartsModule?.labelForegroundColor ? chartsModule.labelForegroundColor : themeState.colors.white
  }
}
const getOffsetX = (name, firstDay, lastDay, timestamp) => {
  if (firstDay === lastDay) {
    return 0
  }
  if (firstDay === timestamp) {
    return getLeftOffset(name)
  }
  if (lastDay === timestamp) {
    return getRightOffset(name)
  }
  return 0
}

const getRightOffset = (name) => {
  return name.length > 7 ? -30 : -20
}
const getLeftOffset = (name) => {
  return -1 * getRightOffset(name)
}

const hasData = computed(() => {
  return chartSeries.value && chartSeries.value.length > 0 && chartSeries.value[0].data && chartSeries.value[0].data.length > 0
})

const chartWasZoomed = ref(false)
const zoomedInfo = ref({})
const ptChart = ref(null)
const resetZoom = () => {
  chartWasZoomed.value = false
  ptChart.value.updateOptions({
    xaxis: {
      max: undefined,
      min: undefined,
    },
  });
}
const zoomed = (chartContext, { xaxis, yaxis }) => {
  if (xaxis.min === undefined && xaxis.max === undefined) {
    chartWasZoomed.value = false;
  } else {
    chartWasZoomed.value = true;
  }
  zoomedInfo.value = {
    x: xaxis, y: yaxis,
  };
}
</script>

<template>
  <Card class="h-full"
        :pt="{ content: { class: 'pt-2 pb-0' } }"
        data-cy="pointHistoryChart">
    <template #subtitle>
      <div class="flex">
        <div>
          Point History
          <SkillsButton
            v-if="chartWasZoomed"
            @click="resetZoom"
            icon="fas fa-search-minus"
            label="Reset Zoom"
            outlined
            size="small"
            class="absolute ml-2"
            data-cy="pointProgressChart-resetZoomBtn"
          />
        </div>
      </div>
    </template>
    <template #content>
      <div class="text-center">
        <div class="flex align-content-center justify-content-center">
          <skills-spinner
            v-if="loading"
            :is-loading="loading"
            line-bg-color="#333" line-fg-color="#17a2b8" size="small"
            message="Loading Chart ..." />
        </div>

        <div v-if="!loading">
          <div v-if="!hasData" class="relative" data-cy="pointHistoryChartNoData">
            <BlockUI :blocked="true" :auto-z-index="false">
              <point-history-chart-placeholder v-if="!hasData" />
            </BlockUI>
            <chart-overlay-msg  style="top: 4rem;">
              <div class="uppercase p-error"><i class="fa fa-lock"></i> Locked
              </div>
              <small>*** <b>2 days</b> of usage will unlock this chart!
                ***</small>
            </chart-overlay-msg>
          </div>
          <div v-if="hasData" data-cy="pointHistoryChartWithData">
            <apexchart ref="ptChart" id="points-chart"
                       :options="chartOptions"
                       @animationEnd="animationEnded = true"
                       @zoomed="zoomed"
                       :series="chartSeries"
                       height="200" type="area" />
            <span v-if="animationEnded" data-cy="pointHistoryChart-animationEnded"></span>

          </div>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>