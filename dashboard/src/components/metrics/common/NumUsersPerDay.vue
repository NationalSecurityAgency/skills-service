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
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import dayjs from 'dayjs';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import MetricsService from "@/components/metrics/MetricsService.js";
import TimeLengthSelector from "@/components/metrics/common/TimeLengthSelector.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";


const appConfig = useAppConfig();
const route = useRoute();
const props = defineProps({
  title: {
    type: String,
    required: false,
    default: 'Users per day',
  },
});

const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()
const layoutSizes = useLayoutSizesState()

const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }
  return themeHelper.isDarkTheme ? 'white' : undefined
}

onMounted(() => {
  if (route.params.skillId) {
    localProps.value.skillId = route.params.skillId;
  } else if (route.params.subjectId) {
    localProps.value.skillId = route.params.subjectId;
  }
  loadData();
})

const loading = ref(true);
const distinctUsersOverTime = ref([]);
const hasDataEnoughData = ref(false);
const mutableTitle = ref(props.title);
const localProps = ref({
  start: dayjs().subtract(30, 'day').valueOf(),
  byMonth: false,
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
const chartOptions = ref({
  chart: {
    type: 'area',
    stacked: false,
    height: 350,
    zoom: {
      type: 'x',
      enabled: true,
      autoScaleYaxis: true,
    },
    toolbar: {
      autoSelected: 'zoom',
      offsetY: -30,
    },
  },
  dataLabels: {
    enabled: false,
  },
  markers: {
    size: 0,
  },
  fill: {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      inverseColors: false,
      opacityFrom: 0.5,
      opacityTo: 0,
      stops: [0, 90, 100],
    },
  },
  yaxis: {
    labels: {
      style: {
        colors: chartAxisColor()
      },
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
    title: {
      text: 'Distinct # of Users',
    },
  },
  xaxis: {
    type: 'datetime',
    labels: {
      style: {
        colors: chartAxisColor()
      }
    }
  },
  tooltip: {
    theme: themeHelper.isDarkTheme ? 'dark' : 'light',
    shared: false,
        y: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
});

const byMonth = ref(false);
const dateOptions = [{ label: 'Day/Week', value: false}, { label: 'Month', value: true }]

const updateTimeRange = (timeEvent) => {
  if (appConfig) {
    const oldestDaily = dayjs().subtract(appConfig.maxDailyUserEvents, 'day');
    if (!byMonth.value) {
      if (timeEvent.startTime < oldestDaily) {
        mutableTitle.value = 'Users per week';
      } else {
        mutableTitle.value = props.title;
      }
    } else {
      mutableTitle.value = 'Users per month';
    }
  }
  localProps.value.start = timeEvent.startTime.valueOf();
  loadData();
};

const allZeros = (data) => {
  console.log(data)
  return data.filter((item) => item.count > 0).length === 0;
};

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'distinctUsersOverTimeForProject', localProps.value)
      .then((response) => {

        if (response && response.users?.length > 1 && !allZeros(response.users)) {
          hasDataEnoughData.value = true;
          distinctUsersOverTime.value = [{
            data: response.users.map((item) => [item.value, item.count]),
            name: 'Users',
          }, {
            data: response.newUsers.map((item) => [item.value, item.count]),
            name: 'New Users',
          }];
        } else {
          distinctUsersOverTime.value = [];
          hasDataEnoughData.value = false;
        }
        loading.value = false;
      });
};

const dateOptionChanged = (option) => {
  byMonth.value = option
  localProps.value.byMonth = option
  if(byMonth.value) {
    mutableTitle.value = 'Users per month';
  } else {
    mutableTitle.value = 'Users per week';
  }
  loadData()
}
</script>

<template>
  <Card data-cy="distinctNumUsersOverTime" class="w-full" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader :title="mutableTitle">
        <template #headerContent>
          <SelectButton :allowEmpty="false" size="small" :defaultValue="false" :options="dateOptions" v-model="byMonth" @update:modelValue="dateOptionChanged" optionLabel="label" optionValue="value"></SelectButton>
          |
          <time-length-selector :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasDataEnoughData" no-data-msg="This chart needs at least 2 days of user activity." class="mt-6">
        <apexchart type="area" height="350" width="100%" :options="chartOptions" :series="distinctUsersOverTime" data-cy="apexchart"></apexchart>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>