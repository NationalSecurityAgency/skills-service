<script setup>
import { useNumberFormat } from '../../../../../common-components/src/common/filter/UseNumberFormat.js'

const props = defineProps({
  diameter: {
    type: Number,
    default: 160,
  },
  title: {
    type: String,
  },
  completedBeforeTodayColor: {
    type: String,
    default: '#14a3d2',
  },
  totalCompletedColor: {
    type: String,
    default: '#7ed6f3',
  },
  incompleteColor: {
    type: String,
    default: '#cdcdcd',
  },
  strokeWidth: {
    type: Number,
    default: 12,
  },
  pointsCompletedToday: {
    type: Number,
  },
  totalCompletedPoints: {
    type: Number,
  },
  totalPossiblePoints: {
    type: Number,
  },
})

const numFormat = useNumberFormat()

const percentComplete = props.totalPossiblePoints > 0 && props.totalCompletedPoints > 0 ?
  Math.trunc(props.totalCompletedPoints / props.totalPossiblePoints) : 0

const series = [percentComplete]
const chartOptions = {
  chart: {
    height: 250,
      type: 'radialBar',
  },
  plotOptions: {
    radialBar: {
      hollow: {
        size: '70%',
      },
      dataLabels: {
        name: {
          show: true,
          fontSize: '1.2rem'
        },
        value: {
          show: true,
          fontSize: '1rem'
        },
      }
    },
  },
  labels: [`${numFormat.pretty(props.totalCompletedPoints > 0 ? props.totalCompletedPoints : 0)} Points`],
}
</script>

<template>
  <div class="progress-circle-wrapper">
    <label class="text-2xl">{{ title }}</label>
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