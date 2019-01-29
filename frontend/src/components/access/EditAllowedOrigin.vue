<template>
  <div class="modal-card" style="width: 1110px">
    <header class="modal-card-head">
      {{action}} Allowed Origin
    </header>

    <section class="modal-card-body">
      <div class="field">
        <label class="label">Allowed Origin</label>
        <div class="control">
          <input class="input" v-model="allowedOriginInternal" v-validate="'required|url:require_protocol'"
                 placeholder="e.g. http://hostname:8080" name="allowedOrigin" v-focus/>
        </div>
        <p class="help is-danger" v-show="errors.has('allowedOrigin')">{{errors.first('allowedOrigin')}}</p>
      </div>

      <p v-if="errors.any() && overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </section>

    <footer class="modal-card-foot skills-justify-content-right">
      <button class="button is-outlined" v-on:click="$parent.close()">
        <span>Cancel</span>
        <span class="icon is-small">
              <i class="fas fa-stop-circle"/>
            </span>
      </button>

      <button class="button is-primary is-outlined" v-on:click="saveAllowedOrigin" :disabled="errors.any()">
        <span>{{action}}</span>
        <span class="icon is-small">
              <i class="fas fa-arrow-circle-right"/>
            </span>
      </button>
    </footer>
  </div>
</template>

<script>
  import axios from 'axios';
  import { Validator } from 'vee-validate';

  const dictionary = {
    en: {
      attributes: {
        allowedOrigin: 'Allowed Origin',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'EditAllowedOrigin',
    props: ['projectId', 'allowedOrigin', 'action', 'id'],
    data() {
      return {
        allowedOriginInternal: this.allowedOrigin,
        overallErrMsg: '',
      };
    },
    methods: {
      saveAllowedOrigin() {
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            axios.put(`/admin/projects/${this.projectId}/allowedOrigins`, {
              projectId: this.projectId,
              allowedOrigin: this.allowedOriginInternal,
              id: this.id,
            })
              .then((result) => {
                const eventName = `allowed-origin-${this.action.toLowerCase()}d`;
                this.$emit(eventName, result.data);
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
