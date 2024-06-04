<script setup>
import { ref, onMounted, watch } from 'vue';
import NumberFormatter from "@/components/utils/NumberFormatter.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  titleIcon: {
    type: String,
    required: true,
  },
  series: {
    type: Array,
    required: true,
  },
  labels: {
    type: Array,
    required: true,
  },
  horizontal: {
    type: Boolean,
    default: false,
  },
});


const chartId = ref(props.title.replace(/\s+/g, ''));
const loading = ref(true);
const hasData = ref(false);
const seriesInternal = ref([]);
const options = {
  chart: {
    type: 'bar',
        height: 350,
        toolbar: {
      show: false,
    },
  },
  legend: {
    show: false,
  },
  plotOptions: {
    bar: {
      horizontal: props.horizontal,
      distributed: true,
      columnWidth: '50%',
      endingShape: 'rounded',
    },
  },
  dataLabels: {
    enabled: false,
  },
  xaxis: {
    categories: props.labels,
  },
  yaxis: {
    min: 0,
    labels: {
      formatter(val) {
        return typeof val === 'number' ? NumberFormatter.format(val) : val;
      },
    },
  },
};

onMounted(() => {
  seriesInternal.value = [{
    name: props.title,
    data: props.series,
  }];

  hasData.value = props.series && props.series.length > 0 && props.series.find((item) => item > 0) !== undefined;

  loading.value = false;
});

watch(() => props.series, () => {
  seriesInternal.value = [{
    name: props.title,
    data: props.series
  }]
});

watch(() => props.labels, () => {
  // chartId.value.updateOptions({
  //   xaxis: {
  //     categories: props.labels,
  //   },
  // })
  // this.$refs[this.chartId].updateOptions({

  // });
})
</script>

<template>
  <Card class="w-full">
    <template #content>
      <div class="text-center">
        <span class="font-weight-bold"><i :class="titleIcon" class="mr-2 text-secondary"></i>{{ title }}</span>
        <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="Selected projects don't have any data">
          <apexchart v-if="!loading"
                     :ref="chartId"
                     type="bar" height="350"
                     :options="options"
                     :series="seriesInternal"/>
        </metrics-overlay>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>