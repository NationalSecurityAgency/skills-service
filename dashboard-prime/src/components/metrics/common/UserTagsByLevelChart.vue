<script setup>
import { ref, computed, onMounted } from 'vue';
import MetricsService from "@/components/metrics/MetricsService.js";
import { useRoute } from 'vue-router';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js';

const props = defineProps(['tag']);
const route = useRoute();

const series = ref([]);
const loading = ref(true);
const chartOptions = ref({
  chart: {
    width: 250,
        type: 'bar',
        toolbar: {
      show: true,
          offsetX: 0,
          offsetY: 0,
    },
  },
  plotOptions: {
    bar: {
      horizontal: true,
          dataLabels: {
        position: 'bottom',
      },
    },
  },
  stroke: {
    show: true,
        width: 2,
        colors: ['transparent'],
  },
  xaxis: {
    title: {
      text: '# of Users',
    },
    labels: {
      style: {
        fontSize: '13px',
            fontWeight: 600,
      },
    },
  },
  yaxis: {
    title: {
      text: props.tag.label,
    },
  },
  tooltip: {
    y: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
  dataLabels: {
    enabled: true,
        textAnchor: 'start',
        offsetX: 0,
        style: {
      colors: ['#17a2b8'],
          fontSize: '14px',
          fontFamily: 'Helvetica, Arial, sans-serif',
          fontWeight: 'bold',
    },
    formatter(val, opt) {
      return `${opt.w.globals.seriesNames[opt.seriesIndex]}: ${NumberFormatter.format(val)} users`;
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
});

onMounted(() => {
  loadData();
})

const chartHeight = computed(() => {
  let height = 350;
  if (chartOptions.value?.xaxis?.categories) {
    const dataSize = chartOptions.value.xaxis.categories.length;
    height = dataSize > 0 ? dataSize * 250 : 350;
  }
  return height;
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'achievementsByTagPerLevelMetricsBuilder', { subjectId: route.params.subjectId, userTagKey: props.tag.key })
      .then((dataFromServer) => {
        if (dataFromServer && Object.keys(dataFromServer.data).length > 0) {
          const userData = dataFromServer.data;
          const tags = Object.keys(userData);

          if (tags) {
            const categories = userData.map((a) => a.tag);
            chartOptions.value.xaxis.categories = categories;
            const numberOfLevels = dataFromServer.totalLevels;
            const localSeries = [];

            for (let level = 1; level <= numberOfLevels; level += 1) {
              const dataForLevel = [];
              tags.forEach((tag) => {
                if (userData[tag].value[level] > 0) {
                  dataForLevel.push(userData[tag].value[level]);
                } else {
                  dataForLevel.push(0);
                }
              });
              if (dataForLevel.length > 0) {
                localSeries.push({ name: `Level ${level}`, data: dataForLevel });
              }
            }
            series.value = localSeries;
          }
        }
        loading.value = false;
      });
};
</script>

<template>
  <Card :data-cy="`numUsersByTag-${tag.key}`">
    <template #header>
      <SkillsCardHeader :title="`Top 20 ${tag.label} Level Breakdown`"></SkillsCardHeader>
    </template>
    <template #content>
      <div style="max-height: 800px; overflow-y: auto; overflow-x: clip;">
        <metrics-overlay :loading="loading" :has-data="series.length > 0" no-data-msg="No users currently">
          <apexchart v-if="!loading" type="bar" :height="chartHeight" :options="chartOptions" :series="series"></apexchart>
        </metrics-overlay>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>