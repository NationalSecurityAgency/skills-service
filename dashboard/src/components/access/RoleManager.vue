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
  <div class="role-manager">
    <div class="p-3">
      <existing-user-input :suggest="true" :validate="true" :user-type="userType" :excluded-suggestions="userIds"
                           v-model="selectedUser" data-cy="existingUserInput"/>
    </div>
    <div class="row px-3 pb-3">
      <div  v-if="!isOnlyOneRole" class="col">
        <b-form-select v-model="userRole.selected"
                       :options="userRole.options"
                       aria-label="Please select user's Role"
                       data-cy="userRoleSelector">
          <template #first>
            <b-form-select-option :value="null" disabled>-- Please select user's Role --</b-form-select-option>
          </template>
        </b-form-select>
      </div>
      <div class="col-auto">
        <b-button variant="outline-hc" @click="addUserRole" :disabled="addUsrBtnDisabled"
                  data-cy="addUserBtn"
                  v-skills="'AddAdmin'">
          Add User <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"
                 aria-hidden="true"></i>
        </b-button>
      </div>
    </div>

    <div v-if="errNotification.enable">
      <b-alert data-cy="error-msg" variant="danger" class="mt-2" show dismissible>
        <i class="fa fa-exclamation-circle mr-1" aria-hidden="true"></i> <strong>Error!</strong>
        Request could not be completed! <strong>{{ errNotification.msg }}</strong>
      </b-alert>
    </div>

    <skills-b-table :options="table.options"
                    :items="data"
                    @page-changed="pageChanged"
                    @page-size-changed="pageSizeChanged"
                    @sort-changed="sortTable"
                    tableStoredStateId="roleManagerTable"
                    data-cy="roleManagerTable">
      <template #head(userId)="data">
        <span class="text-primary"><i class="fas fa-user skills-color-users" aria-hidden="true"></i> {{ data.label }}</span>
      </template>
      <template #head(roleName)="data">
        <span class="text-primary"><i class="fas fa-id-card text-danger" aria-hidden="true"></i> {{ data.label }}</span>
      </template>
      <template v-slot:cell(userId)="data">
        <div :data-cy="`userCell_${data.value}`">
          {{ getUserDisplay(data.item) }}
        </div>
      </template>
      <template v-slot:cell(roleName)="data">
        <div v-if="!data.item.isEdited">{{ getRoleDisplay(data.value) }}</div>
        <b-form-select v-else v-model="data.value"
                       :options="userRole.options"
                       :aria-label="`select new access role for user ${data.item.userId}`"
                       :data-cy="`roleDropDown_${data.item.userId}`"
                       @change="updateUserRole">
        </b-form-select>
      </template>
      <template v-slot:cell(controls)="data">

        <div class="float-right" :data-cy="`controlsCell_${data.item.userId}`">
          <i v-if="!notCurrentUser(data.item.userId)"
             data-cy="cannotRemoveWarning"
             v-b-tooltip.hover="'Can not remove or edit myself. Sorry!!'"
             class="text-warning fas fa-exclamation-circle mr-1"
             aria-hidden="true"/>

          <b-button-group class="">
            <b-button v-if="!isOnlyOneRole" @click="editItem(data.item)"
                      :disabled="!notCurrentUser(data.item.userId)"
                      variant="outline-primary" :aria-label="`edit access role from user ${data.item.userId}`"
                      data-cy="editUserBtn">
              <i class="fas fa-edit" aria-hidden="true"/>
            </b-button>
            <b-button @click="deleteUserRoleConfirm(data.item)"
                      :disabled="!notCurrentUser(data.item.userId)"
                      variant="outline-primary" :aria-label="`remove access role from user ${data.item.userId}`"
                      data-cy="removeUserBtn">
              <i class="text-warning fas fa-trash" aria-hidden="true"/>
            </b-button>
          </b-button-group>
        </div>
      </template>
    </skills-b-table>

  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import AccessService from './AccessService';
  import ExistingUserInput from '../utils/ExistingUserInput';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import NavigationErrorMixin from '../utils/NavigationErrorMixin';

  // role constants
  const ROLE_APP_USER = 'ROLE_APP_USER';
  const ROLE_PROJECT_ADMIN = 'ROLE_PROJECT_ADMIN';
  const ROLE_SUPERVISOR = 'ROLE_SUPERVISOR';
  const ROLE_SUPER_DUPER_USER = 'ROLE_SUPER_DUPER_USER';
  const ROLE_PROJECT_APPROVER = 'ROLE_PROJECT_APPROVER';
  const ALL_ROLES = [ROLE_APP_USER, ROLE_PROJECT_ADMIN, ROLE_SUPERVISOR, ROLE_SUPER_DUPER_USER, ROLE_PROJECT_APPROVER];

  export default {
    name: 'RoleManager',
    mixins: [MsgBoxMixin, NavigationErrorMixin],
    components: { SkillsBTable, ExistingUserInput },
    props: {
      projectId: {
        type: String,
        default: null,
      },
      roles: {
        type: Array,
        default: () => [ROLE_PROJECT_ADMIN, ROLE_PROJECT_APPROVER],
        validator: (value) => (value.every((v) => ALL_ROLES.includes(v))),
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
        type: Object,
        required: false,
        default: null,
        validator(value) {
          return value.msgText && value.titleText && value.okBtnText;
        },
      },
    },
    data() {
      return {
        // user roles table properties
        data: [],
        userIds: [],
        selectedUser: null,
        isSaving: false,
        errNotification: {
          enable: false,
          msg: '',
        },
        table: {
          options: {
            busy: true,
            bordered: false,
            outlined: true,
            stacked: 'md',
            sortBy: 'userId',
            sortDesc: false,
            fields: [
              {
                key: 'userId',
                label: this.roleDescription,
                sortable: true,
              },
              {
                key: 'roleName',
                label: 'Role',
                sortable: true,
              },
              {
                key: 'controls',
                label: '',
                sortable: false,
              },
            ],
            pagination: {
              hideUnnecessary: true,
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
            tableDescription: `${this.roleDescription} table`,
          },
        },
        userRole: {
          selected: null,
          options: [
            { value: ROLE_PROJECT_ADMIN, text: this.getRoleDisplay(ROLE_PROJECT_ADMIN) },
            { value: ROLE_PROJECT_APPROVER, text: this.getRoleDisplay(ROLE_PROJECT_APPROVER) },
          ],
        },
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      addUsrBtnDisabled() {
        return !(this.selectedUser && (this.userRole.selected || this.roles.length === 1));
      },
      isOnlyOneRole() {
        return this.roles.length === 1;
      },
    },
    methods: {
      getRoleDisplay(roleName) {
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
      },
      editItem(item) {
        this.data = this.data.map((user) => {
          if (user.userId === item.userId) {
            return ({ ...user, isEdited: true });
          }
          return ({ ...user, isEdited: false });
        });
      },
      updateUserRole(newRole) {
        const userRoleToUpdate = this.data.find((user) => user.isEdited);
        if (userRoleToUpdate) {
          // mark record as loading
          this.data = this.data.map((user) => {
            if (user.isEdited) {
              return ({
                ...user,
                isLoading: true,
              });
            }
            return user;
          });

          const pkiAuthenticated = this.$store.getters.isPkiAuthenticated;
          AccessService.saveUserRole(this.projectId, { userId: userRoleToUpdate.userId }, newRole, pkiAuthenticated)
            .then(() => {
              this.data = this.data.map((user) => {
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
              this.$nextTick(() => {
                this.$announcer.polite(`New ${newRole} was added to the user`);
              });
            }).catch((e) => {
              this.handleError(e);
            });
        }
      },
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadData();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadData();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      loadData() {
        this.table.options.busy = true;
        const pageParams = {
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        AccessService.getUserRoles(this.projectId, this.roles, pageParams)
          .then((result) => {
            this.table.options.busy = false;
            this.data = result.data;
            this.table.options.pagination.totalRows = result.totalCount;
            this.userIds = result.data.map(({ userIdForDisplay }) => userIdForDisplay);
          });
      },
      userAdded(userRole) {
        this.data.push(userRole);
        this.userIds.push(userRole.userIdForDisplay);
      },
      deleteUserRoleConfirm(row) {
        const msg = `Are you absolutely sure you want to remove ${this.getUserDisplay(row)} as a ${this.roleDescription}?`;
        this.msgConfirm(msg)
          .then((res) => {
            if (res) {
              this.deleteUserRole(row);
            }
          });
      },
      deleteUserRole(row) {
        this.table.options.busy = true;
        AccessService.deleteUserRole(row.projectId, row.userId, row.roleName)
          .then(() => {
            this.data = this.data.filter((item) => item.userId !== row.userId);
            this.userIds = this.userIds.filter((userId) => userId !== row.userIdForDisplay);
            this.$emit('role-deleted', { userId: row.userId, role: row.roleName });
            this.table.options.busy = false;
            this.table.options.pagination.totalRows = this.data.length;
            this.$nextTick(() => {
              this.$announcer.polite(`${row.roleName} was removed from the user`);
            });
          });
      },
      notCurrentUser(userId) {
        return this.$store.getters.userInfo && userId !== this.$store.getters.userInfo.userId;
      },
      addUserRole() {
        if (this.addRoleConfirmation) {
          this.msgConfirm(this.addRoleConfirmation.msgText, this.addRoleConfirmation.titleText, this.addRoleConfirmation.okBtnText).then((ok) => {
            if (ok) {
              this.doAddUserRole();
            }
          });
        } else {
          this.doAddUserRole();
        }
      },
      doAddUserRole() {
        this.isSaving = true;
        this.table.options.busy = true;
        const pkiAuthenticated = this.$store.getters.isPkiAuthenticated;

        const role = this.isOnlyOneRole ? this.roles[0] : this.userRole.selected;
        AccessService.saveUserRole(this.projectId, this.selectedUser, role, pkiAuthenticated)
          .then(() => {
            this.$emit('role-added', { userId: this.selectedUser.userId, role });
            this.loadData();
          }).catch((e) => {
            this.handleError(e);
          })
          .finally(() => {
            this.isSaving = false;
            this.selectedUser = null;
            this.userRole.selected = null;
          });
      },
      handleError(e) {
        if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'UserNotFound') {
          this.errNotification.msg = e.response.data.explanation;
          this.errNotification.enable = true;
        } else {
          const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
          this.handlePush({ name: 'ErrorPage', query: { errorMessage } });
        }
      },
      getUserDisplay(item) {
        return item.lastName && item.firstName ? `${item.firstName} ${item.lastName} (${item.userIdForDisplay})` : item.userIdForDisplay;
      },
    },
  };
</script>

<style scoped>
</style>

<style>
</style>
