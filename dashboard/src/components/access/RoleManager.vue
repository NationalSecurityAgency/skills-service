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
import AdminGroupsService from '@/components/access/groups/AdminGroupsService.js';
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
import QuizService from '@/components/quiz/QuizService.js';
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';

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
  quizId: {
    type: String,
    default: null,
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
const isLoading = ref(true)

const expandedRows = ref([])
const assignedLocalAdmins = ref([])
const assignedAdminGroups = ref([])
const allAdminGroups = ref([])
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
const removeRoleInfo = ref({
  showDialog: false,
  userInfo: {}
})
const data = computed(() => {
  const groupRows = adminGroupsSupported.value ? assignedAdminGroups.value.map((g) => {
    return {
      ...g,
      userId: g.adminGroupId,
      userIdForDisplay: g.name,
      roleName: ROLE_PROJECT_ADMIN,
    }
  }) : [];
  return assignedLocalAdmins.value.concat(groupRows);
})

const adminGroupsSupported = computed(() => {
  return !props.adminGroupId && (!!props.projectId || !!props.quizId);
})
const availableAdminGroups = computed(() => {
  const assignedAdminGroupIds = assignedAdminGroups.value.map((ag) => ag.adminGroupId);
  return allAdminGroups.value.filter((ag) => !assignedAdminGroupIds.includes(ag.adminGroupId));
})
const areAdminGroupsAvailable = computed(() => {
  return availableAdminGroups.value && availableAdminGroups.value.length > 0;
})
const areAdminGroupsAssigned = computed(() => {
  return assignedAdminGroups.value && assignedAdminGroups.value.length > 0;
})
const emptyAdminGroupsMessage = computed(() => {
  if (areAdminGroupsAvailable.value) {
    return 'No results. Please refine your search string.'
  } else {
    if (areAdminGroupsAssigned.value) {
      return 'All of your available admin groups have already been assigned.'
    }
    return 'You currently do not administer any admin groups.'
  }
})

const assignedUserIds = computed(() => {
  const assignedUserIds = new Set(assignedLocalAdmins.value.map((u) => appConfig.isPkiAuthenticated ? u.userIdForDisplay : u.userId));
  assignedAdminGroups.value.forEach((ag) => {
    ag.allMembers.forEach((u) => {
      appConfig.isPkiAuthenticated ? assignedUserIds.add(u.userIdForDisplay) : assignedUserIds.add(u.userId);
    })
  })
  return Array.from(assignedUserIds);
})

const addUsrBtnDisabled = computed(() => {
  return !(selectedUser.value && (userRole.value.selected || props.roles.length === 1));
});

const isOnlyOneRole = computed(() => {
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
  isLoading.value = true;
  const pageParams = {
    limit: 200,
    page: 1,
    ascending: sortInfo.value.sortOrder === 1,
    orderBy: sortInfo.value.sortBy
  };
  const getUserRoles = props.quizId ?
      QuizService.getQuizUserRoles(props.quizId).then((result) => {
        assignedLocalAdmins.value = adminGroupsSupported.value ? result.filter((u) => !u.adminGroupId) : result;
      }) :
      AccessService.getUserRoles(props.projectId, props.roles, pageParams, props.adminGroupId).then((result) => {
        assignedLocalAdmins.value = adminGroupsSupported.value ? result.data.filter((u) => !u.adminGroupId) : result.data
      });
  const getAdminGroupsForProject = props.projectId ? AdminGroupsService.getAdminGroupsForProject(props.projectId).then((result) => {
    assignedAdminGroups.value = result;
  }) : Promise.resolve();

  const getAdminGroupsForQuiz = props.quizId ? AdminGroupsService.getAdminGroupsForQuiz(props.quizId).then((result) => {
    assignedAdminGroups.value = result;
  }) : Promise.resolve();

  const getOwnedAdminGroups = adminGroupsSupported.value ? AdminGroupsService.getAdminGroupDefs().then((result) => {
    allAdminGroups.value = result;
  }) : Promise.resolve();

  Promise.all([getUserRoles, getAdminGroupsForProject, getAdminGroupsForQuiz, getOwnedAdminGroups]).then(() => {
    isLoading.value = false;
  })
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
  isLoading.value = true;
  const pkiAuthenticated = appConfig.isPkiAuthenticated;
  const role = isOnlyOneRole.value ? props.roles[0] : userRole.value.selected;
  if (props.quizId) {
    addQuizUserRole(role);
  } else {
    AccessService.saveUserRole(props.projectId, selectedUser.value, role, pkiAuthenticated, props.adminGroupId).then(() => {
      completeAddRole(role)
    }).catch((e) => {
      handleError(e);
    }).finally(() => {
      isSaving.value = false;
      isLoading.value = false;
      selectedUser.value = null;
      userRole.value.selected = null;
    });
  }
}
const addQuizUserRole = (role) => {
  const userIdParam = appConfig.isPkiAuthenticated ? selectedUser.value.dn : selectedUser.value.userId
  QuizService.addQuizAdmin(props.quizId, userIdParam).then(() => {
    completeAddRole(role)
  }).catch((e) => {
    handleError(e);
  }).finally(() => {
    isSaving.value = false;
    isLoading.value = false;
    selectedUser.value = null;
    userRole.value.selected = null;
  });
}
const completeAddRole = (role) => {
  announcer.polite(`${getRoleDisplay(role)} role was added for ${getUserDisplay({ ...selectedUser.value, firstName: selectedUser.value.first, lastName: selectedUser.value.last })}`);
  emit('role-added', { userId: selectedUser.value.userId, role });
  loadData();
}

function handleError(e) {
  if (e.response.data && e.response.data.errorCode && (e.response.data.errorCode === 'UserNotFound' || e.response.data.errorCode === 'AccessDenied')) {
    errNotification.value.msg = e.response.data.explanation;
    errNotification.value.enable = true;
  } else if (upgradeInProgressErrorChecker.isUpgrading(e)) {
    upgradeInProgressErrorChecker.navToUpgradeInProgressPage()
  } else {
    const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
    errorState.navToErrorPage('Failed to add User Role or Admin Group', errorMessage)
  }
}

function getUserDisplay(item) {
  return item.lastName && item.firstName ? `${item.firstName} ${item.lastName} (${item.userIdForDisplay})` : item.userIdForDisplay;
}

function notCurrentUser(userId) {
  const currentUser = userInfo?.userInfo?.value?.userId;
  return currentUser && userId !== currentUser;
}

function deleteUserRoleConfirm(row) {
  removeRoleInfo.value.userInfo = row
  removeRoleInfo.value.showDialog = true
}

function doDeleteUserRole() {
  isLoading.value = true;
  const row = removeRoleInfo.value.userInfo;
  if (props.quizId) {
    deleteQuizAdminUserRole(row)
  } else {
    AccessService.deleteUserRole(row.projectId, row.userId, row.roleName, props.adminGroupId).then(() => {
      completeDelete(row)
    });
  }
}
const deleteQuizAdminUserRole = (row) => {
  QuizService.deleteQuizAdmin(props.quizId, appConfig.isPkiAuthenticated ? row.dn : row.userId)
      .then(() => {
        completeDelete(row)
      })
}
const completeDelete = (row) => {
  announcer.polite(`${getRoleDisplay(row.roleName)} role was removed from ${getUserDisplay(row)}`);
  assignedLocalAdmins.value = assignedLocalAdmins.value.filter((item) => item.userId !== row.userId);
  emit('role-deleted', { userId: row.userId, role: row.roleName });
  isLoading.value = false;
  document.getElementById('existingUserInput').firstElementChild.focus()
}

function editItem(item) {
  assignedLocalAdmins.value = assignedLocalAdmins.value.map((user) => {
    if (user.userId === item.userId) {
      return ({ ...user, existingRoleName: user.roleName, isEdited: true });
    }
    return ({ ...user, isEdited: false });
  });
}

function updateUserRole(selectedRole) {
  const newRole = selectedRole.value;
  const userRoleToUpdate = assignedLocalAdmins.value.find((user) => user.isEdited);
  if (userRoleToUpdate && newRole !== userRoleToUpdate.existingRoleName) {
    // mark record as loading
    assignedLocalAdmins.value = assignedLocalAdmins.value.map((user) => {
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
          assignedLocalAdmins.value = assignedLocalAdmins.value.map((user) => {
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

const addProjectOrQuizToAdminGroup = (adminGroup) => {
  isLoading.value = true;
  if (props.projectId) {
    AdminGroupsService.addProjectToAdminGroup(adminGroup.adminGroupId, props.projectId).then(() => {
      announcer.polite(`Admin Group ${adminGroup.name} was added to project successfully`);
      loadData();
    }).catch((e) => {
      handleError(e);
    }).finally(() => {
      isLoading.value = false;
    })
  } else {
    AdminGroupsService.addQuizToAdminGroup(adminGroup.adminGroupId, props.quizId).then(() => {
      announcer.polite(`Admin Group ${adminGroup.name} was added to quiz successfully`);
      loadData();
    }).catch((e) => {
      handleError(e);
    }).finally(() => {
      isLoading.value = false;
    })
  }
}

const showExpansion = (row) => {
  return (!row.adminGroupId) ? 'no-expansion' : ''
}

defineExpose({
  loadData,
});
</script>

<template>
  <div>
    <Card :pt="{ body: { class: '!p-0' } }">
      <template #header>
        <SkillsCardHeader v-if="title" :title="title"></SkillsCardHeader>
      </template>
      <template #content>
        <slot name="underHeader"/>
        <div>
          <div class="p-4">
            <div>
              <existing-user-input :suggest="true" :validate="true" :user-type="userType"
                                   :excluded-suggestions="assignedUserIds"
                                   v-model="selectedUser" data-cy="existingUserInput"/>
            </div>
            <div class="mt-4 mb-4 flex gap-2 flex-col sm:flex-row">
              <div v-if="!isOnlyOneRole" class="flex-1">
                <Select class="w-full" v-model="userRole.selected" :options="userRole.options"
                          data-cy="userRoleSelector"
                          placeholder="Please select user's Role" optionLabel="text" optionValue="value"/>
              </div>
              <div>
                <SkillsButton variant="outline-hc" @click="addUserRole" :disabled="addUsrBtnDisabled"
                              data-cy="addUserBtn"
                              label="Add User" id="addUserBtn" :track-for-focus="true"
                              :icon="isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right'">
                </SkillsButton>
              </div>
            </div>
            <div v-if="adminGroupsSupported">
              <div class="text-center my-6 font-semibold">
                OR
              </div>
              <div>
                <Select
                    class="w-full"
                    :aria-label="`Select Admin Group to assign to this ${props.projectId ? 'Project' : 'Quiz'}`"
                    data-cy="adminGroupSelector"
                    showClear
                    filter
                    placeholder="Please select Admin Group"
                    optionLabel="name"
                    @update:modelValue="addProjectOrQuizToAdminGroup"
                    :emptyMessage=emptyAdminGroupsMessage
                    :options="availableAdminGroups">
                  <template #value="slotProps">
                    <div v-if="slotProps.value" class="p-1"
                         :data-cy="`adminGroupSelected-${slotProps.value.adminGroupId}`">
                      <span class="ml-1">{{ slotProps.value.name }}</span>
                    </div>
                    <span v-else> Search available admin groups...</span>
                  </template>
                  <template #option="slotProps">
                    <div :data-cy="`availableAdminGroupSelection-${slotProps.option.adminGroupId}`">
                      <span class="h6 ml-2">{{ slotProps.option.name }}</span>
                    </div>
                  </template>
                </Select>
              </div>
            </div>

            <Message v-if="errNotification.enable" severity="error" data-cy="error-msg">
              <strong>Error!</strong> Request could not be completed! {{ errNotification.msg }}
            </Message>
          </div>
          <SkillsDataTable
              :loading="isLoading"
              :value="data"
              :rowsPerPageOptions="possiblePageSizes"
              data-cy="roleManagerTable"
              tableStoredStateId="roleManagerTableSort"
              aria-label="User Roles"
              striped-rows
              paginator
              :pt:paginator:paginatorWrapper:aria-label='`${title} Paginator`'
              :row-class="showExpansion"
              v-model:expandedRows="expandedRows"
              v-model:sort-field="sortInfo.sortBy"
              v-model:sort-order="sortInfo.sortOrder"
              :rows="pageSize">
            <Column v-if="adminGroupsSupported" expander style="width: 5rem" :showFilterMenu="false"
                    :class="{'flex': responsive.md.value }">
              <template #header>
                <span class="sr-only">Rows expand and collapse control - Not sortable</span>
              </template>
              <template #filter>
                <span class="sr-only">Rows expand and collapse control - No filtering</span>
              </template>
            </Column>
            <Column :header="roleDescription" field="userId" sortable :class="{'flex': responsive.md.value }">
              <template #header>
              <span class="mr-2"><i class="fas fa-user skills-color-users" :class="colors.getTextClass(0)"
                                    aria-hidden="true"></i> </span>
              </template>
              <template #body="slotProps">
                <div v-if="slotProps.data.adminGroupId && !props.adminGroupId">
                  <i class="fas fa-layer-group" aria-hidden="true"></i> <span class="uppercase">Admin Group</span>
                  <Tag class="uppercase ml-2" data-cy="numMembersInGroup">
                    {{ slotProps.data.numberOfMembers + slotProps.data.numberOfOwners }}
                    member{{ slotProps.data.numberOfMembers + slotProps.data.numberOfOwners !== 1 ? 's' : '' }}
                  </Tag>
                </div>
                <div :data-cy="`userCell_${slotProps.data.userId}`">
                  {{ getUserDisplay(slotProps.data) }}
                </div>
              </template>
            </Column>
            <Column header="Role" v-if="!props.quizId" field="roleName" sortable
                    :class="{'flex': responsive.md.value }">
              <template #header>
              <span class="mr-2"><i class="fas fa-id-card text-danger" :class="colors.getTextClass(1)"
                                    aria-hidden="true"></i> </span>
              </template>
              <template #body="slotProps">
                <div v-if="!slotProps.data.isEdited">{{ getRoleDisplay(slotProps.data.roleName) }}</div>
                <Select v-else v-model="slotProps.data.roleName"
                          :options="userRole.options"
                          optionLabel="text"
                          optionValue="value"
                          :aria-label="`select new access role for user ${getUserDisplay(slotProps.data)}`"
                          :data-cy="`roleDropDown_${slotProps.data.userId}`"
                          @change="updateUserRole">
                </Select>
              </template>
            </Column>
            <Column :class="{'flex': responsive.md.value }">
              <template #header>
                <span class="sr-only">Controls - Not sortable</span>
              </template>
              <template #body="slotProps">
                <div v-if="!slotProps.data.adminGroupId || props.adminGroupId">
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
                                  :id="`removeUserBtn_${slotProps.data.userId}`"
                                  :track-for-focus="true"
                                  :aria-label="`remove access role from user ${getUserDisplay(slotProps.data)}`"
                                  data-cy="removeUserBtn" icon="fas fa-trash" label="Delete" size="small">
                    </SkillsButton>

                  </div>
                  <InlineMessage v-if="!notCurrentUser(slotProps.data.userId)" class="mt-1" severity="info" size="small"
                                 aria-live="polite">
                    Can't remove or edit yourself
                  </InlineMessage>
                </div>
              </template>
            </Column>
            <template #expansion="slotProps">
              <ul data-cy="userGroupMembers">
                <li v-for="groupMember in slotProps.data.allMembers" class="m-2"
                    :data-cy="`userGroupMember_${groupMember.userId}`">
                  {{ getUserDisplay(groupMember) }}
                </li>
              </ul>
            </template>
            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{
                data.length
              }}</span>
            </template>
            <template #empty>
              <span class="flex items-center justify-center">There are no records to show</span>
            </template>
          </SkillsDataTable>
        </div>
      </template>
    </Card>

    <RemovalValidation
        v-if="removeRoleInfo.showDialog"
        v-model="removeRoleInfo.showDialog"
        @do-remove="doDeleteUserRole"
        :item-name="removeRoleInfo.userInfo.userIdForDisplay"
        item-type="from having admin privileges"
        :enable-return-focus="true">
    </RemovalValidation>
  </div>

</template>

<style>
.no-expansion .p-row-toggler {
  visibility: hidden !important;
}

</style>