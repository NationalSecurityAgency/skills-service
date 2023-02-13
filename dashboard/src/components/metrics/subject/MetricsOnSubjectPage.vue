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
    <sub-page-header title="Metrics"/>
    <level-breakdown-metric title="Subject Levels"/>
    <num-users-per-day class="my-3" title="Subject's users per day" role="figure"/>
    <div v-for="tag of tags" :key="tag.key">
      <user-tags-by-level-chart :tag="tag" class="mb-3" />
    </div>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import LevelBreakdownMetric from '@/components/metrics/common/LevelBreakdownMetric';
  import NumUsersPerDay from '@/components/metrics/common/NumUsersPerDay';
  import UserTagsByLevelChart from '@/components/metrics/common/UserTagsByLevelChart';

  export default {
    name: 'MetricsOnSubjectPage',
    components: {
      NumUsersPerDay,
      LevelBreakdownMetric,
      SubPageHeader,
      UserTagsByLevelChart,
    },
    mounted() {
      const tags = [];
      const userPageTags = this.$store.getters.config.projectMetricsTagCharts;
      if (userPageTags) {
        const tagSections = JSON.parse(userPageTags);
        tagSections.forEach((section) => {
          tags.push({
            key: section.key, label: section.tagLabel,
          });
        });
      }
      this.tags = tags;
    },
    data() {
      return {
        tags: [],
        loading: true,
      };
    },
  };
</script>

<style scoped>

</style>
