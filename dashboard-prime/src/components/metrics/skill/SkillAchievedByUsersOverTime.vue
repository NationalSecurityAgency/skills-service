<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js'

const route = useRoute();

const series = ref([]);
const chartOptions = {
  chart: {
    height: 250,
    type: 'area',
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
  },
  fill: {
    type: 'gradient',
    gradient: {
      shade: 'light',
      gradientToColors: ['#17a2b8', '#28a745'],
      shadeIntensity: 1,
      type: 'horizontal',
      opacityFrom: 0.3,
      opacityTo: 0.8,
      stops: [0, 100, 100, 100],
    },
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
    labels: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
    title: {
      text: '# Users',
    },
  },
  legend: {
    position: 'top',
  },
};
const loading = ref(true);
const hasData = ref(false);

onMounted(() => {
  loadData();
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'numUserAchievedOverTimeChartBuilder', { skillId: route.params.skillId })
      .then((dataFromServer) => {
        if (dataFromServer.achievementCounts) {
          const datSeries = dataFromServer.achievementCounts.map((item) => [item.timestamp, item.num]);
          hasData.value = datSeries.length > 0;
          if (hasData.value) {
            const dayAgo = dataFromServer.achievementCounts[0].timestamp - (1000 * 60 * 60 * 24);
            datSeries.unshift([dayAgo, 0]);
          }
          series.value = [{
            name: '# Users Achieved',
            data: datSeries,
          }];
        }
        loading.value = false;
      });
};
</script>

<template>
  <Card data-cy="numUsersAchievedOverTimeMetric">
    <template #header>
      <SkillsCardHeader title="Achievements over time"></SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No achievements yet for this skill.">
        <apexchart type="area" height="350" :options="chartOptions" :series="series" class="-mt-4"></apexchart>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>