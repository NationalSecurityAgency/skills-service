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
  <loading-container v-bind:is-loading="isLoading">
    <sub-page-header title="Access Management"/>
    <div class="card">
      <div class="card-header">
        Project Administrators
      </div>
      <div class="card-body">
        <role-manager :project="project"/>
      </div>
    </div>

    <trusted-client-props v-if="showTrustedClientProps" :project="project" class="my-4"/>
  </loading-container>
</template>

<script>
  import RoleManager from './RoleManager';
  import TrustedClientProps from './TrustedClientProps';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import ProjectService from '../projects/ProjectService';
  import LoadingContainer from '../utils/LoadingContainer';

  export default {
    name: 'AccessSettings',
    components: {
      LoadingContainer,
      SubPageHeader,
      RoleManager,
      TrustedClientProps,
    },
    data() {
      return {
        isLoading: true,
        project: {},
      };
    },
    computed: {
      showTrustedClientProps() {
        return (!this.$store.getters.isPkiAuthenticated && !this.$store.getters.config.oAuthOnly);
      },
    },
    mounted() {
      ProjectService.getProjectDetails(this.$route.params.projectId)
        .then((res) => {
          this.project = res;
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
  };
</script>

<style scoped>

</style>
