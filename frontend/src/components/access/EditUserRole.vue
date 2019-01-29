<template>
  <div class="modal-card" style="width: 1110px">
    <header class="modal-card-head">
      New User Role
    </header>

    <section class="modal-card-body">
      <user-dn-input ref="userDn"></user-dn-input>

      <b-field label="Role">
        <b-select name="role" placeholder="Select a role" v-model="roleNameVal" v-validate="'required'" required>
          <option value="ROLE_PROJECT_ADMIN">Project Admin</option>
          <option value="ROLE_SERVER">Server/System</option>
        </b-select>
      </b-field>
      <p class="help is-danger" v-show="errors.has('role')">{{ errors.first('role') }}</p>

    </section>

    <footer class="modal-card-foot skills-justify-content-right">
      <button class="button is-outlined" v-on:click="$parent.close()">
        <span>Cancel</span>
        <span class="icon is-small">
              <i class="fas fa-stop-circle"/>
            </span>
      </button>

      <button class="button is-primary is-outlined" v-on:click="saveUserRole" :disabled="errors.any()">
        <span>Create</span>
        <span class="icon is-small">
          <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
        </span>
      </button>
    </footer>
  </div>
</template>

<script>
  import axios from 'axios';
  import { Validator } from 'vee-validate';
  import UserDnInput from '../utils/UserDnInput';

  const dictionary = {
    en: {
      attributes: {
        role: 'Role',
        user: 'User',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'EditUserRole',
    props: ['projectId', 'userDn', 'roleName'],
    components: { UserDnInput },
    data() {
      return {
        projectIdVal: this.projectId,
        userDnVal: this.userDn,
        roleNameVal: this.roleName,
        suggestions: [],
        selected: null,
        isFetching: false,
        isSaving: false,
      };
    },
    methods: {
      saveUserRole() {
        this.isSaving = true;
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.isSaving = false;
          } else {
            axios.put(`/admin/projects/${this.projectId}/users/${this.$refs.userDn.$data.userDn}/roles/${this.roleNameVal}`, {
              userDnVal: this.$refs.userDn.$data.userDn,
              projectIdVal: this.projectIdVal,
              roleNameVal: this.roleNameVal,
            })
              .then((result) => {
                this.isSaving = false;
                this.$parent.close();
                this.$emit('user-role-created', result.data);
              })
              .catch((e) => {
                this.isSaving = false;
                throw e;
            });
          }
        });
      },
    },
  };
</script>

<style scoped>

</style>
