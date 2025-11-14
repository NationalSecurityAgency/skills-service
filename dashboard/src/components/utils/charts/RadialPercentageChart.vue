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
  },
  fullCircle: {
    type: Boolean,
    default: false
  },
  cutout: {
    type: String,
    default: '85%'
  },
  sizeInRem: {
    type: Number,
    default: 10
  },
  completedBarColor: {
    type: String,
    default: null
  },
  remainingBarColor: {
    type: String,
    default: null
  },
});


onMounted(() => {
});

const colors = chartSupportColors.getColors()
const completedBarColor = computed(() => {
  return props.completedBarColor || colors.green700Color
})
const remainingBarColor = computed(() => {
  return props.remainingBarColor || colors.surface300Color
})
const chartData = computed(() => {
  const maxValue = props.max <= 0 ? 100 : props.max
  const remaining = Math.max(0, maxValue - props.value);
  return {
    labels: ['Completed', 'Remaining'],
    datasets: [{
      data: [props.value, remaining],
      backgroundColor: [completedBarColor.value, remainingBarColor.value],
      borderWidth: 0
    }]
  };
})

const chartOptions = computed(() => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    cutout: props.cutout,
    circumference: props.fullCircle ? 360 : 270,
    rotation:  props.fullCircle ? 0 : -135,
    onHover: () => null, // Disable hover state changes
    hover: { mode: null }, // Disable hover mode
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
})

const percentage = computed(() => {
  return props.max <= 0 ? 0 : Math.round((props.value / props.max) * 100);
});
</script>

<template>
  <div class="w-full h-full flex justify-center items-center">
    <div :class="`relative h-[${sizeInRem}rem] w-[${sizeInRem}rem]`">
      <Chart
          type="doughnut"
          :data="chartData"
          :options="chartOptions"
          :class="`h-[${sizeInRem}rem] w-[${sizeInRem}rem]`"
      />
      <div class="absolute inset-0 flex flex-col justify-center items-center">
        <slot name="center">
          <div class="text-xl">{{ percentage }}%</div>
          <div class="text-sm text-gray-500">Complete</div>
        </slot>
      </div>
    </div>
  </div>
</template>

<style scoped>
</style>