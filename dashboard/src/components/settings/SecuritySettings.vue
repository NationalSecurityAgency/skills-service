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
    <sub-page-header title="Security Settings"/>

    <div class="card">
      <div class="card-header">Root Users Management</div>
      <div class="card-body">
        <role-manager data-cy="rootrm" :role="root.role" :user-type="root.userType" :role-description="root.roleDescription" />
      </div>
    </div>

    <div class="card mt-2">
      <div class="card-header">Supervisor Users Management</div>
      <div class="card-body">
        <role-manager data-cy="supervisorrm" :role="supervisor.role"
                      :user-type="supervisor.userType"
                      :role-description="supervisor.roleDescription"
                      @role-added="handleRoleAdded"
                      @role-deleted="handleRoleDeleted"/>
      </div>
    </div>
  </div>
</template>

<script>
  import RoleManager from '../access/RoleManager';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'SecuritySettings',
    components: { SubPageHeader, RoleManager },
    data() {
      return {
        root: {
          role: 'ROLE_SUPER_DUPER_USER',
          roleDescription: 'Root User',
          userType: 'ROOT',
        },
        supervisor: {
          role: 'ROLE_SUPERVISOR',
          roleDescription: 'Supervisor User',
          userType: 'SUPERVISOR',
        },
      };
    },
    methods: {
      handleRoleAdded(event) {
        if (this.$store.getters.userInfo
          && event.userId === this.$store.getters.userInfo.userId
          && event.role === this.supervisor.role) {
          this.$store.commit('access/supervisor', true);
        }
      },
      handleRoleDeleted(event) {
        if (this.$store.getters.userInfo
          && event.userId === this.$store.getters.userInfo.userId
          && event.role === this.supervisor.role) {
          this.$store.commit('access/supervisor', false);
        }
      },
    },
  };
</script>

<style scoped>

</style>
