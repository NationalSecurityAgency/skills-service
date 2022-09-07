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
        <level-breakdown-metric />
      </div>
    </div>
    <users-table-metric />
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
