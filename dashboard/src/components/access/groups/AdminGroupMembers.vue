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