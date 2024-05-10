<script setup>
import { useNumberFormat } from '../../../../../common-components/src/common/filter/UseNumberFormat.js'
import { computed, watch, ref } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'

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
  }
})

const numFormat = useNumberFormat()
const themeState = useSkillsDisplayThemeState()

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

const defaultColor = '#0ea5e9'
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

const chartOptions = computed(() => {
  return {
    chart: {
      height: 250,
      type: 'radialBar'
    },
    fill: {
      colors: [completedColor]
    },
    plotOptions: {
      radialBar: {
        hollow: {
          size: '67%'
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
    labels: [isCompleted ? '✓' : `${numFormat.pretty(props.totalCompletedPoints > 0 ? props.totalCompletedPoints : 0)} Points`]
  }
})


</script>

<template>
  <div class="progress-circle-wrapper">
    <label class="text-2xl font-medium">{{ title }}</label>
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