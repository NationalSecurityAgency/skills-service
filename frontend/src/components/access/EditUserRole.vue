<template>
  <modal title="New Project Administrator" :isSaveButtonDisabled="true" @cancel-clicked="closeMe" @save-clicked="saveUserRole" style="height: 420px; width: 750px">
    <template slot="content">
      <existing-user-input :suggest="true" :validate="true" user-type="DASHBOARD" :excluded-suggestions="userIds" ref="userInput"></existing-user-input>
      <p v-if="errors.any() && overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </template>
  </modal>
</template>

<script>
  import { Validator } from 'vee-validate';
  import ExistingUserInput from '../utils/ExistingUserInput';
  import AccessService from './AccessService';
  import Modal from '../utils/modal/Modal';

  const dictionary = {
    en: {
      attributes: {
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
    components: { ExistingUserInput, Modal },
    data() {
      return {
        projectIdVal: this.projectId,
        userDnVal: this.userDn,
        isSaving: false,
        overallErrMsg: '',
      };
    },
    methods: {
      closeMe() {
        this.$parent.close();
      },
      saveUserRole() {
        this.isSaving = true;
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.isSaving = false;
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
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
