<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column">
        <span class="title">Access Settings</span>
      </div>
    </div>
    <loading-container v-bind:is-loading="isLoading">
      <role-manager :role="role" :user-type="userType" :role-description="roleDescription" v-if="isRoot" />
    </loading-container>
  </div>
</template>

<script>
  import LoadingContainer from '../utils/LoadingContainer';
  import RoleManager from '../access/RoleManager';
  import SettingsService from './SettingsService';

  export default {
    name: 'SecuritySettings',
    components: { RoleManager, LoadingContainer },
    data() {
      return {
        isLoading: true,
        isRoot: false,
        role: 'ROLE_SUPER_DUPER_ADMIN',
        roleDescription: 'Root User',
        userType: 'ROOT',
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        SettingsService.hasRoot().then((response) => {
          this.isRoot = response;
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
