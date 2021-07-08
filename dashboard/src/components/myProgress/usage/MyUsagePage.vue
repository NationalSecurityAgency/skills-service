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
    <div class="container-fluid">
      <sub-page-header title="My Usage" class="pt-4">
      </sub-page-header>

      <b-row class="my-4">
        <b-col class="my-summary-card">
          <event-history-chart :availableProjects="projects"></event-history-chart>
        </b-col>
      </b-row>
    </div>
</template>

<script>
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import EventHistoryChart from './EventHistoryChart';

  export default {
    name: 'MyUsagePage',
    components: { EventHistoryChart, SubPageHeader },
    props: ['projects'],
  };
</script>

<style scoped>

</style>
