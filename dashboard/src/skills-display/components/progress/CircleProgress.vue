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

const props = defineProps({
  diameter: {
    type: Number,
    default: 160
  },
  title: {
    type: String
  },
  completedBeforeTodayColor: {
    type: String,
    default: '#14a3d2'
  },
  totalCompletedColor: {
    type: String,
    default: '#7ed6f3'
  },
  incompleteColor: {
    type: String,
    default: '#cdcdcd'
  },
  strokeWidth: {
    type: Number,
    default: 12
  },
  pointsCompletedToday: {
    type: Number
  },
  totalCompletedPoints: {
    type: Number
  },
  totalPossiblePoints: {
    type: Number
  },
  customLabel: {
    type: String,
    default: 'Point',
  }
})

const numFormat = useNumberFormat()
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()

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
const is100Percent = percentComplete.value === 100

const series = computed(() => [percentComplete.value])

const defaultColor = themeHelper.isDarkTheme ? 'white' : '#067085'
const completedColor = '#22C55E'
const dataLabelNameColor = computed(() => {
  if (themeState.circleProgressInteriorTextColor) {
    return  themeState.circleProgressInteriorTextColor
  }
  if (themeState.textPrimaryColor) {
    return themeState.textPrimaryColor
  }
  return isCompleted ? completedColor : defaultColor
})

const barColor = computed(() => {
  if (themeState.theme?.progressIndicators?.beforeTodayColor) {
    return themeState.theme?.progressIndicators?.beforeTodayColor
  }
  return defaultColor
})

const backgroundBarColor = computed(() => {
  if (themeState.theme?.progressIndicators?.incompleteColor) {
    return themeState.theme?.progressIndicators?.incompleteColor
  }
  return '#f3f4f6'
})


const chartOptions = computed(() => {
  return {
    chart: {
      height: 250,
      type: 'radialBar'
    },
    fill: {
      colors: [barColor.value]
    },
    plotOptions: {
      radialBar: {
        hollow: {
          size: '67%'
        },
        track: {
          background: backgroundBarColor.value
        },
        dataLabels: {
          name: {
            show: true,
            fontSize: isCompleted ? '3.5rem' : '1.2rem',
            color: dataLabelNameColor.value
          },
          value: {
            show: true,
            fontSize: is100Percent || isCompleted ? '1.2rem' : '1rem',
            color:  dataLabelNameColor.value // is100Percent || isCompleted ? completedColor : defaultColor
          }
        }
      }
    },
    labels: [isCompleted ? '✓' : `${numFormat.pretty(props.totalCompletedPoints > 0 ? props.totalCompletedPoints : 0)} ${props.customLabel}s`]
  }
})


</script>

<template>
  <div class="progress-circle-wrapper">
    <div class="text-2xl font-medium">{{ title }}</div>
    <div>
      <apexchart type="radialBar" height="250" :options="chartOptions" :series="series"></apexchart>
    </div>
    <div>
      <slot name="footer" />
    </div>
  </div>
</template>

<style scoped>

</style>