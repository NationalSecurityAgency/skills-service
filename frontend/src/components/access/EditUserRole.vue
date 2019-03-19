<template>
  <div class="modal-card" style="height: 420px; width: 750px">
    <header class="modal-card-head">
      New User Role
    </header>

    <section class="modal-card-body">
      <existing-user-input :suggest="true" :validate="true" user-type="DASHBOARD" :excluded-suggestions="userIds" ref="userInput"></existing-user-input>
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
    props: {
      projectId: {
        type: String,
      },
      userIds: {
        type: Array,
        default: () => ([]),
      },
    },
    components: { ExistingUserInput },
    data() {
      return {
        projectIdVal: this.projectId,
        userDnVal: this.userDn,
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
            AccessService.saveUserRole(this.projectId, this.$refs.userInput.$data.userQuery, 'ROLE_PROJECT_ADMIN')
              .then((result) => {
                this.$emit('user-role-created', result);
              })
              .finally(() => {
                this.isSaving = false;
                this.$parent.close();
            });
          }
        });
      },
    },
  };
</script>

<style scoped>

</style>
