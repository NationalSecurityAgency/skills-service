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
    <div id="add-user-div" class="row mt-2 mb-5">
      <div class="col-12 col-md-10 col-xlg-11 pb-2 pb-md-0">
        <existing-user-input :suggest="true" :validate="true" :user-type="userType" :excluded-suggestions="userIds"
                             v-model="selectedUser"/>
      </div>
      <div class="col-auto">
        <b-button variant="outline-primary" @click="addUserRole" :disabled="!selectedUser"
                  class="h-100" v-skills="'AddAdmin'">
          Add <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']" aria-hidden="true"></i>
        </b-button>
      </div>
    </div>

    <b-alert v-if="errNotification.enable" data-cy="error-msg" variant="danger" class="mt-2" show
             dismissible>
      <i class="fa fa-exclamation-circle mr-1" aria-hidden="true"></i> <strong>Error!</strong> Request could not be completed! <strong>{{
      errNotification.msg }}</strong>
    </b-alert>

    <loading-container v-bind:is-loading="isLoading">
      <transition name="userRolesContainer" enter-active-class="animated fadeIn">
        <v-client-table :data="data" :columns="columns" :options="options">
          <div slot="userId" slot-scope="props">
            {{ getUserDisplay(props.row) }}
          </div>

          <div slot="edit" slot-scope="props" class="field has-addons">
            <b-button v-if="notCurrentUser(props.row.userId)" @click="deleteUserRoleConfirm(props.row)"
                      variant="outline-primary" :aria-label="'remove access role from user '+props.row.userId">
              <i class="fas fa-trash" aria-hidden="true"/>
            </b-button>
            <span v-else v-b-tooltip.hover="'Can not remove myself. Sorry!!'">
              <b-button variant="outline-primary" disabled aria-label="cannot remove access role from yourself"><i class="fas fa-trash" aria-hidden="true"/></b-button>
            </span>
          </div>
        </v-client-table>
      </transition>
    </loading-container>
  </div>
</template>

<script>
  import LoadingContainer from '../utils/LoadingContainer';
  import AccessService from './AccessService';
  import ExistingUserInput from '../utils/ExistingUserInput';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  // role constants
  const ROLE_APP_USER = 'ROLE_APP_USER';
  const ROLE_PROJECT_ADMIN = 'ROLE_PROJECT_ADMIN';
  const ROLE_SUPERVISOR = 'ROLE_SUPERVISOR';
  const ROLE_SUPER_DUPER_USER = 'ROLE_SUPER_DUPER_USER';

  export default {
    name: 'RoleManager',
    mixins: [MsgBoxMixin],
    components: { ExistingUserInput, LoadingContainer },
    props: {
      project: {
        type: Object,
        default: () => ({}),
      },
      role: {
        type: String,
        default: 'ROLE_PROJECT_ADMIN',
        validator: (value) => ([ROLE_APP_USER, ROLE_PROJECT_ADMIN, ROLE_SUPERVISOR, ROLE_SUPER_DUPER_USER].indexOf(value) >= 0),
      },
      roleDescription: {
        type: String,
        default: 'Project Administrator',
      },
      userType: {
        type: String,
        default: 'DASHBOARD',
      },
    },
    data() {
      return {
        // user roles table properties
        isLoading: true,
        data: [],
        userIds: [],
        columns: ['userId', 'edit'],
        selectedUser: null,
        isSaving: false,
        errNotification: {
          enable: false,
          msg: '',
        },
        options: {
          headings: {
            userId: this.roleDescription,
            edit: '',
          },
          columnsClasses: {
            edit: 'control-column',
          },
          sortable: ['userId', 'roleName'],
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          filterable: false,
          skin: 'table is-striped is-fullwidth',
        },
      };
    },
    mounted() {
      AccessService.getUserRoles(this.project.projectId, this.role)
        .then((result) => {
          this.isLoading = false;
          this.data = result;
          this.userIds = result.map(({ userIdForDisplay }) => userIdForDisplay);
        });
    },
    methods: {
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
        AccessService.deleteUserRole(row.projectId, row.userId, row.roleName)
          .then(() => {
            this.data = this.data.filter((item) => item.userId !== row.userId);
            this.userIds = this.userIds.filter((userId) => userId !== row.userIdForDisplay);
            this.$emit('role-deleted', { userId: row.userId, role: row.roleName });
          });
      },
      notCurrentUser(userId) {
        return this.$store.getters.userInfo && userId !== this.$store.getters.userInfo.userId;
      },
      addUserRole() {
        this.isSaving = true;
        const pkiAuthenticated = this.$store.getters.isPkiAuthenticated;

        AccessService.saveUserRole(this.project.projectId, this.selectedUser, this.role, pkiAuthenticated)
          .then((userInfo) => {
            this.userAdded(userInfo);
            this.$emit('role-added', { userId: this.selectedUser.userId, role: this.role });
          }).catch((e) => {
            if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'UserNotFound') {
              this.errNotification.msg = e.response.data.explanation;
              this.errNotification.enable = true;
            } else {
              const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
              this.$router.push({ name: 'ErrorPage', query: { errorMessage } });
            }
          })
          .finally(() => {
            this.isSaving = false;
            this.selectedUser = null;
          });
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
  .role-manager .control-column {
    max-width: 2rem;
  }

  .role-manager .VuePagination__count {
    display: none;
  }
</style>
