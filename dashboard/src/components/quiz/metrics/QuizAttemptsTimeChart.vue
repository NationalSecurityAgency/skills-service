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
import { useRoute } from 'vue-router';
import { useNumberFormat } from "@/common-components/filter/UseNumberFormat.js";
import QuizService from '@/components/quiz/QuizService.js';
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue';

const route = useRoute()
const numberFormat = useNumberFormat()

const loading = ref(false);
const hasData = ref(false);
const numItems = ref(0);
const series = ref([]);

const chartOptions = {
  chart: {
    height: 350,
    type: 'line',
    id: 'quizAttemptsTimeChart',
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
    colors: ['#28a745', '#008ffb'],
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
    min: 0,
    labels: {
      formatter(val) {
        return numberFormat.pretty(val);
      },
    },
    title: {
      text: '# of Runs',
    },
  },
  legend: {
    position: 'top',
  },
}

onMounted(()=> {        
  loading.value = true;
  QuizService.getUsageOverTime(route.params.quizId)
      .then((res) => {
        hasData.value = res && res.length > 0;
        numItems.value = hasData.value ? res.length : 0;
        series.value = [{
          data: res.map((item) => [item.value, item.count]),
          name: 'Runs',
        }];
      })
      .finally(() => {
        loading.value = false;
      });
})
</script>

<template>

  <Card>
    <template #title>Runs Over Time</template>
    <template #content>
      <MetricsOverlay :loading="loading" :has-data="hasData && numItems > 1" no-data-msg="This chart needs at least 2 days worth of runs">
        <apexchart type="line" width="100%" :height="chartOptions.chart.height" :options="chartOptions" :series="series"></apexchart>
      </MetricsOverlay>
    </template>
  </Card>
</template>

<style scoped>
</style>