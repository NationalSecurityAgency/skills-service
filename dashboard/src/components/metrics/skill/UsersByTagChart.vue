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
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';

const route = useRoute();
const props = defineProps(['tag']);

const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()

const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }
  return themeHelper.isDarkTheme ? 'white' : undefined
}

const inProgressSeries = ref([]);
const achievedSeries = ref([]);
const chartOptions = {
  chart: {
    height: 250,
    width: 250,
    type: 'bar',
    toolbar: {
      show: true,
      offsetX: 0,
      offsetY: -60,
    },
  },
  tooltip: {
    theme: themeHelper.isDarkTheme ? 'dark' : 'light',
    y: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
  plotOptions: {
    bar: {
      horizontal: true,
      barHeight: '30%',
      dataLabels: {
        position: 'bottom',
      },
      distributed: true,
    },
  },
  stroke: {
    show: true,
    width: 2,
    colors: ['transparent'],
  },
  xaxis: {
    categories: [],
    title: {
      style: {
        color: chartAxisColor()
      },
      text: '# of Users',
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
    categories: [],
    title: {
      style: {
        color: chartAxisColor()
      },
      text: props.tag.label,
    },
    labels: {
      style: {
        colors: chartAxisColor()
      }
    }
  },
  dataLabels: {
    enabled: false,
  },
  legend: {
    show: false,
  },
};
const loading = ref(true);

onMounted(() => {
  loadData();
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'skillAchievementsByTagBuilder', { skillId: route.params.skillId, userTagKey: props.tag.key })
      .then((dataFromServer) => {
        chartOptions.labels = Object.keys(dataFromServer);
        const inProgressData = [];
        const achievedData = [];

        chartOptions.labels.forEach((label) => {
          inProgressData.push({ x: label, y: dataFromServer[label].numberInProgress });
          achievedData.push({ x: label, y: dataFromServer[label].numberAchieved });
        });

        const totalInProgressData = inProgressData.map((value) => value.y).filter((value) => value > 0);
        const totalAchievedData = achievedData.map((value) => value.y).filter((value) => value > 0);

        if (inProgressData.length > 0 && totalInProgressData.length > 0) {
          inProgressSeries.value = [{ data: inProgressData, name: 'In Progress' }];
        }

        if (achievedData.length > 0 && totalAchievedData.length > 0) {
          achievedSeries.value = [{ data: achievedData, name: 'Achieved' }];
        }
        loading.value = false;
      });
};
</script>

<template>
  <Card data-cy="numUsersByTag">
    <template #header>
      <SkillsCardHeader :title="`Top 20 ${tag.label} User Counts`"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex flex-column xl:flex-row gap-4">
        <div class="flex flex-1">
          <Card data-cy="usersInProgressByTag" class="w-full">
            <template #header>
              <SkillsCardHeader title="In Progress"></SkillsCardHeader>
            </template>
            <template #content>
              <metrics-overlay :loading="loading" :has-data="inProgressSeries.length > 0" no-data-msg="No users currently working on this skill.">
                <apexchart v-if="!loading" type="bar" height="350" :options="chartOptions" :series="inProgressSeries"></apexchart>
              </metrics-overlay>
            </template>
          </Card>
        </div>
        <div class="flex flex-1">
          <Card data-cy="usersAchievedByTag" class="w-full">
            <template #header>
              <SkillsCardHeader title="Achieved"></SkillsCardHeader>
            </template>
            <template #content>
              <metrics-overlay :loading="loading" :has-data="achievedSeries.length > 0" no-data-msg="No achievements yet for this skill.">
                <apexchart v-if="!loading" type="bar" height="350" :options="chartOptions" :series="achievedSeries"></apexchart>
              </metrics-overlay>
            </template>
          </Card>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>