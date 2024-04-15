<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import AccessService from '@/components/access/AccessService.js'
import ExistingUserInput from "@/components/utils/ExistingUserInput.vue";
import UserRolesUtil from '@/components/utils/UserRolesUtil';
import { useConfirm } from 'primevue/useconfirm'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import Column from "primevue/column";
import DataTable from "primevue/datatable";

// role constants
const ROLE_APP_USER = 'ROLE_APP_USER';
const ROLE_PROJECT_ADMIN = 'ROLE_PROJECT_ADMIN';
const ROLE_SUPERVISOR = 'ROLE_SUPERVISOR';
const ROLE_SUPER_DUPER_USER = 'ROLE_SUPER_DUPER_USER';
const ROLE_PROJECT_APPROVER = 'ROLE_PROJECT_APPROVER';
const ALL_ROLES = [ROLE_APP_USER, ROLE_PROJECT_ADMIN, ROLE_SUPERVISOR, ROLE_SUPER_DUPER_USER, ROLE_PROJECT_APPROVER];

const confirm = useConfirm();
const appConfig = useAppConfig();
const announcer = useSkillsAnnouncer();
const userInfo = useUserInfo();

const emit = defineEmits(['role-added', 'role-deleted']);
const props = defineProps({
  projectId: {
    type: String,
    default: null,
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
});

onMounted(() => {
  loadData();
});

let table = ref({
  options: {
    busy: true,
    sortBy: 'userId',
    sortDesc: 1,
    pagination: {
      hideUnnecessary: true,
      server: false,
      currentPage: 1,
      totalRows: 1,
      pageSize: 5,
      possiblePageSizes: [5, 10, 15, 20],
    },
    tableDescription: `${props.roleDescription} table`,
  },
});

let data = ref([]);
let userIds = ref([]);
const selectedUser = ref(null);
const isSaving = ref(false);
const errNotification = ref({
  enable: false,
  msg: '',
});
const userRole = ref({
  selected: null,
  options: [
    { value: ROLE_PROJECT_ADMIN, text: getRoleDisplay(ROLE_PROJECT_ADMIN) },
    { value: ROLE_PROJECT_APPROVER, text: getRoleDisplay(ROLE_PROJECT_APPROVER) },
  ],
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
  }
  return 'Unknown';
}

function loadData() {
  table.value.options.busy = true;
  const pageParams = {
    limit: 200,
    ascending: table.value.options.sortDesc === 1,
    page: 1,
    orderBy: table.value.options.sortBy,
  };
  AccessService.getUserRoles(props.projectId, props.roles, pageParams).then((result) => {
    table.value.options.busy = false;
    data.value = result.data;
    table.value.options.pagination.totalRows = result.totalCount;
    // userIds.value = result.data.map((u) => [u.userId, u.userIdForDisplay]).flatten();
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

    confirm.require({
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
  const pkiAuthenticated = appConfig.isPkiAuthenticated.value;

  const role = isOnlyOneRole.value ? props.roles[0] : userRole.value.selected;
  AccessService.saveUserRole(props.projectId, selectedUser.value, role, pkiAuthenticated).then(() => {
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
  } else {
    const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
    // this.handlePush({ name: 'ErrorPage', query: { errorMessage } });
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

  confirm.require({
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
    data.value = data.value.filter((item) => item.userId !== row.userId);
    userIds.value = userIds.value.filter((userId) => userId !== row.userId && userId !== row.userIdForDisplay);
    emit('role-deleted', { userId: row.userId, role: row.roleName });
    table.value.options.busy = false;
    table.value.options.pagination.totalRows = data.value.length;
    nextTick(() => {
      announcer.polite(`${row.roleName} was removed from the user`);
    });
  });
}

function editItem(item) {
  data.value = data.value.map((user) => {
    if (user.userId === item.userId) {
      return ({ ...user, isEdited: true });
    }
    return ({ ...user, isEdited: false });
  });
}

function updateUserRole(newRole) {
  const userRoleToUpdate = data.value.find((user) => user.isEdited);
  if (userRoleToUpdate) {
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

    const pkiAuthenticated = appConfig.isPkiAuthenticated.value;
    AccessService.saveUserRole(props.projectId, userRoleToUpdate, newRole, pkiAuthenticated)
        .then(() => {
          data.value = data.value.map((user) => {
            if (user.isEdited) {
              const copy = ({
                ...user,
                isEdited: false,
                isLoading: false,
                roleName: newRole,
              });
              return copy;
            }
            return user;
          });
          nextTick(() => {
            announcer.polite(`New ${newRole} was added to the user`);
          });
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
  <div>
    <div>
      <existing-user-input :suggest="true" :validate="true" :user-type="userType" :excluded-suggestions="userIds"
                           v-model="selectedUser" data-cy="existingUserInput"/>
    </div>
    <div class="mt-3 mb-3 flex gap-2">
      <div v-if="!isOnlyOneRole" class="flex-1">
        <Dropdown class="w-full" v-model="userRole.selected" :options="userRole.options" data-cy="userRoleSelector"
                  placeholder="Please select user's Role" optionLabel="text" optionValue="value" />
      </div>
      <div>
        <SkillsButton variant="outline-hc" @click="addUserRole" :disabled="addUsrBtnDisabled" data-cy="addUserBtn"
                      label="Add User" :icon="isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right'">
        </SkillsButton>
      </div>
    </div>

    <Message v-if="errNotification.enable" severity="error" data-cy="error-msg">
      <strong>Error!</strong> Request could not be completed! {{ errNotification.msg }}
    </Message>

    <DataTable :value="data" :rowsPerPageOptions="[5, 10, 15, 20]" data-cy="roleManagerTable" striped-rows
               v-model:sort-field="table.options.sortBy"
               v-model:sort-order="table.options.sortDesc"
               paginator :rows="5">
      <Column :header="roleDescription" field="userId" sortable>
        <template #header>
          <span class="mr-2"><i class="fas fa-user skills-color-users" aria-hidden="true"></i> </span>
        </template>
        <template #body="slotProps">
          <div :data-cy="`userCell_${slotProps.data.userId}`">
            {{ getUserDisplay(slotProps.data) }}
          </div>
        </template>
      </Column>
      <Column header="Role" field="roleName" sortable>
        <template #header>
          <span class="mr-2"><i class="fas fa-id-card text-danger" aria-hidden="true"></i> </span>
        </template>
        <template #body="slotProps">
          <div v-if="!slotProps.data.isEdited">{{ getRoleDisplay(slotProps.data.roleName) }}</div>
        </template>
      </Column>
      <Column header="Controls">
        <template #body="slotProps">
          <div class="float-right mr-1" :data-cy="`controlsCell_${slotProps.data.userId}`">
<!--            <i v-if="!notCurrentUser(slotProps.data.userId)"-->
<!--               data-cy="cannotRemoveWarning"-->
<!--               class="text-warning fas fa-exclamation-circle mr-1"-->
<!--               aria-hidden="true"/>-->

              <SkillsButton v-if="!isOnlyOneRole" @click="editItem(slotProps.data)"
                        :disabled="!notCurrentUser(slotProps.data.userId)"
                        :aria-label="`edit access role from user ${slotProps.data.userId}`"
                        data-cy="editUserBtn" icon="fas fa-edit" label="Edit" size="small">
              </SkillsButton>
              <SkillsButton @click="deleteUserRoleConfirm(slotProps.data)"
                        :disabled="!notCurrentUser(slotProps.data.userId)"
                        :aria-label="`remove access role from user ${slotProps.data.userId}`"
                        data-cy="removeUserBtn" icon="fas fa-trash" label="Delete" size="small">
              </SkillsButton>
          </div>
        </template>
      </Column>
      <template #paginatorstart>
        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ data.length }}</span>
      </template>
      <template #empty>
        <span class="flex align-items-center justify-content-center">There are no records to show</span>
      </template>
    </DataTable>
  </div>
</template>

<style scoped>

</style>