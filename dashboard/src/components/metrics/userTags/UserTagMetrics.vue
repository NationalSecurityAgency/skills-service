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
    <div class="row mb-3">
      <div class="col-12">
        <level-breakdown-metric :title="`Overall Levels for ${metricTitle}`" />
      </div>
    </div>
    <users-table-metric :title="`Users for ${metricTitle}`" />
  </div>
</template>

<script>
  import LevelBreakdownMetric from '../common/LevelBreakdownMetric';
  import UsersTableMetric from './UsersTableMetric';

  export default {
    name: 'UserTagMetrics',
    components: {
      UsersTableMetric,
      LevelBreakdownMetric,
    },
    data() {
      return {
        tagCharts: null,
      };
    },
    mounted() {
      this.buildTagCharts();
    },
    computed: {
      metricLabel() {
        const chartInfo = this.tagCharts?.find((i) => i.key === this.$route.params.tagKey);
        return chartInfo ? `${chartInfo.title}:` : '';
      },
      metricValue() {
        return this.$route.params.tagFilter;
      },
      metricTitle() {
        return `${this.metricLabel} ${this.metricValue}`;
      },
    },
    methods: {
      buildTagCharts() {
        if (this.$store.getters.config && this.$store.getters.config.projectMetricsTagCharts) {
          const json = this.$store.getters.config.projectMetricsTagCharts;
          const charts = JSON.parse(json);
          this.tagCharts = charts;
        }
        return [];
      },
    },
  };
</script>

<style scoped>

</style>
