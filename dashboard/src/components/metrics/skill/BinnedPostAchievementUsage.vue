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
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';
import MetricsService from '@/components/metrics/MetricsService.js'
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue'

const route = useRoute();
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()

const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }
  return themeHelper.isDarkTheme ? 'white' : undefined
}

const loading = ref(true);
const isEmpty = ref(true);
const series = ref([]);
const chartOptions = ref({
  chart: {
    type: 'bar',
      height: 250,
      toolbar: {
        show: true,
        offsetX: 0,
        offsetY: -55,
    },
  },
  plotOptions: {
    bar: {
      horizontal: false,
    },
  },
  dataLabels: {
    enabled: false,
  },
  stroke: {
    show: true,
        width: 2,
        colors: ['transparent'],
  },
  xaxis: {
    categories: [],
    title: {
      text: '# of times Skill performed',
      style: {
        color: chartAxisColor(),
      },
    },
    labels: {
      style: {
        fontSize: '13px',
        fontWeight: 600,
        colors: chartAxisColor(),
      },
    },
  },
  yaxis: {
    title: {
      text: '# of distinct users',
    },
    labels: {
      style: {
        colors: chartAxisColor(),
      },
    },
  },
  fill: {
    opacity: 1,
  },
  tooltip: {
    theme: themeHelper.isDarkTheme ? 'dark' : 'light',
    y: {
      formatter(val) {
        return `${val}`;
      },
    },
  },
  legend: {
    offsetY: 5,
  },
});

onMounted(() => {
  MetricsService.loadChart(route.params.projectId, 'binnedUsagePostAchievementMetricsBuilder', { skillId: route.params.skillId })
    .then((res) => {
      updateChart(res);
      loading.value = false;
    });
});

const updateChart = (res) => {
  const localSeries = [];
  if (res && res.length > 0) {
    isEmpty.value = false;
    chartOptions.value.xaxis.categories = res.map((labeledCount) => labeledCount.label);
    const data = [];
    res.forEach((labeledCount) => {
      data.push(labeledCount.count);
    });
    localSeries.push({
      data,
      name: '# of users',
    });
  }
  series.value = localSeries;
};
</script>

<template>
  <Card data-cy="binnedNumUsersPostAchievement">
    <template #header>
      <SkillsCardHeader title="Usage"></SkillsCardHeader>
    </template>
    <template #content>
      <MetricsOverlay :loading="loading" :has-data="!isEmpty" no-data-msg="No achievements yet for this skill.">
        <apexchart v-if="!loading" type="bar" height="350" :options="chartOptions" :series="series"></apexchart>
      </MetricsOverlay>
      <div class="font-light text-sm">Number of times this Skill is performed per user after having fully achieved it.</div>
    </template>
  </Card>
</template>

<style scoped>

</style>