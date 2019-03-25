<template>
  <div class="modal-card" style="width: 900px; height: 300px;">
    <header class="modal-card-head">
      <p class="modal-card-title">Add New User</p>
      <button class="delete" aria-label="close" v-on:click="$parent.close()"></button>
    </header>

    <section class="modal-card-body">
      <div class="columns">
        <div class="column">
          <user-dn-input ref="userDn"></user-dn-input>
        </div>
      </div>
    </section>

    <footer class="modal-card-foot skills-justify-content-right">
      <button class="button is-link is-outlined" v-on:click="$parent.close()">
        <span>Close</span>
        <span class="icon is-small">
            <i class="fas fa-stop-circle"></i>
          </span>
      </button>

      <button class="button is-primary is-outlined" v-on:click="saveUserRole" :disabled="errors.any()">
        <span>Add</span>
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
        user: 'User',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'AddUser',
    components: { UserDnInput },
    data() {
      return {
        overallErrMsg: '',
        overallInfoMsg: '',
        userDn: '',
        isSaving: false,
      };
    },
    methods: {
      saveUserRole() {
        this.isSaving = true;
        this.$validator.validateAll()
          .then((res) => {
            if (!res) {
              this.isSaving = false;
            } else {
              axios.put(`/admin/users/${encodeURIComponent(this.$refs.userDn.$data.userDn)}`)
                .then((result) => {
                  this.isSaving = false;
                  this.$emit('user-role-created', result.data);
                  this.$parent.close();
                })
                .finally(() => {
                  this.isSaving = false;
                });
            }
          });
      },
    },
  };

</script>
