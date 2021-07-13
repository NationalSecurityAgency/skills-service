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

      <skills-spinner :is-loading="loading" />
      <b-row v-if="!loading" class="my-4">
        <b-col class="my-summary-card">
          <event-history-chart :availableProjects="projects"></event-history-chart>
        </b-col>
      </b-row>
    </div>
</template>

<script>
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import EventHistoryChart from './EventHistoryChart';
  import MyProgressService from '../MyProgressService';
  import SkillsSpinner from '../../utils/SkillsSpinner';

  export default {
    name: 'MyUsagePage',
    components: { SkillsSpinner, EventHistoryChart, SubPageHeader },
    data() {
      return {
        loading: true,
        projects: [],
      };
    },
    mounted() {
      if (this.$route.params.projects) {
        this.projects = this.$route.params.projects;
        this.loading = false;
      } else {
        this.loadProjects();
      }
    },
    methods: {
      loadProjects() {
        MyProgressService.loadMyProgressSummary()
          .then((res) => {
            this.myProgressSummary = res;
            this.projects = this.myProgressSummary.projectSummaries;
          }).finally(() => {
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
