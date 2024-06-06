<script setup>
import { ref, onMounted } from 'vue';
import MetricsService from "@/components/metrics/MetricsService.js";
import { useRoute } from 'vue-router';
import NumberFormatter from '@/components/utils/NumberFormatter.js'
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";

const route = useRoute();
const props = defineProps({
  title: {
    type: String,
    required: false,
    default: 'Overall Levels',
  },
});

const isLoading = ref(true);
const isEmpty = ref(false);
const series = ref([]);
const chartOptions = ref({
  chart: {
    type: 'bar',
    height: 350,
    toolbar: {
      show: true,
      offsetX: -20,
      offsetY: -35,
    },
  },
  plotOptions: {
    bar: {
      barHeight: '90%',
      endingShape: 'rounded',
      distributed: true,
      horizontal: true,
      dataLabels: {
        position: 'bottom',
      },
    },
  },
  dataLabels: {
    enabled: true,
    textAnchor: 'start',
    style: {
      colors: ['#17a2b8'],
      fontSize: '14px',
      fontFamily: 'Helvetica, Arial, sans-serif',
      fontWeight: 'bold',
    },
    formatter(val, opt) {
      return `${opt.w.globals.labels[opt.dataPointIndex]}: ${NumberFormatter.format(val)} users`;
    },
    offsetX: 0,
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
  stroke: {
    show: true,
    width: 2,
    colors: ['transparent'],
  },
  xaxis: {
    categories: [],
    labels: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
  yaxis: {
    labels: {
      show: false,
    },
  },
  grid: {
    borderColor: '#cfeaf3',
    position: 'front',
  },
  legend: {
    show: false,
  },
  fill: {
    opacity: 1,
  },
  tooltip: {
    y: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
});

onMounted(() => {
  let localProps = { };
  // figure out if subjectId is passed based on the context (page it's being loaded from)
  if (route.params.subjectId) {
    localProps = { subjectId: route.params.subjectId };
  } else if (route.params.tagKey && route.params.tagFilter) {
    localProps = { tagKey: route.params.tagKey, tagFilter: route.params.tagFilter };
  }

  MetricsService.loadChart(route.params.projectId, 'numUsersPerLevelChartBuilder', localProps)
      .then((response) => {
        // sort by level
        const sorted = response.sort((item) => item.value).reverse();

        isEmpty.value = response.find((item) => item.count > 0) === undefined;
        chartOptions.value.xaxis.categories = sorted.map((item) => item.value);
        series.value = [{
          name: 'Number of Users',
          data: sorted.map((item) => item.count),
        }];
        isLoading.value = false;
      });
});
</script>

<template>
  <Card data-cy="levelsChart" >
    <template #header>
      <SkillsCardHeader :title="title"></SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="isLoading" :has-data="!isLoading && !isEmpty" no-data-icon="fa fa-info-circle" no-data-msg="No one reached Level 1 yet...">
        <apexchart v-if="!isLoading" type="bar" height="350" :options="chartOptions" :series="series" class="mt-5" />
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>