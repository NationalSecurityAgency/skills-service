/*
Copyright 2025 SkillTree

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
import { ref, onMounted, computed } from "vue";
import Chart from 'primevue/chart';
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";

const chartSupportColors = useChartSupportColors()

const props = defineProps({
  value: {
    type: Number,
    default: 0
  },
  max: {
    type: Number,
    default: 100
  }
});

const chartData = ref();
const chartOptions = ref(null);

onMounted(() => {
  chartData.value = setChartData();
  chartOptions.value = setChartOptions();
});

const colors = chartSupportColors.getColors()
const setChartData = () => {
  const remaining = Math.max(0, props.max - props.value);
  console.log(remaining)
  return {
    labels: ['Completed', 'Remaining'],
    datasets: [{
      data: [props.value, remaining],
      backgroundColor: [colors.green700Color, colors.surface300Color],
      borderWidth: 0
    }]
  };
};

const setChartOptions = () => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '85%',
    circumference: 270,
    rotation: -135,
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        enabled: false
      }
    },
    animation: {
      animateScale: true,
      animateRotate: true
    }
  };
};

const percentage = computed(() => {
  return Math.round((props.value / props.max) * 100);
});
</script>

<template>
  <div class="relative h-full">
    <Chart
        type="doughnut"
        :data="chartData"
        :options="chartOptions"
    />
    <div class="absolute inset-0 flex flex-col items-center justify-center">
      <div class="text-xl">{{ percentage }}%</div>
      <div class="text-sm text-gray-500">Complete</div>
    </div>
  </div>
</template>

<style scoped>
/* Add any custom styles here */
</style>