<script setup>
import { ref } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'

const themeState = useSkillsDisplayThemeState()

const animationEnded = ref(false)
const chartOptions = {
  chart: {
    type: 'area',
    toolbar: {
      show: false
    },
  },
  stroke: {
    colors: ['#dee2e6']
  },
  dataLabels: {
    enabled: false
  },
  xaxis: {
    type: 'datetime',
    tickAmount: 1,
    labels: {
      style: {
        colors: themeState.theme.charts.axisLabelColor
      }
    }
  }
}
const series = [{
  data: [
    [1553227200000, 0],
    [1553313600000, 50],
    [1553400000000, 450],
    [1553486400000, 475],
    [1553572800000, 475],
    [1553659200000, 475],
    [1553745600000, 800],
    [1553832000000, 1200],
    [1553918400000, 1250],
    [1554004800000, 1250],
    [1554091200000, 1500],
    [1554177600000, 2000],
    [1554264000000, 2100],
    [1554350400000, 2200]
  ],
  name: 'Points'
}]

const onAnimationEnd = () => {
  animationEnded.value = true
}
</script>

<template>
  <div>
    <apexchart :options="chartOptions"
               @animationEnd="onAnimationEnd"
               :series="series" height="200" type="area" />
    <span v-if="animationEnded" data-cy="pointHistoryChartPlaceholder-animationEnded"></span>
  </div>
</template>

<style scoped>

</style>