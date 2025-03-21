/*
Copyright 2024 SkillTree

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
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import { useUserTagChartConfig } from '@/components/metrics/common/UserTagChartConfig.js';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";

const route = useRoute();
const userTagChartConfig = useUserTagChartConfig();
const layoutSizes = useLayoutSizesState()

const props = defineProps({
  tagKey: {
    type: String,
    required: true,
  },
  chartType: {
    type: String,
    required: true,
    validator: (value) => (['pie', 'bar'].indexOf(value) >= 0),
  },
  title: {
    type: String,
    required: false,
    default: 'Users',
  },
})

onMounted(() => {
  if (props.chartType === 'pie') {
    chartOptions.value = userTagChartConfig.pieChartOptions;
  }
  if (props.chartType === 'bar') {
    chartOptions.value = userTagChartConfig.barChartOptions;
  }
  loadData();
});

const isLoading = ref(true);
const isEmpty = ref(false);
const series = ref([]);
const chartOptions = ref({});
const heightInPx = ref(350);
const titleInternal = ref(props.title);

const loadData = () => {
  isLoading.value = true;

  const params = {
    tagKey: props.tagKey,
    currentPage: 1,
    pageSize: 20,
    sortDesc: true,
    tagFilter: '',
  };

  MetricsService.loadChart(route.params.projectId, 'numUsersPerTagBuilder', params)
      .then((dataFromServer) => {
        if (dataFromServer) {
          const localSeries = [];
          const labels = [];
          const { items } = dataFromServer;
          items.forEach((data) => {
            localSeries.push(data.count);
            labels.push(data.value);
          });
          if (props.chartType === 'pie') {
            series.value = localSeries;
          }
          if (props.chartType === 'bar') {
            series.value = [{
              name: 'Number of Users',
              data: localSeries,
            }];
          }
          chartOptions.value = Object.assign(chartOptions.value, { labels });
          isEmpty.value = items.find((item) => item.count > 0) === undefined;

          if (items.length > 10) {
            heightInPx.value = 600;
          }
          if (dataFromServer.totalNumItems > params.pageSize) {
            titleInternal.value = `${titleInternal.value} (Top ${params.pageSize})`;
          }
        }
        isLoading.value = false;
      });
};
</script>

<template>
  <Card data-cy="userTagChart" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader :title="titleInternal"></SkillsCardHeader>
    </template>
    <template #content>
      <skills-spinner :is-loading="isLoading" v-if="isLoading" />
      <div v-if="!isLoading">
        <metrics-overlay :loading="isLoading" :has-data="!isEmpty" no-data-msg="No data yet...">
          <apexchart :type="chartType" width="100%" :height="`${heightInPx}px`"  :options="chartOptions" :series="series"></apexchart>
        </metrics-overlay>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>