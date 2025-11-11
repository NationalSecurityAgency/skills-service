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
import { ref } from 'vue';
import dayjs from "dayjs";

const props = defineProps({
  vueChartRef: {
    type: Object,
    required: true
  },
})

const getChartDataForExport = (chartRef) => {
  // Handle both direct chart instance and ref
  const chart = chartRef?.value || chartRef;
  if (!chart?.data) {
    console.warn('Chart instance not available', chartRef);
    return null;
  }

  return chart.data.datasets.map(dataset => ({
    label: dataset.label,
    data: dataset.data
  }));
};

const exportChartDataToCSV = () => {
  const chartData = getChartDataForExport(props.vueChartRef);
  if (!chartData) return;
  console.log(chartData)
  const headers = ['Date', ...chartData.map(ds => ds.label)];
  let csvContent = headers.join(',') + '\n';
  console.log(csvContent)
  chartData[0].data.forEach((item, index) => {
    csvContent += `${item.x},${item.y}`
    // get other datasets
    const otherVals = []
    if (chartData.length > 1) {
      for (let i = 1; i < chartData.length; i++) {
        const otherItem = chartData[i].data[index]
        if (item.x !== otherItem.x) {
          throw new Error(`${item.x} !== ${otherItem.x} at index [${index}] from the [${i + 1}] datasets`)
        }
        otherVals.push(otherItem.y)
      }
      csvContent += `,${otherVals.join(',')}`;
    }
    csvContent += '\n';
  })

  // Trigger download
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `users_metrics_${dayjs().format('YYYY-MM-DD')}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

const exportChartToJPG = () => {
  const chart = props.vueChartRef?.chart;
  if (!chart) {
    console.warn('Chart instance not available');
    return;
  }

  // Get the canvas from the chart instance
  const canvas = chart.canvas;
  if (!canvas) {
    console.warn('Canvas element not found');
    return;
  }
  // Create a temporary canvas with white background
  const tempCanvas = document.createElement('canvas');
  const tempCtx = tempCanvas.getContext('2d');

  // Set the same dimensions as the original canvas
  tempCanvas.width = canvas.width;
  tempCanvas.height = canvas.height;

  // Fill with white background
  tempCtx.fillStyle = 'white';
  tempCtx.fillRect(0, 0, tempCanvas.width, tempCanvas.height);

  // Draw the original canvas on top of the white background
  tempCtx.drawImage(canvas, 0, 0);

  // Create a temporary link to trigger download
  const link = document.createElement('a');
  link.download = `users_chart_${dayjs().format('YYYY-MM-DD')}.jpg`;
  link.href = tempCanvas.toDataURL('image/jpeg', 0.9); // 0.9 is the quality (0.0 to 1.0)
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

const chartMenu = ref();
const toggle = (event) => {
  chartMenu.value.toggle(event);
};

const items = [
  {
    label: 'Export to CSV',
    icon: 'fa-solid fa-file-csv',
    command: () => exportChartDataToCSV()
  },
  {
    label: 'Export to JPG',
    icon: 'fa-solid fa-file-image',
    command: () => exportChartToJPG()
  }
];

</script>

<template>
<div>
  <div class="flex justify-end">
    <SkillsButton
        icon="fa-solid fa-bars"
        @click="toggle"
        aria-haspopup="true"
        aria-controls="chartMenu"
        text
        aria-label="Chart Download Menu"/>
    <Menu ref="chartMenu" id="chartMenu" :model="items" :popup="true" />
  </div>
</div>
</template>

<style scoped>

</style>