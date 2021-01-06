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
    <sub-page-header title="My Skills"/>

    <skills-spinner :is-loading="loading" />
    <div v-if="!loading">
      <training-profile-comparator class="mb-3" :available-projects="projects"/>
      <multiple-proj-users-in-common :available-projects="projects"/>
    </div>
  </div>
</template>

<script>
  import SubPageHeader from './utils/pages/SubPageHeader';
  import MultipleProjUsersInCommon from './metrics/multipleProjects/MultipleProjUsersInCommon';
  import TrainingProfileComparator from './metrics/multipleProjects/TrainingProfileComparator';
  import SupervisorService from './utils/SupervisorService';
  import SkillsSpinner from './utils/SkillsSpinner';

  export default {
    name: 'MySkillsPage',
    components: {
      SkillsSpinner,
      TrainingProfileComparator,
      MultipleProjUsersInCommon,
      SubPageHeader,
    },
    data() {
      return {
        loading: true,
        projects: [],
      };
    },
    mounted() {
      this.loadProjects();
    },
    methods: {
      loadProjects() {
        SupervisorService.getAllProjects()
          .then((res) => {
            this.projects = res;
          }).finally(() => {
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
