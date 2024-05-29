<script setup>
import { ref } from 'vue';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import RoleManager from '@/components/access/RoleManager.vue';

const root = ref({
  role: 'ROLE_SUPER_DUPER_USER',
      roleDescription: 'Root User',
      userType: 'ROOT',
});

const supervisor = ref({
  role: 'ROLE_SUPERVISOR',
      roleDescription: 'Supervisor User',
      userType: 'SUPERVISOR',
});

const supervisorRoleManager = ref();

const handleRootRoleChanged = () => {
  supervisorRoleManager.value.loadData();
};

const handleRoleAdded = (event) => {
  // if ($store.getters.userInfo && event.userId === $store.getters.userInfo.userId && event.role === supervisor.role) {
  //   $store.commit('access/supervisor', true);
  // }
};

const handleRoleDeleted = (event) => {
  // if ($store.getters.userInfo && event.userId === $store.getters.userInfo.userId && event.role === supervisor.role) {
  //   $store.commit('access/supervisor', false);
  // }
};
</script>

<template>
  <div>
    <sub-page-header title="Security Settings" />

    <role-manager id="add-root-user"
                  title="Root Users Management"
                  ref="rootUserRoleManager"
                  :roles="['ROLE_SUPER_DUPER_USER']"
                  @role-added="handleRootRoleChanged"
                  @role-deleted="handleRootRoleChanged"
                  data-cy="rootrm" :user-type="root.userType" :role-description="root.roleDescription" />

    <role-manager id="add-supervisor-user"
                  title="Supervisor Users Management"
                  ref="supervisorRoleManager"
                  data-cy="supervisorrm"
                  class="mt-3"
                  :roles="['ROLE_SUPERVISOR']"
                  :user-type="supervisor.userType"
                  :role-description="supervisor.roleDescription"
                  @role-added="handleRoleAdded"
                  @role-deleted="handleRoleDeleted" />
  </div>
</template>

<style scoped></style>
