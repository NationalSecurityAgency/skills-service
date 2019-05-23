<template>
  <div class="role-manager">
    <div id="add-user-div" class="row mt-2 mb-4">
      <div class="col-12 col-md-10 col-xlg-11 pb-2 pb-md-0">
        <existing-user-input :suggest="true" :validate="true" :user-type="userType" :excluded-suggestions="userIds"
                             :selectedUserId="selectedUserId"
                             ref="userInput"
                             v-model="selectedUserId"/>
      </div>
      <div class="col-auto">
        <b-button variant="outline-primary" @click="addUserRole" :disabled="errors.any() || !selectedUserId"
                  class="h-100">
          Add <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
        </b-button>
      </div>
    </div>

    <loading-container v-bind:is-loading="isLoading">
      <transition name="userRolesContainer" enter-active-class="animated fadeIn">
        <v-client-table :data="data" :columns="columns" :options="options">
          <div slot="edit" slot-scope="props" class="field has-addons">
            <b-button v-if="notCurrentUser(props.row.userId)" @click="deleteUserRoleConfirm(props.row)"
                      variant="outline-primary">
              <i class="fas fa-trash"/>
            </b-button>
            <span v-else v-b-tooltip.hover="'Can not remove myself. Sorry!!'">
              <b-button variant="outline-primary" disabled><i class="fas fa-trash"/></b-button>
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
        selectedUserId: '',
        isSaving: false,
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
      AccessService.getUserRoles(this.project.projectId)
        .then((result) => {
          this.isLoading = false;
          this.data = result;
          this.userIds = result.map(({ userId }) => userId);
        });
    },
    methods: {
      userAdded(userRole) {
        this.data.push(userRole);
        this.userIds.push(userRole.userId);
      },
      deleteUserRoleConfirm(row) {
        const msg = `Are you absolutely sure you want to remove [${row.userId}] as a ${this.roleDescription}?`;
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
            this.data = this.data.filter(item => item.id !== row.id);
            this.userIds = this.userIds.filter(userId => userId !== row.userId);
          });
      },
      notCurrentUser(userId) {
        return this.$store.getters.userInfo && userId !== this.$store.getters.userInfo.userId;
      },
      addUserRole() {
        this.isSaving = true;
        AccessService.saveUserRole(this.project.projectId, this.selectedUserId, this.role)
          .then((userInfo) => {
            this.userAdded(userInfo);
          })
          .finally(() => {
            this.isSaving = false;
            this.selectedUserId = '';
          });
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
</style>
