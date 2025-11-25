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
import {computed, onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import SubjectsService from "@/components/subjects/SubjectsService.js";
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue'
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import Chart from "primevue/chart";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import dayjs from "dayjs";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";

const route = useRoute();
const layoutSizes = useLayoutSizesState()
const chartSupportColors = useChartSupportColors()

const loading = ref({
  subjects: true,
  charts: false,
  generatedAtLeastOnce: false,
});
const subjects = ref({
  selected: null,
  available: [],
});
const series = ref([]);
const chartData = ref({})

const hasData = computed(() => chartData.value?.datasets !== undefined && chartData.value?.datasets?.length > 0)

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  loadSubjects();
})

const loadSubjects = () => {
  SubjectsService.getSubjects(route.params.projectId)
      .then((res) => {
        subjects.value.available = res.map((subj) => ({ value: subj.subjectId, text: subj.name }));
        loading.value.subjects = false;
      });
};

const loadChart = () => {
  loading.value.charts = true;
  const params = { subjectId: subjects.value.selected };
  MetricsService.loadChart(route.params.projectId, 'usersByLevelForSubjectOverTimeChartBuilder', params)
      .then((res) => {
        // sort by level to force order in the legend's display
        res.sort((a, b) => a.level - b.level);

        const formatTimestamp = (timestamp) => {
          const format = 'YYYY-MM-DD';
          return dayjs(timestamp).format(format)
        }
        const datasets = res.map((resItem) => {
          return {
            label: `Level ${resItem.level}`,
            data: resItem.counts.map((item) => {
              return {x: formatTimestamp(item.value), y: item.count}
            }),
            cubicInterpolationMode: 'monotone',
            borderColor: chartSupportColors.getSolidColor(resItem.level),
            backgroundColor: chartSupportColors.getTranslucentColor(resItem.level, 0.5),
          }
        })
        chartData.value = {datasets}

        loading.value.charts = false;
        loading.value.generatedAtLeastOnce = true;
      });
};

const overlayMessage  = computed(() => {
  if (!loading.value.generatedAtLeastOnce && !hasData.value) {
    return 'Generate the chart using controls above!'
  }
  if (loading.value.generatedAtLeastOnce && !hasData.value) {
    return 'Zero users achieved levels for this subject!'
  }
  return ''
})

const chartJsOptions = ref();
const subjectLevelsOverTimeChart = ref(null);
const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  const textColor = colors.textColor
  const textColorSecondary = colors.textMutedColor
  const surfaceBorder =  colors.contentBorderColor

  return {
    responsive: true,
    maintainAspectRatio: false,
    elements: {
      point: {
        pointStyle: false
      }
    },
    scales: {
      x: {
        type: 'time',
        time: {
          unit: 'month'
        },
        ticks: {
          color: textColorSecondary
        },
        grid: {
          color: surfaceBorder,
          drawOnChartArea: false  // Ensures no grid lines are drawn in the chart area
        }
      },
      y: {
        beginAtZero: true,
        ticks: {
          color: textColorSecondary
        },
        grid: {
          color: surfaceBorder
        },
        title: {
          display: true,
          text: 'Distinct # of Users',
          color: textColorSecondary,
          font: {
            size: 12,
            weight: '500'
          },
          padding: { left: 10, right: 5 }
        }
      }
    },
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: textColor,
          padding: 20,
          boxWidth: 12,
          usePointStyle: true,
          pointStyle: 'circle'
        }
      },
      tooltip: {
        callbacks: {
          title: (context) => {
            const date = new Date(context[0].parsed.x)
            const formatStr = 'MMM D, YYYY'
            return dayjs(date).format(formatStr)
          },
        },
      }
    }
  };
}
</script>

<template>
  <Card data-cy="subjectNumUsersPerLevelOverTime" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader title="Number of users for each level over time"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex gap-2 mb-4 flex-col sm:flex-row">
        <BlockUI :blocked="loading.subjects" rounded-sm="sm" opacity="0.5" spinner-variant="info" spinner-type="grow" spinner-small class="flex flex-1">
          <Select :options="subjects.available"
                    v-model="subjects.selected"
                    optionLabel="text"
                    optionValue="value"
                    class="w-full"
                    placeholder="Select a Subject to plot"
                    data-cy="subjectNumUsersPerLevelOverTime-subjectSelector">
          </Select>
        </BlockUI>
        <SkillsButton variant="outline-info" class="ml-2" :disabled="!subjects.selected" @click="loadChart" icon="fas fa-paint-roller" label="Generate" data-cy="genSubLevelsOverTimeBtn" />
      </div>
      <metrics-overlay :loading="loading.charts" :has-data="hasData" :no-data-msg="overlayMessage">
        <chart-download-controls :vue-chart-ref="subjectLevelsOverTimeChart" />
        <Chart ref="subjectLevelsOverTimeChart"
               id="subjectLevelsOverTimeChart"
               type="line"
               :data="chartData"
               :options="chartJsOptions"
               class="h-[17rem]"
        />
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>