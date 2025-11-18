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
import {onMounted, ref} from 'vue'
import {useSkillsDisplayThemeState} from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import {useNumberFormat} from '@/common-components/filter/UseNumberFormat.js'
import {useThemesHelper} from "@/components/header/UseThemesHelper.js";
import {useSkillsDisplayAttributesState} from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import {useSkillsDisplayService} from '@/skills-display/services/UseSkillsDisplayService.js'
import {useRoute} from 'vue-router'
import Chart from "primevue/chart";
import ChartDataLabels from 'chartjs-plugin-datalabels';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";

const props = defineProps({
  myLevel: Number
})
const skillsDisplayService = useSkillsDisplayService()
const route = useRoute()
const attributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()
const animationEnded = ref(false)
const numFormat = useNumberFormat()
const chartSupportColors = useChartSupportColors()


const usersPerLevelLoading = ref(true)
const usersPerLevel = ref({})
const hasData = ref(false)
const chartData = ref({})
const chartJsOptions = ref();

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  loadData()
})

const loadData = () => {
  return skillsDisplayService.getRankingDistributionUsersPerLevel(route.params.subjectId)
    .then((response) => {
      usersPerLevel.value = response
      if (usersPerLevel.value) {
        computeChartData()
      }
    })
    .finally(() => {
      usersPerLevelLoading.value = false
    })
}

const computeChartData = () => {
  chartData.value = {
    labels: usersPerLevel.value.map((item) => `${attributes.levelDisplayName} ${item.level}`),
    datasets: [{
      label: '# of Users',
      data: usersPerLevel.value.map((item) => item.numUsers),
      backgroundColor: chartSupportColors.getBackgroundColorArray(usersPerLevel.value.length),
      borderColor: chartSupportColors.getBorderColorArray(usersPerLevel.value.length),
      borderWidth: 1,
      borderRadius: 6,
      maxBarThickness: 50,
      minBarLength: 2,
    }]
  }
  hasData.value = usersPerLevel.value.find((item) => item.numUsers > 0) !== undefined
}

const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  const labelColor = themeState.theme.charts.axisLabelColor || colors.textMutedColor
  return {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        ticks: {
          color: labelColor
        },
        grid: {
          display: false,  // This will hide the vertical grid lines
        }
      },
      y: {
        display: true,
        beginAtZero: true,
        ticks: {
          color: labelColor,
          stepSize: 1,  // This ensures the step size is 1
          precision: 0   // Ensures whole numbers
        },
        grid: {
          color: colors.contentBorderColor
        },
        title: {
          display: true,
          text: '# of Users',
          color: labelColor
        },
      }
    },
    plugins: {
      legend: {
        display: false
      },
      datalabels: {
        color: colors.cyan700Color,
        font: {
          size: 12,
        },
        backgroundColor: colors.surface100Color,
        borderColor: colors.surface600Color,
        borderWidth: 1,
        borderRadius: 4,
        padding: 5,
        formatter: function(value, context) {
          const currentLevel = context.dataIndex + 1
          if (props.myLevel !== currentLevel) {
            return null
          }
          return `You are ${attributes.levelDisplayName} ${props.myLevel}!`
        }
      }
    }
  };
}
const levelBreakdownChartRef = ref(null)
</script>

<template>
  <Card data-cy="levelBreakdownChart" :pt="{ content: { class: 'mb-0! pb-0!'}}" class="w-min-15rem h-full">
    <template #subtitle>
      <div class="flex">
        <h2 class="flex-1">{{ attributes.levelDisplayName }} Breakdown</h2>
        <chart-download-controls v-if="hasData" :vue-chart-ref="levelBreakdownChartRef" />
      </div>
    </template>
    <template #content>
      <metrics-overlay :loading="usersPerLevelLoading" :has-data="hasData"
                       no-data-msg="No achievements yet...">
        <Chart ref="levelBreakdownChartRef"
               type="bar"
               :data="chartData"
               :options="chartJsOptions"
               :plugins="[ChartDataLabels]"
               class="h-[18rem]" />
        <template #no-data>
          <InlineMessage>No one achieved
          <Tag>{{ attributes.levelDisplayName }} 1</Tag>
            yet... You could be the <i><strong>first one</strong></i>!</InlineMessage>
        </template>
      </metrics-overlay>
      <span v-if="animationEnded" data-cy="levelBreakdownChart-animationEnded"></span>
    </template>
  </Card>
</template>

<style scoped>

</style>