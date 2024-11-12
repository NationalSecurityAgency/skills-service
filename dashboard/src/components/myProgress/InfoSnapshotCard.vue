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
import { computed } from 'vue'
import MyProgressInfoCardUtil from '@/components/myProgress/MyProgressInfoCardUtil.vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

const myProgressState = useMyProgressState()
const myProgress = computed(() => myProgressState.myProgress)
const myProjects = computed(() => myProgressState.myProjects)
const themeHelper = useThemesHelper()

const chartOptions = {
  chart: {
    height: 200,
    width: 200,
    type: 'radialBar',
    toolbar: {
      show: false
    }
  },
  grid: {
    padding: {
      top: -10,
      bottom: -15
    }
  },
  plotOptions: {
    radialBar: {
      startAngle: -135,
      endAngle: 225,
      hollow: {
        margin: 0,
        size: '75%',
        background: undefined,
        image: undefined,
        imageOffsetX: 0,
        imageOffsetY: 0,
        position: 'front',
        dropShadow: {
          enabled: true,
          top: 3,
          left: 0,
          blur: 4,
          opacity: 0.24
        }
      },
      track: {
        background: '#fff',
        strokeWidth: '67%',
        margin: 0, // margin is in pixels
        dropShadow: {
          enabled: true,
          top: -3,
          left: 0,
          blur: 4,
          opacity: 0.35
        }
      },
      dataLabels: {
        show: true,
        name: {
          offsetY: -10,
          show: true,
          color:  themeHelper.isDarkTheme ? 'white' : '#888',
          fontSize: '16px'
        },
        value: {
          formatter(val) {
            return `${val} %`
          },
          offsetY: 0,
          color:  themeHelper.isDarkTheme ? 'white' : '#888',
          fontSize: '22px',
          show: true
        }
      }
    }
  },
  fill: {
    type: 'gradient',
    gradient: {
      shade: 'dark',
      type: 'horizontal',
      shadeIntensity: 0.5,
      gradientToColors: ['#7ED6F3'],
      inverseColors: true,
      opacityFrom: 1,
      opacityTo: 1,
      stops: [0, 100]
    }
  },
  stroke: {
    lineCap: 'round'
  },
  labels: ['STARTED']
}

const series = computed(() => {
  const percent = (myProgress.value.numProjectsContributed / myProjects.value.length) * 100
  if (percent > 0) {
    if (percent < 1) {
      return [1]
    }
    return [Math.round(percent)]
  }
  return [0]
})
const projectsNotContributedToYet = computed(() => myProjects.value.length - myProgress.value.numProjectsContributed)

</script>

<template>
  <my-progress-info-card-util title="Projects">
    <template #left-content>
      <span class="text-4xl text-color-warn mr-1" data-cy="numProjectsContributed">{{ myProgress.numProjectsContributed }}</span>
      <span class="text-secondary" data-cy="numProjectsAvailable">/ {{ myProjects.length }}</span>
    </template>
    <template #right-content>
      <div class="flex justify-content-center sm:justify-content-end">
        <apexchart type="radialBar" height="200" width="200" :options="chartOptions" :series="series"></apexchart>
      </div>
    </template>
    <template #footer>
      <div class="flex gap-2 align-items-center flex-column sm:flex-row">
        <div v-if="projectsNotContributedToYet > 0"
             data-cy="info-snap-footer"
             class="w-min-12rem">
          You still have
          <Tag severity="info">{{ projectsNotContributedToYet
            }}
          </Tag>
          project{{ projectsNotContributedToYet > 1 ? 's' : '' }} to explore.
        </div>
        <div v-else
             data-cy="info-snap-footer"
             class="w-min-12rem"
             title="Great job, you have contributed to all projects!">Great job, you have contributed to all
          projects!
        </div>
        <div class="flex-1 text-right">
        <router-link :to="{ name: 'MyUsagePage' }" tabindex="-1">
          <SkillsButton
            label="Usage"
            icon="fas fa-chart-line"
            outlined
            size="small"
            variant="outline-info"
            data-cy="viewUsageBtn" />
          </router-link>
        </div>
      </div>
    </template>
  </my-progress-info-card-util>
</template>

<style scoped>

</style>