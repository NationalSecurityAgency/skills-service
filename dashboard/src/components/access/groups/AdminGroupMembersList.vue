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
import { computed, onMounted, ref } from 'vue'
import AccessService from '@/components/access/AccessService.js'
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import Column from 'primevue/column';
import { useColors } from '@/skills-display/components/utilities/UseColors.js';
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';

const ROLE_APP_USER = 'ROLE_APP_USER';
const ROLE_PROJECT_ADMIN = 'ROLE_PROJECT_ADMIN';
const ROLE_SUPERVISOR = 'ROLE_SUPERVISOR';
const ROLE_SUPER_DUPER_USER = 'ROLE_SUPER_DUPER_USER';
const ROLE_PROJECT_APPROVER = 'ROLE_PROJECT_APPROVER';
const ROLE_DASHBOARD_ADMIN_ACCESS = 'ROLE_DASHBOARD_ADMIN_ACCESS'
const ROLE_ADMIN_GROUP_MEMBER = 'ROLE_ADMIN_GROUP_MEMBER'
const ROLE_ADMIN_GROUP_OWNER = 'ROLE_ADMIN_GROUP_OWNER'

const props = defineProps({
  adminGroupId: {
    type: String,
  },
  roleDescription: {
    type: String,
    default: 'User',
  },
});

const colors = useColors()
const responsive = useResponsiveBreakpoints()

const isLoading = ref(true);
const groupMemberUserRoles = ref([])

onMounted(() => {
  loadAdminGroupMembers(props.adminGroupId);
});

function getUserDisplay(item) {
  return item.lastName && item.firstName ? `${item.firstName} ${item.lastName} (${item.userIdForDisplay})` : item.userIdForDisplay;
}

function getRoleDisplay(roleName) {
  if (roleName === ROLE_PROJECT_ADMIN) {
    return 'Administrator';
  }
  if (roleName === ROLE_APP_USER) {
    return 'Skills Display';
  }
  if (roleName === ROLE_SUPERVISOR) {
    return 'Supervisor';
  }
  if (roleName === ROLE_SUPER_DUPER_USER) {
    return 'Root';
  }
  if (roleName === ROLE_PROJECT_APPROVER) {
    return 'Approver';
  } if (roleName === ROLE_DASHBOARD_ADMIN_ACCESS) {
    return 'Training Creator';
  } if (roleName === ROLE_ADMIN_GROUP_MEMBER) {
    return 'Group Member';
  } if (roleName === ROLE_ADMIN_GROUP_OWNER) {
    return 'Group Owner';
  }
  return 'Unknown';
}

const loadAdminGroupMembers = (adminGroupId) => {
  // table.value.options.busy = true;
  const pageParams = {
    limit: 200,
    page: 1,
    ascending: true,
    orderBy: 'userId'
  };
  AccessService.getUserRoles(null, ['ROLE_ADMIN_GROUP_MEMBER', 'ROLE_ADMIN_GROUP_OWNER'], pageParams, adminGroupId).then((result) => {
    groupMemberUserRoles.value = result.data
    isLoading.value = false;
  });
}
const groupMembers = computed(() => {
  return groupMemberUserRoles.value.map((u) => getUserDisplay(u));//.join(', ');
})
</script>

<template>
<!--  <DataTable :value="groupMemberUserRoles" tableStyle="min-width: 50rem">-->
<!--    <Column :header="roleDescription" field="userId" sortable :class="{'flex': responsive.md.value }">-->
<!--      <template #header>-->
<!--              <span class="mr-2"><i class="fas fa-user skills-color-users" :class="colors.getTextClass(0)"-->
<!--                                    aria-hidden="true"></i> </span>-->
<!--      </template>-->
<!--      <template #body="slotProps">-->
<!--        <div :data-cy="`userCell_${slotProps.data.userId}`">-->
<!--          {{ getUserDisplay(slotProps.data) }}-->
<!--        </div>-->
<!--      </template>-->
<!--    </Column>-->
<!--    <Column header="Role" field="roleName" sortable :class="{'flex': responsive.md.value }">-->
<!--      <template #header>-->
<!--              <span class="mr-2"><i class="fas fa-id-card text-danger" :class="colors.getTextClass(1)"-->
<!--                                    aria-hidden="true"></i> </span>-->
<!--      </template>-->
<!--      <template #body="slotProps">-->
<!--        <div>-->
<!--          <div>{{ getRoleDisplay(slotProps.data.roleName) }}</div>-->
<!--        </div>-->
<!--      </template>-->
<!--    </Column>-->
<!--  </DataTable>-->

  <loading-container :is-loading="isLoading" class="">
    <ul>
      <li v-for="groupMember in groupMembers" class="m-2">
        {{ groupMember }}
      </li>
    </ul>
  </loading-container>

<!--  <div>-->
<!--    {{ groupMembers.join(', ') }}-->
<!--  </div>-->
</template>

<style>
</style>