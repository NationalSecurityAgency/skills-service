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
    <num-users-per-day class="my-3" title="Subject's users per day"/>
  </div>
</template>

<script>
  import SubPageHeader from '@//components/utils/pages/SubPageHeader';
  import LevelBreakdownMetric from '../common/LevelBreakdownMetric';
  import NumUsersPerDay from '../common/NumUsersPerDay';

  export default {
    name: 'MetricsOnSubjectPage',
    components: {
      NumUsersPerDay,
      LevelBreakdownMetric,
      SubPageHeader,
    },
    data() {
      return {
        loading: true,
      };
    },
  };
</script>

<style scoped>

</style>
