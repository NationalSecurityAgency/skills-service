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

const route = useRoute();

onMounted(() => {
  MetricsService.loadChart(route.params.projectId, 'numUsersPerSubjectPerLevelChartBuilder')
      .then((res) => {
        updateChart(res);
        loading.value = false;
      });
});

const loading = ref(true);
const isEmpty = ref(true);
const series = ref([]);
const chartOptions = ref({
  chart: {
    type: 'bar',
    height: 350,
    toolbar: {
      show: true,
      offsetX: 0,
      offsetY: -60,
    },
  },
  plotOptions: {
    bar: {
      horizontal: false,
      columnWidth: '55%',
      endingShape: 'rounded',
    },
  },
  dataLabels: {
    enabled: false,
  },
  stroke: {
    show: true,
    width: 2,
    colors: ['transparent'],
  },
  xaxis: {
    categories: [],
    axisTicks: {
      height: 300,
      offsetY: -300,
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
      text: '# of users',
    },
  },
  fill: {
    opacity: 1,
  },
  tooltip: {
    y: {
      formatter(val) {
        return `${val}`;
      },
    },
  },
  legend: {
    offsetY: 5,
  },
});

const updateChart = (res) => {
  const localSeries = [];
  const sortedSubjects = res.sort((subj) => subj.subject);
  chartOptions.value.xaxis.categories = sortedSubjects.map((subj) => subj.subject);
  const allLevels = sortedSubjects.map((subj) => subj.numUsersPerLevels.length);
  if (allLevels) {
    const maxLevel = Math.max(...allLevels);
    for (let i = 1; i <= maxLevel; i += 1) {
      const data = sortedSubjects.map((subj) => {
        const found = subj.numUsersPerLevels.find((item) => item.level === i);
        const numUsers = found ? found.numberUsers : 0;
        if (numUsers > 0) {
          isEmpty.value = false;
        }
        return numUsers;
      });
      localSeries.push({
        name: `Level ${i}`,
        data,
      });
    }
  }
  series.value = localSeries;
};
</script>

<template>
  <Card data-cy="userCountsBySubjectMetric">
    <template #header>
      <SkillsCardHeader title="Number of users for each level for each subject"></SkillsCardHeader>
    </template>
    <template #content>
      <BlockUI :blocked="loading" opacity=".5">
        <apexchart v-if="loading" type="bar" height="350" :options="{}" :series="[]" class="mt-4"></apexchart>
      </BlockUI>
      <BlockUI :blocked="loading" opacity=".5">
        <apexchart v-if="!loading" type="bar" height="350" :options="chartOptions" :series="series" class="mt-4"></apexchart>
        <div class="alert alert-info" v-if="!loading && series.length === 0">
          <i class="fas fa-user-clock"></i> Users have not achieved any levels, yet...
        </div>
      </BlockUI>
    </template>
  </Card>
</template>

<style scoped>

</style>