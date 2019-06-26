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

    <trusted-client-props v-if="!$store.getters.isPkiAuthenticated" :project="project" class="my-4"/>
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
