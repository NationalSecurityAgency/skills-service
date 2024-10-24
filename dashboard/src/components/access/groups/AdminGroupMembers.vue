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

import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import { useRoute } from 'vue-router';
import { computed } from 'vue';
import RoleManager from '@/components/access/RoleManager.vue';
import { useAdminGroupState } from '@/stores/UseAdminGroupState.js';

const route = useRoute()
const adminGroupState = useAdminGroupState()

const adminGroupId = computed(() => route.params.adminGroupId)

const updateMemberCount = (event, updateValue) => {
  if (event.role === 'ROLE_ADMIN_GROUP_MEMBER') {
    adminGroupState.adminGroup.numberOfMembers += updateValue
  } else if (event.role === 'ROLE_ADMIN_GROUP_OWNER') {
    adminGroupState.adminGroup.numberOfOwers += updateValue
  }
}
</script>

<template>
  <div>
    <sub-page-header title="Group Members" />
      <role-manager id="add-group-admin-access-user"
                    :admin-group-id="adminGroupId"
                    title=""
                    role-description="Group Member"
                    data-cy="adminGroupMemberRoleManager"
                    @role-added="updateMemberCount($event, 1)"
                    @role-deleted="updateMemberCount($event, -1)"
                    :roles="['ROLE_ADMIN_GROUP_MEMBER', 'ROLE_ADMIN_GROUP_OWNER']">
      </role-manager>
  </div>
</template>

<style scoped>

</style>