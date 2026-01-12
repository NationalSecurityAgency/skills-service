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
import {computed, onMounted, ref, watch} from 'vue'
import {useSkillsDisplayPointHistoryState} from '@/skills-display/stores/UseSkillsDisplayPointHistoryState.js'
import {useSkillsDisplayThemeState} from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import {useRoute} from 'vue-router'
import {useSkillsDisplayAttributesState} from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import ChartDataLabels from "chartjs-plugin-datalabels";
import Chart from "primevue/chart";
import dayjs from "dayjs";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const appConfig = useAppConfig()
const pointHistoryState = useSkillsDisplayPointHistoryState()
const themeState = useSkillsDisplayThemeState()
const route = useRoute()
const attributes = useSkillsDisplayAttributesState()
const chartSupportColors = useChartSupportColors()
const colors = chartSupportColors.getColors()

const loading = ref(true)
const animationEnded = ref(false)
const chartLoaded = ref(false)

const chartData = ref({});
const achievedLevels = ref([])
const splashChartData = ({
  datasets: [{
    label: 'Points',
    data: Array.from({ length: 6 }, (_, i) => ({
      x: dayjs().subtract(6 - i, 'day').toDate(),
      y: [10, 150, 150, 400, 500, 1200][i]
    })),
    cubicInterpolationMode: 'monotone',
    borderColor: colors.surface300Color,
    pointRadius: 0,
    pointHoverRadius: 0,
    borderWidth: 4, // Make the line slightly thicker for better visibility
  }]
})


onMounted(() => {
  pointHistoryState.loadPointHistory(route.params.subjectId)
      .then(() => {
        setupData()
      })
})
const formatTimestamp = (timestamp) => dayjs(timestamp).format('YYYY-MM-DD')


function combineAchievements(achievements) {
  if (achievements.length === 1) {
    return achievements[0];
  }

  // Sort to ensure consistent ordering
  const sorted = [...achievements].sort((a, b) => a.dataIndex - b.dataIndex);

  // Extract and sort level numbers
  const levelNumbers = sorted
      .map(a => {
        const match = a.name.match(/^Levels?\s+([\d,\s]+)$/i);
        if (match) {
          return match[1].split(/\s*,\s*/).map(num => parseInt(num, 10));
        }
        return null;
      })
      .flat()
      .filter(Boolean)
      .filter((value, index, self) => self.indexOf(value) === index) // Remove duplicates
      .sort((a, b) => a - b);

  // Format as "Level 1, 2, 3"
  const combinedName = `Level ${levelNumbers.join(', ')}`;

  // Use the first achievement as the base and update its properties
  return {
    ...sorted[0],
    name: combinedName,
    // Use the latest dataIndex to position the combined label
    dataIndex: sorted[sorted.length - 1].dataIndex,
    day: sorted[sorted.length - 1].day
  };
}

const combineOverlappingAchievements = (achievements, numItems) => {
  // At least 1 item apart
  const minDistance = Math.max(1, Math.floor(numItems * appConfig.sdPointHistoryChartAchievementsCombinePct));
  const achievementsWorkCopy = [...achievements]
  achievementsWorkCopy.sort((a, b) => a.dataIndex - b.dataIndex);

  const combined = [];
  let currentGroup = [achievementsWorkCopy[0]];

  for (let i = 1; i < achievementsWorkCopy.length; i++) {
    const prev = currentGroup[currentGroup.length - 1];
    const current = achievementsWorkCopy[i];

    const distance = current.dataIndex - prev.dataIndex
    if (distance <= minDistance) {
      // Add to current group if too close to previous
      currentGroup.push(current);
    } else {
      // Push the combined group and start a new one
      combined.push(combineAchievements(currentGroup));
      currentGroup = [current];
    }
  }

  // Add the last group
  if (currentGroup.length > 0) {
    combined.push(combineAchievements(currentGroup));
  }
  return combined
}

const setupData = () => {
  const pointHistoryRes = pointHistoryState.getPointHistory(route.params.subjectId)
  const numItems = pointHistoryRes.pointsHistory?.length || 0

  if (pointHistoryRes.achievements) {
    const achievedLevelsTmp = pointHistoryRes.achievements.map((achievement) => {
      const dataIndex = pointHistoryRes.pointsHistory.findIndex((item) => item.dayPerformed === achievement.achievedOn)
      return {
        ...achievement,
        dataIndex: dataIndex >= 0 ? dataIndex + 1 : null,
        day: formatTimestamp(achievement.achievedOn)
      }
    })

    // Combine achievements that are too close to each other (within 5% of total items)
    const considerForOverlapMerge = numItems > 10 && achievedLevelsTmp.length > 1
    achievedLevels.value = considerForOverlapMerge ? combineOverlappingAchievements(achievedLevelsTmp, numItems) : achievedLevelsTmp
  }

  const pointHistory = pointHistoryRes.pointsHistory
  if (pointHistory && pointHistory.length > 0) {
    // find the earliest date, then add a new entry for a previous date with 0 points
    const earliestDate = pointHistory[0].dayPerformed
    const prevDay = dayjs(earliestDate).subtract(1, 'day').toDate()
    pointHistory.unshift({
      dayPerformed: formatTimestamp(prevDay),
      points: 0
    })
  }

  const pointRadius = numItems < tooManyPointsForTooltip ? 3 : 0
  chartData.value = {
    datasets: [{
      label: 'Points',
      data: pointHistoryRes.pointsHistory.map((item) => {
        return {x: formatTimestamp(item.dayPerformed), y: item.points}
      }),
      cubicInterpolationMode: 'monotone',
      pointRadius: pointRadius,
      pointHoverRadius: pointRadius,
      borderWidth: 4, // Make the line slightly thicker for better visibility
    }]
  }

  loading.value = false
}

