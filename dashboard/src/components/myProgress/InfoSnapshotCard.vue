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
import {computed, onMounted, ref} from 'vue'
import MyProgressInfoCardUtil from '@/components/myProgress/MyProgressInfoCardUtil.vue'
import {useMyProgressState} from '@/stores/UseMyProgressState.js'
import Chart from "primevue/chart";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";

const myProgressState = useMyProgressState()
const myProgress = computed(() => myProgressState.myProgress)
const myProjects = computed(() => myProgressState.myProjects)
const chartSupportColors = useChartSupportColors()

const percentStarted = computed(() => {
  const percent = (myProgress.value.numProjectsContributed / myProjects.value.length) * 100
  if (percent > 0) {
    if (percent < 1) {
      return 1
    }
    return Math.round(percent)
  }
  return 0
})

const colors = chartSupportColors.getColors()
const chartData = computed(() => {
  const percent = percentStarted.value
  return {
    labels: ['Not Started', "Started"],
    datasets: [
      {
        label: 'Started %',
        data: [percent, 100 - percent],
        backgroundColor: [colors.green700Color, colors.surface100Color],
      }
    ]
  }
})

const projectsNotContributedToYet = computed(() => myProjects.value.length - myProgress.value.numProjectsContributed)
const chartJsOptions = ref(null)
onMounted(() => {
  chartJsOptions.value = setChartOptions()
})

const setChartOptions = () => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '80%',
    hover: { mode: null },
    plugins: {
      legend: {
        display: false
      },
      title: {
        display: false,
      },
      tooltip: {
        enabled: false,
      },
    },
  };
}

const chartPlugins = [{
  id: 'centerText',
  beforeDraw(chart) {
    const { ctx, chartArea: { left, right, top, bottom, width, height } } = chart;
    const centerX = left + width / 2;
    const centerY = top + height / 2;
    ctx.save();


    // Draw percentage
    ctx.font = 'bold 20px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = colors.textColor;
    ctx.fillText(`${percentStarted.value}%`, centerX, centerY - 10);

    // Draw label
    ctx.font = '12px Arial';
    ctx.fillText('Started', centerX, centerY + 15);

    // Restore the context state
    ctx.restore();
  }
}];
</script>

<template>
  <my-progress-info-card-util title="Projects">
    <template #left-content>
      <span class="text-4xl text-orange-700 dark:text-orange-400 mr-1" data-cy="numProjectsContributed">{{ myProgress.numProjectsContributed }}</span>
      <span class="text-secondary" data-cy="numProjectsAvailable">/ {{ myProjects.length }}</span>
    </template>
    <template #right-content>
      <div class="flex justify-end w-full">
        <div class="w-[10rem] h-[10rem]">
          <Chart
            type="doughnut"
            :data="chartData"
            :options="chartJsOptions"
            :plugins="chartPlugins"
            class="w-full h-full"
          />
        </div>
      </div>
    </template>
    <template #footer>
      <div class="flex gap-2 items-center flex-col sm:flex-row">
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