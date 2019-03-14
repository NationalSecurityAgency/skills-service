<template>
  <div class="modal-card" style="width: 750px">
    <header class="modal-card-head">
      New User Role
    </header>

    <section class="modal-card-body">
      <existing-user-input :suggest="true" :validate="true" user-type="DASHBOARD" ref="userInput"></existing-user-input>

      <b-field label="Role">
        <b-select name="role" placeholder="Select a role" v-model="roleNameVal" v-validate="'required'" required>
          <option value="ROLE_PROJECT_ADMIN">Project Admin</option>
          <option value="ROLE_SERVER">Server/System</option>
        </b-select>
      </b-field>
      <p class="help is-danger" v-show="errors.has('role')">{{ errors.first('role') }}</p>

    </section>

    <footer class="modal-card-foot skills-justify-content-right">
      <button class="button is-outlined" @click="$parent.close()">
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
  import { Validator } from 'vee-validate';
  import ExistingUserInput from '../utils/ExistingUserInput';
  import AccessService from './AccessService';

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
    components: { ExistingUserInput },
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
            AccessService.saveUserRole(this.projectId, this.$refs.userInput.$data.userQuery, this.roleNameVal)
              .then((result) => {
                this.isSaving = false;
                this.$parent.close();
                this.$emit('user-role-created', result);
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
