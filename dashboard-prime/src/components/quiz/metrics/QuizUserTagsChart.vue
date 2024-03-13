<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router';
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js';
import QuizService from '@/components/quiz/QuizService.js';
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue';
import { useUserTagsUtils } from "@/components/utils/UseUserTagsUtils.js";

const route = useRoute()
const numberFormat = useNumberFormat()
const userTagsUtils = useUserTagsUtils();

const loading = ref(true);
const hasData = ref(false);
const series = ref([]);

const chartOptions = {
  chart: {
    height: 350,
    width: 250,
    type: 'bar',
    toolbar: {
      show: true,
      offsetX: -13,
      offsetY: 0,
    },
  },
  tooltip: {
    y: {
      formatter(val) {
        return numberFormat.pretty(val);
      },
    },
  },
  plotOptions: {
    bar: {
      horizontal: true,
      barHeight: '40%',
      dataLabels: {
        position: 'bottom',
      },
      distributed: true,
    },
  },
  stroke: {
    show: true,
    width: 2,
    colors: ['transparent'],
  },
  xaxis: {
    categories: [],
    title: {
      text: '# of runs',
    },
    labels: {
      formatter(val) {
        return Math.trunc(val);
      },
    },
  },
  yaxis: {
    title: {
      text: '',
    },
    labels: {
      show: false,
    },
  },
  dataLabels: {
    enabled: true,
    textAnchor: 'start',
    offsetX: 0,
    formatter(val, {seriesIndex, dataPointIndex, w}) {
      return `${w.globals.seriesX[seriesIndex][dataPointIndex]}: ${numberFormat.pretty(val)}`;
    },
    style: {
      colors: ['#17a2b8'],
      fontSize: '14px',
      fontFamily: 'Helvetica, Arial, sans-serif',
      fontWeight: 'bold',
    },
    dropShadow: {
      enabled: true,
    },
    background: {
      enabled: true,
      foreColor: '#ffffff',
      padding: 10,
      borderRadius: 2,
      borderWidth: 1,
      borderColor: '#686565',
      opacity: 1,
      dropShadow: {
        enabled: false,
      },
    },
  },
  legend: {
    show: false,
  },
};

onMounted(()=> {        
  loading.value = true;
  QuizService.getUserTagCounts(route.params.quizId, userTagsUtils.userTagKey())
      .then((res) => {
        const seriesData = res.map((item) => ({ x: item.value, y: item.count }));
        const height = seriesData.length > 0 ? 150 + (seriesData.length * 50) : 350;
        chartOptions.chart.height = height;
        series.value = [{ data: seriesData, name: '# of Runs' }];
        hasData.value = seriesData.length > 0;
      })
      .finally(() => {
        loading.value = false;
      });
})
</script>

<template>
    <Card :pt="{ content: { class: 'p-0' } }" data-cy="quizUserTagsChart">
      <template #title>{{ `${userTagsUtils.userTagLabel()} Metrics (Top 20)` }}</template>
      <template #content>
        <div style="max-height: 400px; min-height: 275px; overflow-y: auto; overflow-x: clip;" class="pt-2 pr-2">
          <MetricsOverlay :loading="loading" :has-data="hasData" no-data-msg="No data yet...">
          <apexchart v-if="!loading" type="bar" :height="chartOptions.chart.height" :options="chartOptions" :series="series"></apexchart>
          </MetricsOverlay>
        </div>
      </template>
    </Card>
</template>

<style scoped>
.apexcharts-toolbar {
  color: green;
  z-index: 10000 !important;
}
</style>