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
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { computed, watch, ref } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import {useThemesHelper} from "@/components/header/UseThemesHelper.js";
import RadialPercentageChart from "@/components/utils/charts/RadialPercentageChart.vue";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";

const props = defineProps({
  diameter: {
    type: Number,
    default: 160
  },
  title: {
    type: String
  },
  totalCompletedPoints: {
    type: Number
  },
  totalPossiblePoints: {
    type: Number
  },
})

const numFormat = useNumberFormat()
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()
const chartSupportColors = useChartSupportColors()
const attributes = useSkillsDisplayAttributesState()

const chartColors = chartSupportColors.getColors()

// If totalPossiblePoints is -1 it means this is charting Level progress and the user has completed this level
const isCompleted = props.totalPossiblePoints === -1
const percentComplete = computed(() => {
  if (isCompleted) {
    return 100
  }
  if (props.totalPossiblePoints > 0 && props.totalCompletedPoints > 0) {
    return Math.trunc((props.totalCompletedPoints / props.totalPossiblePoints) * 100)
  }
  return 0
})
const is100Percent = computed(() => percentComplete.value === 100)

const defaultColor = themeHelper.isDarkTheme ? 'white' : chartColors.cyan700Color
const completedColor = chartColors.green700Color
const dataLabelNameColor = computed(() => {
  if (themeState.circleProgressInteriorTextColor) {
    return themeState.circleProgressInteriorTextColor
  }
  if (themeState.textPrimaryColor) {
    return themeState.textPrimaryColor
  }
  return isCompleted ? completedColor : defaultColor
})

const barColor = computed(() => themeState.theme?.progressIndicators?.beforeTodayColor || chartColors.cyan700Color)
const remainingBarColor = computed(() => themeState.theme?.progressIndicators?.incompleteColor || chartColors.surface300Color)

const radialChartLabelStyle = computed(() => {
  if (dataLabelNameColor.value) {
    return  { color: dataLabelNameColor.value }
  }
  return {}
})
</script>

<template>
  <div class="progress-circle-wrapper flex flex-col gap-3">
    <h2 class="text-2xl font-medium">{{ title }}</h2>
    <div>
      <radial-percentage-chart
          :value="is100Percent ? 100 :  totalCompletedPoints"
          :max="is100Percent ? 100 : totalPossiblePoints"
          :full-circle="true"
          :size-in-rem="12"
          :completed-bar-color="barColor"
          :remaining-bar-color="remainingBarColor"
      >
        <template #center>
          <div v-if="is100Percent" data-cy="circleProgressCompleted">
            <i class="fa-solid fa-check-double text-green-500 text-4xl" aria-hidden="true"></i>
          </div>
          <div v-else>
            <div class="text-xl font-semibold" :style="radialChartLabelStyle"><span data-cy="circleProgressPts">{{ totalCompletedPoints }}</span> {{ attributes.pointDisplayNamePlural }}</div>
          </div>
          <div class="text-gray-500 dark:text-gray-200" :style="radialChartLabelStyle" data-cy="circleProgressPercent">{{  percentComplete }}%</div>
        </template>
      </radial-percentage-chart>
    </div>
    <div>
      <slot name="footer" />
    </div>
  </div>
</template>

<style scoped>

</style>