<template>
  <div class="box">
    <div>
      <existing-user-input :suggest="true" :validate="true" :user-type="userType" :excluded-suggestions="userIds" ref="userInput"
                           v-on:userSelected="onUserSelected"></existing-user-input>
    </div>

    <loading-container v-bind:is-loading="isLoading">
      <transition name="userRolesContainer" enter-active-class="animated fadeIn">
        <v-client-table :data="data" :columns="columns" :options="options">
          <div slot="edit" slot-scope="props" class="field has-addons">
            <p class="control">
              <a v-if="notCurrentUser(props.row.userId)" v-on:click="deleteUserRoleConfirm(props.row)" class="button is-outlined">
              <span class="icon is-small">
                <i class="fas fa-trash"/>
              </span>
                <span>Delete</span>
              </a>
            </p>
          </div>
        </v-client-table>
      </transition>
    </loading-container>
  </div>
</template>

<script>
  import AddRole from './AddRole';
  import LoadingContainer from '../utils/LoadingContainer';
  import ToastHelper from '../utils/ToastHelper';
  import AccessService from './AccessService';
  import ExistingUserInput from '../utils/ExistingUserInput';

  export default {
    name: 'RoleManager',
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
        options: {
          headings: {
            userId: 'User',
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
      newUser() {
        this.$modal.open({
          parent: this,
          component: AddRole,
          hasModalCard: true,
          props: {
            projectId: this.project.projectId,
            userIds: this.userIds,
          },
          events: {
            'user-role-created': this.userAdded,
          },
        });
      },
      userAdded(userRole) {
        this.data.push(userRole);
        this.userIds.push(userRole.userId);
        this.$toast.open(ToastHelper.defaultConf(`Created '${userRole.roleName}' role`));
      },
      deleteUserRoleConfirm(row) {
        this.$dialog.confirm({
          title: 'Delete Role',
          message: `Are you absolutely sure you want to remove [${row.userId}] as a ${this.roleDescription}?`,
          confirmText: 'Delete',
          type: 'is-danger',
          hasIcon: true,
          onConfirm: () => this.deleteUserRole(row),
        });
      },
      deleteUserRole(row) {
        AccessService.deleteUserRole(row.projectId, row.userId, row.roleName)
          .then(() => {
            this.data = this.data.filter(item => item.id !== row.id);
            this.userIds = this.userIds.filter(userId => userId !== row.userId);
            this.$toast.open(ToastHelper.defaultConf(`Removed '${row.roleName}' role`));
        });
      },
      notCurrentUser(userId) {
        return userId !== this.$store.getters.userInfo.userId;
      },
      onUserSelected(userId) {
        this.$dialog.confirm({
          title: 'Add Role',
          message: `Are you absolutely sure you want to add [${userId}] as a ${this.roleDescription}?`,
          confirmText: 'Add',
          type: 'is-danger',
          hasIcon: true,
          onConfirm: () => this.addUserRole(userId),
        });
      },
      addUserRole(userId) {
        AccessService.saveUserRole(this.project.projectId, userId, this.role).then((userInfo) => {
          this.userAdded(userInfo);
        });
      },
    },
  };
</script>

<style scoped>

</style>
