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
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js'

const route = useRoute();
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()

const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }
  return themeHelper.isDarkTheme ? 'white' : undefined
}

const series = ref([]);
const chartOptions = ref ({
  chart: {
    height: 350,
    type: 'area',
    toolbar: {
      show: true,
      offsetY: -52,
      autoSelected: 'zoom',
      tools: {
        pan: false,
      },
    },
  },
  dataLabels: {
    enabled: false,
  },
  stroke: {
    curve: 'smooth',
  },
  fill: {
    type: 'gradient',
    gradient: {
      shade: 'light',
      gradientToColors: ['#17a2b8', '#28a745'],
      shadeIntensity: 1,
      type: 'horizontal',
      opacityFrom: 0.3,
      opacityTo: 0.8,
      stops: [0, 100, 100, 100],
    },
  },
  grid: {
    padding: {
      right: 30,
      left: 20,
    },
  },
  xaxis: {
    type: 'datetime',
  },
  yaxis: {
    forceNiceScale: true,
    min: 0,
    labels: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
      style: {
        colors: chartAxisColor()
      }
    },
    title: {
      text: '# Users',
    },
  },
  legend: {
    position: 'top',
  },
  tooltip: {
    theme: themeHelper.isDarkTheme ? 'dark' : 'light',
  },
});
const loading = ref(true);
const hasData = ref(false);

onMounted(() => {
  loadData();
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'numUserAchievedOverTimeChartBuilder', { skillId: route.params.skillId })
      .then((dataFromServer) => {
        if (dataFromServer.achievementCounts) {
          const datSeries = dataFromServer.achievementCounts.map((item) => [item.timestamp, item.num]);
          hasData.value = datSeries.length > 0;
          if (hasData.value) {
            const dayAgo = dataFromServer.achievementCounts[0].timestamp - (1000 * 60 * 60 * 24);
            datSeries.unshift([dayAgo, 0]);
          }
          series.value = [{
            name: '# Users Achieved',
            data: datSeries,
          }];
        }
        loading.value = false;
      });
};
</script>

<template>
  <Card data-cy="numUsersAchievedOverTimeMetric">
    <template #header>
      <SkillsCardHeader title="Achievements over time"></SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No achievements yet for this skill.">
        <apexchart v-if="chartOptions?.chart?.height" type="area" :height="chartOptions.chart.height" :options="chartOptions" :series="series" class="mt-6"></apexchart>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>