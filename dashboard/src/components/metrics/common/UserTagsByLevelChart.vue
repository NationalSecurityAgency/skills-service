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
import { ref, computed, onMounted } from 'vue';
import MetricsService from "@/components/metrics/MetricsService.js";
import { useRoute } from 'vue-router';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js';
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';

const props = defineProps(['tag']);
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
const loading = ref(true);
const chartOptions = ref({
  chart: {
    width: 250,
    type: 'bar',
    toolbar: {
      show: true,
      offsetX: 0,
      offsetY: 0,
    },
  },
  plotOptions: {
    bar: {
      horizontal: true,
      dataLabels: {
        position: 'bottom',
      },
    },
  },
  stroke: {
    show: true,
    width: 2,
    colors: ['transparent'],
  },
  xaxis: {
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
    title: {
      text: props.tag.label,
    },
    labels: {
      style: {
        colors: chartAxisColor()
      }
    }
  },
  tooltip: {
    theme: themeHelper.isDarkTheme ? 'dark' : 'light',
    y: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
  dataLabels: {
    enabled: true,
        textAnchor: 'start',
        offsetX: 0,
        style: {
      colors: ['#17a2b8'],
          fontSize: '14px',
          fontFamily: 'Helvetica, Arial, sans-serif',
          fontWeight: 'bold',
    },
    formatter(val, opt) {
      return `${opt.w.globals.seriesNames[opt.seriesIndex]}: ${NumberFormatter.format(val)} users`;
    },
    dropShadow: {
      enabled: true,
    },
    background: {
      enabled: true,
          foreColor: '#ffffff',
          padding: 10,
          borderRadius: 2,
          borderWidth: 1,
          borderColor: '#686565',
          opacity: 1,
          dropShadow: {
        enabled: false,
      },
    },
  },
  legend: {
    show: false,
  },
});

onMounted(() => {
  loadData();
})

const chartHeight = computed(() => {
  let height = 350;
  if (chartOptions.value?.xaxis?.categories) {
    const dataSize = chartOptions.value.xaxis.categories.length;
    height = dataSize > 0 ? dataSize * 250 : 350;
  }
  return height;
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'achievementsByTagPerLevelMetricsBuilder', { subjectId: route.params.subjectId, userTagKey: props.tag.key })
      .then((dataFromServer) => {
        if (dataFromServer && Object.keys(dataFromServer.data).length > 0) {
          const userData = dataFromServer.data;
          const tags = Object.keys(userData);

          if (tags) {
            const categories = userData.map((a) => a.tag);
            chartOptions.value.xaxis.categories = categories;
            const numberOfLevels = dataFromServer.totalLevels;
            const localSeries = [];

            for (let level = 1; level <= numberOfLevels; level += 1) {
              const dataForLevel = [];
              tags.forEach((tag) => {
                if (userData[tag].value[level] > 0) {
                  dataForLevel.push(userData[tag].value[level]);
                } else {
                  dataForLevel.push(0);
                }
              });
              if (dataForLevel.length > 0) {
                localSeries.push({ name: `Level ${level}`, data: dataForLevel });
              }
            }
            series.value = localSeries;
          }
        }
        loading.value = false;
      });
};
</script>

<template>
  <Card :data-cy="`numUsersByTag-${tag.key}`">
    <template #header>
      <SkillsCardHeader :title="`Top 20 ${tag.label} Level Breakdown`"></SkillsCardHeader>
    </template>
    <template #content>
      <div style="max-height: 800px; overflow-y: auto; overflow-x: clip;">
        <metrics-overlay :loading="loading" :has-data="series.length > 0" no-data-msg="No users currently">
          <apexchart v-if="!loading" type="bar" :height="chartHeight" :options="chartOptions" :series="series"></apexchart>
        </metrics-overlay>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>