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
import ExistingUserInput from '@/components/utils/ExistingUserInput.vue'
import UserRolesUtil from '@/components/utils/UserRolesUtil'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import Column from 'primevue/column'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { userErrorState } from '@/stores/UserErrorState.js'
import { useDialogMessages } from '@/components/utils/modal/UseDialogMessages.js'
import { useUpgradeInProgressErrorChecker } from '@/components/utils/errors/UseUpgradeInProgressErrorChecker.js'

const dialogMessages = useDialogMessages()
// role constants
const ROLE_APP_USER = 'ROLE_APP_USER';
const ROLE_PROJECT_ADMIN = 'ROLE_PROJECT_ADMIN';
const ROLE_SUPERVISOR = 'ROLE_SUPERVISOR';
const ROLE_SUPER_DUPER_USER = 'ROLE_SUPER_DUPER_USER';
const ROLE_PROJECT_APPROVER = 'ROLE_PROJECT_APPROVER';
const ROLE_DASHBOARD_ADMIN_ACCESS = 'ROLE_DASHBOARD_ADMIN_ACCESS'
const ROLE_ADMIN_GROUP_MEMBER = 'ROLE_ADMIN_GROUP_MEMBER'
const ROLE_ADMIN_GROUP_OWNER = 'ROLE_ADMIN_GROUP_OWNER'
const ALL_ROLES = [ROLE_APP_USER, ROLE_PROJECT_ADMIN, ROLE_SUPERVISOR, ROLE_SUPER_DUPER_USER, ROLE_PROJECT_APPROVER];

const appConfig = useAppConfig();
const announcer = useSkillsAnnouncer();
const userInfo = useUserInfo();
const colors = useColors()
const responsive = useResponsiveBreakpoints()
const errorState = userErrorState()
const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()

const emit = defineEmits(['role-added', 'role-deleted']);
const props = defineProps({
  projectId: {
    type: String,
    default: null,
  },
  title: {
    type: String,
    required: true,
  },
  roles: {
    type: Array,
    // eslint-disable-next-line vue/valid-define-props
    default: () => [ROLE_PROJECT_ADMIN, ROLE_PROJECT_APPROVER],
    // validator: (value) => (value.every((v) => ALL_ROLES.includes(v))),
  },
  roleDescription: {
    type: String,
    default: 'User',
  },
  userType: {
    type: String,
    default: 'DASHBOARD',
  },
  id: {
    type: String,
    default: 'add-user-div',
  },
  addRoleConfirmation: {
    type: Boolean,
    required: false,
    default: false,
  },
  adminGroupId: {
    type: String,
    default: null,
  },
});

onMounted(() => {
  loadData();
});


const sortInfo = ref({ sortOrder: 1, sortBy: 'userId' })
const possiblePageSizes = [ 5, 10, 15, 20]
const pageSize = ref(5)
let table = ref({
  options: {
    busy: true,
    pagination: {
      hideUnnecessary: true,
      server: false,
      currentPage: 1,
      totalRows: 1,
    },
    tableDescription: `${props.roleDescription} table`,
  },
});

const data = ref([]);
const userIds = computed(() => data.value.map((d) => d.userId));
const selectedUser = ref(null);
const isSaving = ref(false);
const errNotification = ref({
  enable: false,
  msg: '',
});
const userRole = ref({
  selected: null,
  options: props.roles.map((role) => ({ value: role, text: getRoleDisplay(role) })),
});

let addUsrBtnDisabled = computed(() => {
  return !(selectedUser.value && (userRole.value.selected || props.roles.length === 1));
});

let isOnlyOneRole = computed(() => {
  return props.roles.length === 1;
});

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

const loadData = () => {
  table.value.options.busy = true;
  const pageParams = {
    limit: 200,
    page: 1,
    ascending: sortInfo.value.sortOrder === 1,
    orderBy: sortInfo.value.sortBy
  };
  AccessService.getUserRoles(props.projectId, props.roles, pageParams, props.adminGroupId).then((result) => {
    table.value.options.busy = false;
    data.value = result.data;
    table.value.options.pagination.totalRows = result.totalCount;
  });
}

function addUserRole() {
  if (props.addRoleConfirmation) {
    const role = isOnlyOneRole.value ? props.roles[0] : userRole.value.selected;
    const isApproverRole = UserRolesUtil.isApproverRole(role);
    const msgText = isApproverRole
        ? 'The selected user will be added as an Approver for this project and will be able to view all aspects of the Project as well as approve and deny self reporting requests.'
        : 'The selected user will be added as an Administrator for this project and will be able to edit/add/delete all aspects of the Project.';
    const titleText = isApproverRole ? 'Add Project Approver?' : 'Add Project Administrator?';
    const okBtnText = isApproverRole ? 'Add Approver!' : 'Add Administrator!';

    dialogMessages.msgConfirm({
      message: msgText,
      header: titleText,
      acceptLabel: okBtnText,
      rejectLabel: 'Cancel',
      accept: () => {
        doAddUserRole();
      }
    })
  } else {
    doAddUserRole();
  }
}

