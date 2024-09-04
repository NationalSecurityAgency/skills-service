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
import RoleManager from '@/components/access/RoleManager.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const appConfig = useAppConfig()
</script>

<template>
  <role-manager v-if="appConfig.limitAdminAccess"
                id="add-admin-access-user"
                title="Training Creators Management"
                data-cy="limitAdminDashboardAccessRoleManager"
                :roles="['ROLE_DASHBOARD_ADMIN_ACCESS']">
    <template #underHeader>
      <Message :closable="false" class="mx-2">Only users with
        <Tag>Training Creator</Tag>
        role will be able to access administrative portion of SkillTree Dashboard.
      </Message>
    </template>
  </role-manager>
</template>

<style scoped>

</style>