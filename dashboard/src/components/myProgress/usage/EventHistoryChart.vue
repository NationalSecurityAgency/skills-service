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
import { computed, onMounted, ref, watch } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import dayjs from 'dayjs'
import AutoComplete from 'primevue/autocomplete';
import NumberFormatter from '@/components/utils/NumberFormatter.js'
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue';
import TimeLengthSelector from "@/components/metrics/common/TimeLengthSelector.vue";

const props = defineProps({
  availableProjects: {
    type: Array,
    required: true,
  },
  title: {
    type: String,
    required: false,
    default: 'Your Daily Usage History',
  },
});

const appConfig = useAppConfig()

const timeLengthSelector = ref(null);
const loading = ref(true);
const hasData = ref(false);
const mutableTitle = ref(props.title);
const projects = ref({
    selected: [],
    available: [],
  });
const timeProps = ref({
  start: dayjs().subtract(30, 'day').valueOf(),
});
const timeSelectorOptions = ref([
  {
    length: 30,
    unit: 'days',
  },
  {
    length: 6,
    unit: 'months',
  },
  {
    length: 1,
    unit: 'year',
  },
]);
const series = ref([]);
const chartOptions = ref({
  chart: {
    height: 350,
    type: 'line',
    zoom: {
      type: 'x',
      enabled: true,
      autoScaleYaxis: true,
    },
    toolbar: {
      autoSelected: 'zoom',
    },
  },
  dataLabels: {
    enabled: false,
  },
  stroke: {
    curve: 'smooth',
    dashArray: [0, 0, 5, 0, 0],
  },
  markers: {
    size: 0,
    hover: {
      sizeOffset: 6,
    },
  },
  yaxis: {
    labels: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
    title: {
      text: 'Skill Events Reported',
    },
  },
  xaxis: {
    type: 'datetime',
  },
  tooltip: {
    shared: false,
    y: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
  grid: {
    borderColor: '#f1f1f1',
  },
})

onMounted(() => {
  projects.value.available = props.availableProjects.map((proj) => ({ ...proj }));
  const numProjectsToSelect = Math.min(props.availableProjects.length, 4);
  const availableSortedByMostPoints = projects.value.available.sort((a, b) => b.points - a.points);
  projects.value.selected = availableSortedByMostPoints.slice(0, numProjectsToSelect);
  // loadData() is not called here because of the watch on `projects.selected`, which is triggered by the assignment above
})

const enoughOverallProjects = computed(() => {
  return props.availableProjects && props.availableProjects.length > 0;
});
const enoughProjectsSelected = computed(() => {
  return projects.value.selected && projects.value.selected.length > 0;
});
const noDataMessage = computed(() => {
  if (!enoughOverallProjects.value) {
    return 'There are no projects available.';
  }
  if (!enoughProjectsSelected.value) {
    return 'Please select at least one project from the list above.';
  }
  return 'There are no events for the selected project(s) and time period.';
});
const beforeListSlotText = computed(() => {
  if (projects.value.selected.length >= 5) {
    return 'Maximum of 5 options selected. First remove a selected option to select another.';
  }
  return '';
});
watch(() => projects.value.selected, () => {
  timeProps.value.projIds = projects.value.selected.map((project) => project.projectId);
  loadData();
})

const updateTimeRange = (timeEvent) => {
  if (appConfig.maxDailyUserEvents) {
    const oldestDaily = dayjs().subtract(appConfig.maxDailyUserEvents, 'day');
    if (timeEvent.startTime < oldestDaily) {
      mutableTitle.value = 'Events per week';
    } else {
      mutableTitle.value = props.title;
    }
  }
  timeProps.value.start = timeEvent.startTime.valueOf();
  loadData();
}
const loadData = () => {
  loading.value = true;
  if (enoughOverallProjects.value && enoughProjectsSelected.value) {
    MetricsService.loadMyMetrics('allProjectsSkillEventsOverTimeMetricsBuilder', timeProps.value)
        .then((response) => {
          if (response && response.length > 0 && notAllZeros(response)) {
            hasData.value = true;
            series.value = response.map((item) => {
              const ret = {};
              ret.project = props.availableProjects.find(({ projectId }) => projectId === item.project);
              ret.name = ret.project.projectName;
              ret.data = item.countsByDay.map((it) => [it.timestamp, it.num]);
              return ret;
            });
          } else {
            series.value = [];
            hasData.value = false;
          }
          loading.value = false;
        });
  } else {
    hasData.value = false;
    loading.value = false;
  }
};
const notAllZeros = (data) => {
  return data.filter((item) => item.countsByDay.find((it) => it.num > 0)).length > 0;
}
</script>

<template>
  <Card data-cy="eventHistoryChart">
    <template #header>
      <SkillsCardHeader :title="mutableTitle" title-tag="h2">
        <template #headerContent>
          <span class="text-muted ml-2">|</span>
          <time-length-selector ref="timeLengthSelector" :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex w-full mb-2">
        <MultiSelect
            v-model="projects.selected"
            :options="projects.available"
            display="chip"
            optionLabel="projectName"
            aria-label="Select projects"
            placeholder="Select projects"
            class="w-full"
            :selection-limit="5"
            data-cy="eventHistoryChartProjectSelector">

        </MultiSelect>
      </div>
      <MetricsOverlay :loading="loading" :has-data="hasData" :no-data-msg="noDataMessage">
        <apexchart type="line" height="350"
                   :options="chartOptions"
                   :series="series">
        </apexchart>
      </MetricsOverlay>
    </template>
  </Card>
</template>

<style scoped>

</style>