<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import dayjs from 'dayjs';
import TimeLengthSelector from "@/components/metrics/common/TimeLengthSelector.vue";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import MetricsService from "@/components/metrics/MetricsService.js";
import NumberFormatter from '@/components/utils/NumberFormatter.js'

const route = useRoute();
const props = defineProps(['skillName']);

const title = 'Skill events';
const loading = ref(true);
const hasData = ref(false);
const series = ref([]);
const start = ref(dayjs().subtract(30, 'day').valueOf());
const timeSelectorOptions = [
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
];
const chartOptions = {
  chart: {
    height: 250,
    type: 'line',
    id: 'areachart-2',
    toolbar: {
      show: true,
      offsetY: -52,
      autoSelected: 'zoom',
      tools: {
        pan: false,
      },
    },
  },
  colors: ['#28a745', '#008ffb'],
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
        return NumberFormatter.format(val);
      },
    },
    title: {
      text: '# of Applied Skill Events',
    },
  },
  legend: {
    position: 'top',
  },
};

onMounted(() => {
  loadData();
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'skillEventsOverTimeChartBuilder', { skillId: route.params.skillId, start: start.value })
      .then((dataFromServer) => {
        let appliedEvents = [];
        let allEvents = [];
        if (dataFromServer.countsByDay && dataFromServer.countsByDay.length > 1) {
          appliedEvents = dataFromServer.countsByDay.map((item) => [item.timestamp, item.num]);
        }
        if (dataFromServer.allEvents && dataFromServer.allEvents.length > 0) {
          allEvents = dataFromServer.allEvents.map((item) => [item.timestamp, item.num]);
        }

        const s = [];
        let hasAppliedSkillEvents = false;
        if (appliedEvents && appliedEvents.length > 0) {
          s.push({
            name: 'Applied Skill Events',
            data: appliedEvents,
          });
          hasAppliedSkillEvents = true;
        }

        let hasAllEvents = false;
        if (allEvents && allEvents.length > 0) {
          s.push({
            name: 'All Skill Events',
            data: allEvents,
          });
          hasAllEvents = true;
        }

        // eslint-disable-next-line
        hasData.value = Boolean(hasAllEvents | hasAppliedSkillEvents);
        series.value = s;
        loading.value = false;
      });
};

const updateTimeRange = (timeEvent) => {
  start.value = timeEvent.startTime.valueOf();
  loadData();
};
</script>

<template>
  <Card data-cy="appliedSkillEventsOverTimeMetric">
    <template #header>
      <SkillsCardHeader :title="title">
        <span class="text-muted ml-2">|</span>
        <time-length-selector :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="This chart needs at least 2 days of user activity.">
        <apexchart type="line" height="350" :options="chartOptions" :series="series" class="mt-4"></apexchart>
      </metrics-overlay>
      <div class="font-light text-sm">Please Note: Only 'applied' events contribute to users' points and achievements. An event will not be applied if that skill has already reached its maximum points or has unfulfilled dependencies.</div>
    </template>
  </Card>
</template>

<style scoped>

</style>