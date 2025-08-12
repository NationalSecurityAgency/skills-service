/*
Copyright 2024 SkillTree

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
<script setup>
import { ref } from 'vue';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import RoleManager from '@/components/access/RoleManager.vue';
import LimitDashboardAccess from '@/components/settings/LimitDashboardAccess.vue'

const root = ref({
  role: 'ROLE_SUPER_DUPER_USER',
      roleDescription: 'Root User',
      userType: 'ROOT',
});

</script>

<template>
  <div>
    <sub-page-header title="Security Settings" />

    <role-manager id="add-root-user"
                  title="Root Users Management"
                  ref="rootUserRoleManager"
                  :roles="['ROLE_SUPER_DUPER_USER']"
                  data-cy="rootrm" :user-type="root.userType" :role-description="root.roleDescription" />

    <limit-dashboard-access class="mt-4" />

  </div>
</template>

<style scoped></style>
