<template>
  <b-tooltip :label="'Add new app user'"
             position="is-bottom" animanted="true" type="is-light">
    <button class="button" v-on:click="newUser" >
      <span class="icon is-small"><i class="fas fa-user-plus"/></span>
    </button>
  </b-tooltip>
</template>

<script>
  import AddUser from './AddUser';
  import ToastHelper from '../utils/ToastHelper';

  export default {
    name: 'AddUserButton',
    data() {
      return {
        userInfo: {},
        serverErrors: [],
      };
    },

    methods: {
      newUser() {
        this.$modal.open({
          parent: this,
          component: AddUser,
          hasModalCard: true,
          width: 1110,
          events: {
            'user-role-created': this.userAdded,
          },
        });
      },
      userAdded(userRole) {
        this.$toast.open(ToastHelper.defaultConf(`Created user account for '${userRole.userDn}'`));
      },
    },
  };
</script>

<style scoped>
</style>
