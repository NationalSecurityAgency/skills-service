<script setup>
import { ref, onMounted } from 'vue';
import NumUsersPerDay from "@/components/metrics/common/NumUsersPerDay.vue";
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import UserTagTable from "@/components/metrics/common/UserTagTable.vue";
import UserTagChart from "@/components/metrics/common/UserTagChart.vue";

const appConfig = useAppConfig();
const tagCharts = ref(null);

onMounted(() => {
  buildTagCharts();
});

const buildTagCharts = () => {
  if (appConfig.projectMetricsTagCharts) {
    const json = appConfig.projectMetricsTagCharts;
    tagCharts.value = JSON.parse(json);
  }
  return [];
}
</script>

<template>
  <div>
    <div class="flex">
      <div class="w-full">
        <num-users-per-day />
      </div>
    </div>
    <div v-if="tagCharts" class="flex gap-4 mt-4 mb-4" data-cy="userTagCharts">
      <div class="flex flex-1" v-for="(tagChart, index) in tagCharts" :key="`${tagChart.key}-${index}`">
<!--        <div v-if="index > 0 && index % 2 == 0" class="w-100"></div>-->
        <user-tag-table v-if="tagChart.type === 'table'"
                        class="h-100"
                        :tag-chart="tagChart"
                        tabindex="0" />
        <user-tag-chart v-if="tagChart.type !== 'table'"
                        class="h-100 w-full"
                        :chart-type="tagChart.type"
                        :tag-key="tagChart.key"
                        :title="tagChart.title"
                        tabindex="0" />
      </div>
    </div>
  </div>
</template>

<style scoped></style>
