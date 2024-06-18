<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import dayjs from 'dayjs';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import MetricsService from "@/components/metrics/MetricsService.js";
import TimeLengthSelector from "@/components/metrics/common/TimeLengthSelector.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js'

const appConfig = useAppConfig();
const route = useRoute();
const props = defineProps({
  title: {
    type: String,
    required: false,
    default: 'Users per day',
  },
});

onMounted(() => {
  if (route.params.skillId) {
    localProps.value.skillId = route.params.skillId;
  } else if (route.params.subjectId) {
    localProps.value.skillId = route.params.subjectId;
  }
  loadData();
})

const loading = ref(true);
const distinctUsersOverTime = ref([]);
const hasDataEnoughData = ref(false);
const mutableTitle = ref(props.title);
const localProps = ref({
  start: dayjs().subtract(30, 'day').valueOf()
});
const timeSelectorOptions = ref([
  {
    length: 30,
    unit: 'days',
  },
  {
    length: 6,
    unit: 'months',
  },
  {
    length: 1,
    unit: 'year',
  },
]);
const chartOptions = ref({
  chart: {
    type: 'area',
    stacked: false,
    height: 350,
    zoom: {
      type: 'x',
      enabled: true,
      autoScaleYaxis: true,
    },
    toolbar: {
      autoSelected: 'zoom',
      offsetY: -52,
    },
  },
  dataLabels: {
    enabled: false,
  },
  markers: {
    size: 0,
  },
  fill: {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      inverseColors: false,
      opacityFrom: 0.5,
      opacityTo: 0,
      stops: [0, 90, 100],
    },
  },
  yaxis: {
    labels: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
    title: {
      text: 'Distinct # of Users',
    },
  },
  xaxis: {
    type: 'datetime',
  },
  tooltip: {
    shared: false,
        y: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
});

const updateTimeRange = (timeEvent) => {
  if (appConfig) {
    const oldestDaily = dayjs().subtract(appConfig.maxDailyUserEvents, 'day');
    if (timeEvent.startTime < oldestDaily) {
      mutableTitle.value = 'Users per week';
    } else {
      mutableTitle.value = props.title;
    }
  }
  localProps.value.start = timeEvent.startTime.valueOf();
  loadData();
};

const allZeros = (data) => {
  return data.filter((item) => item.count > 0).length === 0;
};

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'distinctUsersOverTimeForProject', localProps.value)
      .then((response) => {
        if (response && response.length > 1 && !allZeros(response)) {
          hasDataEnoughData.value = true;
          distinctUsersOverTime.value = [{
            data: response.map((item) => [item.value, item.count]),
            name: 'Users',
          }];
        } else {
          distinctUsersOverTime.value = [];
          hasDataEnoughData.value = false;
        }
        loading.value = false;
      });
};
</script>

<template>
  <Card data-cy="distinctNumUsersOverTime" class="w-full">
    <template #header>
      <SkillsCardHeader :title="mutableTitle">
        <template #headerContent>
          <time-length-selector :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasDataEnoughData" no-data-msg="This chart needs at least 2 days of user activity." class="mt-4">
        <apexchart type="area" height="350" :options="chartOptions" :series="distinctUsersOverTime" data-cy="apexchart"></apexchart>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>