function doAddUserRole() {
  isSaving.value = true;
  table.value.options.busy = true;
  const pkiAuthenticated = appConfig.isPkiAuthenticated;

  const role = isOnlyOneRole.value ? props.roles[0] : userRole.value.selected;
  AccessService.saveUserRole(props.projectId, selectedUser.value, role, pkiAuthenticated, props.adminGroupId).then(() => {
    announcer.polite(`${getRoleDisplay(role)} role was added for ${getUserDisplay({ ...selectedUser.value, firstName: selectedUser.value.first, lastName: selectedUser.value.last })}`);
    emit('role-added', { userId: selectedUser.value.userId, role });
    loadData();
  }).catch((e) => {
    handleError(e);
  }).finally(() => {
    isSaving.value = false;
    table.value.options.busy = false;
    selectedUser.value = null;
    userRole.value.selected = null;
  });
}

function handleError(e) {
  if (e.response.data && e.response.data.errorCode && (e.response.data.errorCode === 'UserNotFound' || e.response.data.errorCode === 'AccessDenied')) {
    errNotification.value.msg = e.response.data.explanation;
    errNotification.value.enable = true;
  } else if (upgradeInProgressErrorChecker.isUpgrading(e)) {
    upgradeInProgressErrorChecker.navToUpgradeInProgressPage()
  } else {
    const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
    errorState.navToErrorPage('Failed to add User Role', errorMessage)
  }
}

function getUserDisplay(item) {
  return item.lastName && item.firstName ? `${item.firstName} ${item.lastName} (${item.userIdForDisplay})` : item.userIdForDisplay;
}

function notCurrentUser(userId) {
  const currentUser = userInfo.userInfo.value.userId;
  return currentUser && userId !== currentUser;
}

function deleteUserRoleConfirm(row) {
  const msg = `Are you absolutely sure you want to remove ${getUserDisplay(row)} as a ${props.roleDescription}?`;

  dialogMessages.msgConfirm({
    message: msg,
    header: 'Please Confirm!',
    acceptLabel: 'YES, Delete It',
    accept: () => {
      deleteUserRole(row);
    }
  });
}

function deleteUserRole(row) {
  table.value.options.busy = true;
  AccessService.deleteUserRole(row.projectId, row.userId, row.roleName).then(() => {
    announcer.polite(`${getRoleDisplay(row.roleName)} role was removed from ${getUserDisplay(row)}`);
    data.value = data.value.filter((item) => item.userId !== row.userId);
    emit('role-deleted', { userId: row.userId, role: row.roleName });
    table.value.options.busy = false;
    table.value.options.pagination.totalRows = data.value.length;
  });
}

function editItem(item) {
  data.value = data.value.map((user) => {
    if (user.userId === item.userId) {
      return ({ ...user, existingRoleName: user.roleName, isEdited: true });
    }
    return ({ ...user, isEdited: false });
  });
}

function updateUserRole(selectedRole) {
  const newRole = selectedRole.value;
  const userRoleToUpdate = data.value.find((user) => user.isEdited);
  if (userRoleToUpdate && newRole !== userRoleToUpdate.existingRoleName) {
    // mark record as loading
    data.value = data.value.map((user) => {
      if (user.isEdited) {
        return ({
          ...user,
          isLoading: true,
        });
      }
      return user;
    });

    const pkiAuthenticated = appConfig.isPkiAuthenticated;
    AccessService.saveUserRole(props.projectId, userRoleToUpdate, newRole, pkiAuthenticated, props.adminGroupId)
        .then(() => {
          data.value = data.value.map((user) => {
            if (user.isEdited) {
              const copy = ({
                ...user,
                isEdited: false,
                isLoading: false,
                roleName: newRole,
                existingRoleName: newRole,
              });
              return copy;
            }
            return user;
          });
          announcer.polite(`User role was updated to ${getRoleDisplay(newRole)} for ${getUserDisplay(userRoleToUpdate)}`);
        }).catch((e) => {
      handleError(e);
    });
  }
}

defineExpose({
  loadData,
});
</script>