const hasData = computed(() => {
  const res = chartData.value?.datasets && chartData.value?.datasets.length > 0 && (chartData.value?.datasets[0].data?.length > 0)
  return res !== undefined && res !== false && res !== null
})
const tooManyPointsForTooltip = 50
const hasManyPoints = computed(() => {
  if (!hasData.value) {
    return false
  }
  const data = chartData.value?.datasets[0].data
  return data.length > tooManyPointsForTooltip
})

const chartJsOptions = computed(() => {
  const axisLabelColor = themeState.theme.charts.axisLabelColor || colors.textMutedColor
  const dataLabelBackgroundColor = themeState.theme.charts.labelBackgroundColor || colors.surface100Color
  const dataLabelBorderColor = themeState.theme.charts.labelBorderColor || colors.surface600Color
  const dataLabelForegroundColor = themeState.theme.charts.labelForegroundColor || colors.cyan700Color
  const surfaceBorder =  colors.contentBorderColor

  return {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        type: 'time',
        time: {
          unit: 'day',
          displayFormats: {
            day: 'MMM D, YYYY'  // Format to show month, day, and year
          },
        },
        ticks: {
          color: axisLabelColor
        },
        grid: {
          color: surfaceBorder,
          drawOnChartArea: false  // Ensures no grid lines are drawn in the chart area
        }
      },
      y: {
        beginAtZero: true,
        ticks: {
          color: axisLabelColor
        },
        grid: {
          color: surfaceBorder
        },
        title: {
          display: false,
        }
      }
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        enabled: !hasManyPoints.value,
        callbacks: {
          title: function (context) {
            return dayjs(context[0].parsed.x).format('MMM D, YYYY')
          },
        }
      },
      datalabels: {
        color: dataLabelForegroundColor,
        font: {
          size: 10,
        },
        clamp: true,
        align: function (context) {
          // Find all achievements before the current dataIndex
          const previousAchievements = achievedLevels.value.filter(item =>
              item.dataIndex < context.dataIndex
          )
          const totalNumItems = context.dataset.data.length
          const isAtTheEnd = context.dataIndex >= (totalNumItems - 2)
          if (previousAchievements && previousAchievements.length > 0) {
            const previousLabelIndex = previousAchievements[previousAchievements.length - 1].dataIndex
            if (previousLabelIndex && (context.dataIndex - previousLabelIndex) < 10) {
              if (isAtTheEnd) {
                return 'left'
              }
              return 'top'
            }
          }
          return 'left'
        },
        backgroundColor: dataLabelBackgroundColor,
        borderColor: dataLabelBorderColor,
        borderWidth: 1,
        borderRadius: 4,
        padding: 5,
        formatter: function(value, context) {
          const achievements = achievedLevels.value.find((item) => item.dataIndex === context.dataIndex)
          if (!achievements) {
            return null
          }
          return `${achievements.name}`
        }
      }
    },
    animation: {
      onComplete: function() {
        animationEnded.value = true;
      }
    },
  };
})

const pointsChartRef = ref(null)

watch(() => pointHistoryState.pointHistoryDateLoadedMap.get(route.params.subjectId), (newVal, oldVal) => {
  if (oldVal && newVal) {
    setupData()
  }
})
</script>

<template>
  <Card class="h-full"
        :pt="{
           body: { class: '!pt-3' },
           content: { class: '!pt-0 !pb-0' },
        }"
        data-cy="pointHistoryChart">
    <template #title>
      <div class="flex">
        <div class="flex-1">
          {{ attributes.pointDisplayName }} History
        </div>
        <chart-download-controls v-if="hasData" :vue-chart-ref="pointsChartRef" />
      </div>
    </template>
    <template #content>
      <metrics-overlay :loading="loading"
                       :has-data="hasData"
                       :data-cy="`${hasData ? 'pointHistoryChartWithData' : 'pointHistoryChartNoData'}`"
                       no-data-msg="This chart needs at least 2 days of user activity.">
        <Chart ref="pointsChartRef"
               type="line"
               :data="hasData ? chartData : splashChartData"
               :options="chartJsOptions"
               :plugins="[ChartDataLabels]"
               @loaded="chartLoaded = true"
               class="h-[14rem]"/>
        <template #no-data>
          <Card>
            <template #content>
              <div class="text-blue-800 dark:text-blue-200 sd-theme-primary-color"><i class="fas fa-chart-line"></i>
                Your Progress Awaits!
              </div>
              <small class="text-green-900 dark:text-green-100 sd-theme-primary-color">Your progress journey begins with your first {{ attributes.pointDisplayNameLower }}!</small>
            </template>
          </Card>
        </template>
        <span v-if="!loading && animationEnded && chartLoaded" data-cy="pointHistoryChart-animationEnded"></span>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>
