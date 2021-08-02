/*
Copyright 2020 SkillTree

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
<template>
  <div>
    <div class="row">
      <div class="col-12">
        <num-users-per-day />
      </div>
    </div>
    <div v-if="tagCharts" class="row" data-cy="userTagCharts">
      <div class="col-12 col-md-6 mt-2" v-for="(tagChart, index) in tagCharts" :key="`${tagChart.key}-${index}`">
        <div  v-if="index > 0 && index % 2 == 0" class="w-100"></div>
<!--        <user-tag-pie-chart v-if="tagChart.type == 'pie'" class="h-100" :tag-key="tagChart.key" :title="tagChart.title"/>-->
        <user-tag-chart class="h-100" :chart-type="tagChart.type" :tag-key="tagChart.key" :title="tagChart.title"/>
      </div>
    </div>
  </div>
</template>

<script>
  import NumUsersPerDay from './common/NumUsersPerDay';
  // import UserTagPieChart from './common/UserTagPieChart';
  // import UserTagBarChart from './common/UserTagBarChart';
  import UserTagChart from './common/UserTagChart';

  export default {
    name: 'ProjectMetrics',
    components: {
      NumUsersPerDay, UserTagChart,
    },
    computed: {
      tagCharts() {
        if (this.$store.getters.config && this.$store.getters.config.projectMetricsTagCharts) {
          const json = this.$store.getters.config.projectMetricsTagCharts;
          const charts = JSON.parse(json);
          return charts;
        }
        return [];
      },
    },
  };
</script>

<style scoped>

</style>
