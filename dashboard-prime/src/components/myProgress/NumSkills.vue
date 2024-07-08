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
  <my-progress-info-card-util title="Skills" style="min-width: 20rem">
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
      <span data-cy="num-skills-footer" class="">So many skills... so little time! Good luck!</span>
    </template>
  </my-progress-info-card-util>
</template>

<style scoped>

</style>