<template>
  <div class="box">
    <div class="columns">
      <div class="column">
        <div class="subtitle">User/Role Settings</div>
      </div>
      <div class="column has-text-right">
        <a v-on:click="newUser" class="button is-outlined is-info">
          <span>Add New User</span>
          <span class="icon is-small">
            <i class="fas fa-plus-circle"/>
          </span>
        </a>
      </div>
    </div>

    <loading-container v-bind:is-loading="isLoading">
      <transition name="userRolesContainer" enter-active-class="animated fadeIn">
        <v-client-table :data="data" :columns="columns" :options="options">
          <div slot="edit" slot-scope="props" class="field has-addons">
            <!--<p class="control">-->
              <!--<a class="button">-->
              <!--<span class="icon is-small">-->
                <!--<i class="fas fa-edit"/>-->
              <!--</span>-->
                <!--<span>Edit</span>-->
              <!--</a>-->
            <!--</p>-->
            <p class="control">
              <a v-on:click="deleteUserRole(props.row)" class="button">
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
  import axios from 'axios';
  import EditUserRole from './EditUserRole';
  import LoadingContainer from '../utils/LoadingContainer';
  import ToastHelper from '../utils/ToastHelper';

  export default {
    name: 'UserRoles',
    components: { LoadingContainer },
    props: ['project'],
    data() {
      return {
        serverErrors: [],
        // user roles table properties
        isLoading: true,
        data: [],
        columns: ['userId', 'roleName', 'edit'],
        options: {
          headings: {
            userId: 'User',
            roleName: 'Role',
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
      axios.get(`/admin/projects/${this.project.projectId}/userRoles`)
        .then((response) => {
          this.isLoading = false;
          this.data = response.data;
        })
        .catch((e) => {
          this.serverErrors.push(e);
      });
    },
    methods: {
      newUser() {
        this.$modal.open({
          parent: this,
          component: EditUserRole,
          hasModalCard: true,
          width: 1110,
          props: {
            projectId: this.project.projectId,
          },
          events: {
            'user-role-created': this.userAdded,
          },
        });
      },
      userAdded(userRole) {
        this.data.push(userRole);
        this.$toast.open(ToastHelper.defaultConf(`Created '${userRole.roleName}' role`));
      },
      deleteUserRole(row) {
        this.$dialog.confirm({
          title: 'Delete Role',
          message: `Are you absolutely sure you want to delete [${row.roleName}] for user
                    [${row.userId}]?`,
          confirmText: 'Delete',
          type: 'is-danger',
          hasIcon: true,
          onConfirm: () => this.deleteUserRoleAjax(row),
        });
      },
      deleteUserRoleAjax(row) {
        axios.delete(`/admin/projects/${row.projectId}/users/${row.userId}/roles/${row.roleName}`)
          .then(() => {
            this.data = this.data.filter(item => item.id !== row.id);
            this.$toast.open(ToastHelper.defaultConf(`Removed '${row.roleName}' role`));
        });
      },
    },
  };
</script>

<style scoped>

</style>
