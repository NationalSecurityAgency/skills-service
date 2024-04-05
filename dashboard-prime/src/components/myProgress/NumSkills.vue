<script setup>
import MyProgressInfoCardUtil from '@/components/myProgress/MyProgressInfoCardUtil.vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import { computed } from 'vue'

const myProgressState = useMyProgressState()
const myProgress = computed(() => myProgressState.myProgress)

const series = [{ data: [10, 41, 35, 51, 49, 62, 69, 91, 148] }]
const chartOptions = {
  chart: {
    height: 135,
    type: 'line',
    zoom: {
      enabled: false
    },
    toolbar: {
      show: false
    }
  },
  dataLabels: {
    enabled: false
  },
  stroke: {
    curve: 'straight'
  },
  grid: {
    padding: {
      top: -10,
      bottom: -15
    },
    row: {
      colors: ['transparent', 'transparent'], // takes an array which will be repeated on columns
      opacity: 0.5
    }
  },
  xaxis: {
    labels: {
      show: false
    }
  },
  yaxis: {
    labels: {
      show: false
    }
  },
  tooltip: {
    enabled: false
  }
}
</script>

<template>
  <my-progress-info-card-util title="Skills">
    <template #left-content>
      <div>
        <div class="text-4xl text-orange-500" data-cy="numAchievedSkills">{{ myProgress.numAchievedSkills }}</div>
        <div class="w-5rem">
          <Tag severity="info" data-cy="numSkillsAvailable">Total: {{ myProgress.totalSkills }}</Tag>
        </div>
      </div>
    </template>
    <template #right-content>
      <apexchart type="line" height="140" :options="chartOptions" :series="series"></apexchart>
    </template>
    <template #footer>
      <span data-cy="num-skills-footer" class="line-height-4">So many skills... so little time! Good luck!</span>
    </template>
  </my-progress-info-card-util>
</template>

<style scoped>

</style>