<template>
  <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
    <template #header>
      <SkillsCardHeader v-if="title" :title="title"></SkillsCardHeader>
    </template>
    <template #content>
      <slot name="underHeader"/>
      <div>
        <div class="p-3">
          <div>
            <existing-user-input :suggest="true" :validate="true" :user-type="userType" :excluded-suggestions="userIds"
                                 v-model="selectedUser" data-cy="existingUserInput" />
          </div>
          <div class="mt-3 mb-3 flex gap-2 flex-column sm:flex-row">
            <div v-if="!isOnlyOneRole" class="flex-1">
              <Dropdown class="w-full" v-model="userRole.selected" :options="userRole.options" data-cy="userRoleSelector"
                        placeholder="Please select user's Role" optionLabel="text" optionValue="value" />
            </div>
            <div>
              <SkillsButton variant="outline-hc" @click="addUserRole" :disabled="addUsrBtnDisabled" data-cy="addUserBtn"
                            label="Add User" id="addUserBtn" :track-for-focus="true"
                            :icon="isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right'">
              </SkillsButton>
            </div>
          </div>

          <Message v-if="errNotification.enable" severity="error" data-cy="error-msg">
            <strong>Error!</strong> Request could not be completed! {{ errNotification.msg }}
          </Message>
        </div>
        <SkillsDataTable
            :loading="table.options.busy"
          :value="data"
          :rowsPerPageOptions="possiblePageSizes"
          data-cy="roleManagerTable"
          tableStoredStateId="roleManagerTableSort"
          aria-label="User Roles"
          striped-rows
          paginator
          :pt:paginator:paginatorWrapper:aria-label='`${title} Paginator`'
          v-model:sort-field="sortInfo.sortBy"
          v-model:sort-order="sortInfo.sortOrder"
          :rows="pageSize">
          <Column :header="roleDescription" field="userId" sortable :class="{'flex': responsive.md.value }">
            <template #header>
              <span class="mr-2"><i class="fas fa-user skills-color-users" :class="colors.getTextClass(0)"
                                    aria-hidden="true"></i> </span>
            </template>
            <template #body="slotProps">
              <div :data-cy="`userCell_${slotProps.data.userId}`">
                {{ getUserDisplay(slotProps.data) }}
              </div>
            </template>
          </Column>
          <Column header="Role" field="roleName" sortable :class="{'flex': responsive.md.value }">
            <template #header>
              <span class="mr-2"><i class="fas fa-id-card text-danger" :class="colors.getTextClass(1)"
                                    aria-hidden="true"></i> </span>
            </template>
            <template #body="slotProps">
              <div v-if="!slotProps.data.isEdited">{{ getRoleDisplay(slotProps.data.roleName) }}</div>
              <Dropdown v-else v-model="slotProps.data.roleName"
                        :options="userRole.options"
                        optionLabel="text"
                        optionValue="value"
                        :aria-label="`select new access role for user ${getUserDisplay(slotProps.data)}`"
                        :data-cy="`roleDropDown_${slotProps.data.userId}`"
                        @change="updateUserRole">
              </Dropdown>
            </template>
          </Column>
          <Column :class="{'flex': responsive.md.value }">
            <template #header>
              <span class="sr-only">Controls - Not sortable</span>
            </template>
            <template #body="slotProps">
              <div>
                <div class="float-right mr-1 flex gap-2" :data-cy="`controlsCell_${slotProps.data.userId}`">
                  <!--            <i v-if="!notCurrentUser(slotProps.data.userId)"-->
                  <!--               data-cy="cannotRemoveWarning"-->
                  <!--               class="text-warning fas fa-exclamation-circle mr-1"-->
                  <!--               aria-hidden="true"/>-->

                  <SkillsButton v-if="!isOnlyOneRole" @click="editItem(slotProps.data)"
                                :disabled="!notCurrentUser(slotProps.data.userId)"
                                :aria-label="`edit access role from user ${getUserDisplay(slotProps.data)}`"
                                data-cy="editUserBtn" icon="fas fa-edit" label="Edit" size="small">
                  </SkillsButton>
                  <SkillsButton @click="deleteUserRoleConfirm(slotProps.data)"
                                :disabled="!notCurrentUser(slotProps.data.userId)"
                                id="removeUserBtn"
                                :track-for-focus="true"
                                :aria-label="`remove access role from user ${getUserDisplay(slotProps.data)}`"
                                data-cy="removeUserBtn" icon="fas fa-trash" label="Delete" size="small">
                  </SkillsButton>

                </div>
                <InlineMessage v-if="!notCurrentUser(slotProps.data.userId)" class="mt-1" severity="info" aria-live="polite">
                  Can't remove or edit yourself
                </InlineMessage>
              </div>
            </template>
          </Column>
          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ data.length }}</span>
          </template>
          <template #empty>
            <span class="flex align-items-center justify-content-center">There are no records to show</span>
          </template>
        </SkillsDataTable>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>