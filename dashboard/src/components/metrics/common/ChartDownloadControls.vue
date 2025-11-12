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
    type: [Object, null], // Allow both Object and null
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
    chartType: chart.type,
    label: dataset.label,
    data: dataset.data,
    barChart: {
      labels: chart.data.labels,
    }
  }));
};

const exportChartDataToCSV = () => {
  const chartData = getChartDataForExport(props.vueChartRef);
  if (!chartData) return;

  let csvContent = ''
  if (chartData[0].chartType === 'bar') {
    csvContent = createCvsForBarChart(chartData)
  } else {
    csvContent = createCvsForTimeSeriesData(chartData)
  }

  // Trigger download
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `chart_export_${dayjs().format('YYYY-MM-DD HH:mm:ss')}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

const createCvsForTimeSeriesData = (chartData) => {
  const headers = ['Date', ...chartData.map(ds => ds.label)];
  let csvContent = headers.join(',') + '\n';

  // iterate over all datasets and get unique x values
  const uniqueXValues = new Set()
  const yLookupMaps = []
  chartData.forEach(ds => {
    const yLookupMap = new Map()
    ds.data.forEach(item => {
      uniqueXValues.add(item.x)
      yLookupMap.set(item.x, item.y)
    })
    yLookupMaps.push(yLookupMap)
  })
  // sort x values
  const sortedXValues = Array.from(uniqueXValues).sort((a, b) => a - b)

  sortedXValues.forEach(x => {
    csvContent += `${x}`
    yLookupMaps.forEach(yLookupMap => {
      csvContent += `,${yLookupMap.get(x) || '0'}`
    })
    csvContent += '\n'
  })

  // chartData[0].data.forEach((item, index) => {
  //   csvContent += `${item.x},${item.y}`
  //   // get other datasets
  //   const otherVals = []
  //   if (chartData.length > 1) {
  //     for (let i = 1; i < chartData.length; i++) {
  //       const otherItem = chartData[i].data[index]
  //       if (item.x !== otherItem.x) {
  //         throw new Error(`${item.x} !== ${otherItem.x} at index [${index}] from the [${i + 1}] datasets`)
  //       }
  //       otherVals.push(otherItem.y)
  //     }
  //     csvContent += `,${otherVals.join(',')}`;
  //   }
  //   csvContent += '\n';
  // })

  return csvContent
}

const createCvsForBarChart = (chartData) => {
  let csvContent = ''
  csvContent += ['Category', ...chartData.map((singleChartData) => singleChartData.label)].join(',') + '\n'
  const firstDataset = chartData[0]
  firstDataset.barChart.labels.forEach((label, index) => {
    csvContent += `${label},${firstDataset.data[index]}`
    if (chartData.length > 1) {
      const otherVals = []
      for (let i = 1; i < chartData.length; i++) {
        const otherDataset = chartData[i]
        otherVals.push(otherDataset.data[index])
      }
      csvContent += `,${otherVals.join(',')}`;
    }
    csvContent += '\n';
  })

  return csvContent
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
  link.download = `chart_image_${dayjs().format('YYYY-MM-DD HH:mm:ss')}.jpg`;
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