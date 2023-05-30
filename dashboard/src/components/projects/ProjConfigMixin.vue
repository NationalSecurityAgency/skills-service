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
<script>
  import UserRolesUtil from '@/components/utils/UserRolesUtil';

  export default {
    name: 'ProjConfigMixin',
    methods: {
      loadProjConfig() {
        return this.$store.dispatch('afterProjConfigStateLoaded').then((projConfig) => projConfig);
      },
      isReadOnlyProjMethod() {
        return this.$store.getters.projConfig && UserRolesUtil.isReadOnlyProjRole(this.$store.getters.projConfig.user_project_role);
      },
    },
    computed: {
      isLoadingProjConfig() {
        return this.$store.getters.loadingProjConfig;
      },
      projConfig() {
        return this.$store.getters.projConfig;
      },
      isProjConfigInviteOnly() {
        return this.$store.getters.projConfig && this.$store.getters.projConfig.invite_only === 'true';
      },
      isProjConfigDiscoverable() {
        return this.$store.getters.projConfig && this.$store.getters.projConfig['production.mode.enabled'] === 'true';
      },
      projConfigRootHelpUrl() {
        return this.$store.getters.projConfig && this.$store.getters.projConfig['help.url.root'];
      },
      isReadOnlyProj() {
        return this.isReadOnlyProjMethod();
      },
      userProjRole() {
        return this.$store.getters.projConfig && this.$store.getters.projConfig.user_project_role;
      },
    },
  };
</script>

<style scoped>

</style>
