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
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';
import MetricsService from "@/components/metrics/MetricsService.js";
import SubjectsService from "@/components/subjects/SubjectsService.js";
import NumberFormatter from '@/components/utils/NumberFormatter.js'
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue'
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";

const route = useRoute();
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()
const layoutSizes = useLayoutSizesState()

const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }
  return themeHelper.isDarkTheme ? 'white' : undefined
}

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
const chartOptions = ref({
  chart: {
    type: 'line',
    toolbar: {
      offsetY: -20,
    },
  },
  colors: ['#008FFB', '#546E7A', '#00E396'],
  yaxis: {
    title: {
      text: '# of users',
    },
    labels: {
      style: {
        colors: chartAxisColor()
      },
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
    min: 0,
    forceNiceScale: true,
  },
  xaxis: {
    type: 'datetime',
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
    showForSingleSeries: true,
  },
  tooltip: {
    theme: themeHelper.isDarkTheme ? 'dark' : 'light',
  },
});

const isSeriesEmpty = computed(() => {
  return !series.value || series.value.length === 0;
});

onMounted(() => {
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
        series.value = res.map((resItem) => {
          const data = resItem.counts.map((dayCount) => [dayCount.value, dayCount.count]);
          return {
            name: `Level ${resItem.level}`,
            data,
          };
        });

        loading.value.charts = false;
        loading.value.generatedAtLeastOnce = true;
      });
};

const overlayMessage  = computed(() => {
  if (!loading.value.generatedAtLeastOnce && isSeriesEmpty.value) {
    return 'Generate the chart using controls above!'
  }
  if (loading.value.generatedAtLeastOnce && isSeriesEmpty.value) {
    return 'Zero users achieved levels for this subject!'
  }
  return ''
})
</script>

<template>
  <Card data-cy="subjectNumUsersPerLevelOverTime" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader title="Number of users for each level over time"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex gap-2 mb-8 flex-col sm:flex-row">
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
        <SkillsButton variant="outline-info" class="ml-2" :disabled="!subjects.selected" @click="loadChart" icon="fas fa-paint-roller" label="Generate" />
      </div>
      <metrics-overlay :loading="loading.charts" :has-data="!isSeriesEmpty" :no-data-msg="overlayMessage" class="mt-6">
        <apexchart type="area" height="300" width="100%" :options="chartOptions" :series="series"></apexchart>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>