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
import { ref, onMounted, watch } from 'vue';
import NumberFormatter from "@/components/utils/NumberFormatter.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  titleIcon: {
    type: String,
    required: true,
  },
  series: {
    type: Array,
    required: true,
  },
  labels: {
    type: Array,
    required: true,
  },
  horizontal: {
    type: Boolean,
    default: false,
  },
});

const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()

const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }
  return themeHelper.isDarkTheme ? 'white' : undefined
}

const chartId = ref(props.title.replace(/\s+/g, ''));
const chartRef = ref();
const loading = ref(true);
const hasData = ref(false);
const seriesInternal = ref([]);
const options = {
  chart: {
    type: 'bar',
    height: 350,
    toolbar: {
      show: false,
    },
  },
  legend: {
    show: false,
  },
  plotOptions: {
    bar: {
      horizontal: props.horizontal,
      distributed: true,
      columnWidth: '50%',
      endingShape: 'rounded',
    },
  },
  dataLabels: {
    enabled: false,
  },
  xaxis: {
    categories: props.labels,
    labels: {
      style: {
        colors: chartAxisColor()
      }
    }
  },
  yaxis: {
    min: 0,
    labels: {
      style: {
        colors: chartAxisColor()
      },
      formatter(val) {
        return typeof val === 'number' ? NumberFormatter.format(val) : val;
      },
    },
  },
  tooltip: {
    theme: themeHelper.isDarkTheme ? 'dark' : 'light',
  }
};

onMounted(() => {
  seriesInternal.value = [{
    name: props.title,
    data: props.series,
  }];

  hasData.value = props.series && props.series.length > 0 && props.series.find((item) => item > 0) !== undefined;

  loading.value = false;
});

watch(() => props.series, () => {
  seriesInternal.value = [{
    name: props.title,
    data: props.series
  }]
});

watch(() => props.labels, () => {
  chartRef.value.updateOptions({
    xaxis: {
      categories: props.labels,
    },
  })
})
</script>

<template>
  <Card class="w-full">
    <template #content>
      <div class="text-center">
        <span class="font-weight-bold"><i :class="titleIcon" class="mr-2 text-secondary"></i>{{ title }}</span>
        <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="Selected projects don't have any data">
          <apexchart v-if="!loading"
                     ref="chartRef"
                     type="bar" height="350"
                     :options="options"
                     :series="seriesInternal"/>
        </metrics-overlay>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>