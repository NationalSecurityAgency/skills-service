<template>
  <div class="card" id="trusted-client-props-panel">
    <div class="card-header">
      Trusted Client Properties
    </div>
    <div class="card-body">
      <div class="row">
        <div class="col-12 col-md-3 text-secondary">
          <span>Client ID:</span>
        </div>
        <div class="col">
          <span>{{ project.projectId }}</span>
        </div>
      </div>
      <div class="row mt-1">
        <div class="col-12 col-md-3 text-secondary">
          <span>Client Secret:</span>
        </div>
        <div class="col">
          <span>{{ project.clientSecret }}</span>
        </div>
      </div>
      <b-button @click="confirmResetClientSecret" variant="outline-info" class="mt-3">
        <i class="fas fa-sync-alt"/> Reset Client Secret
      </b-button>
    </div>
  </div>
</template>

<script>
  import AccessService from './AccessService';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'TrustedClientProps',
    mixins: [MsgBoxMixin],
    props: ['project'],
    methods: {
      confirmResetClientSecret() {
        this.msgConfirm('Are you sure you want reset the client secret? Your current client secret will no longer work after reset and you will need to update any application configuration using the old secret.', 'Reset Secret?', 'Reset Please!')
          .then((res) => {
            if (res) {
              this.resetClientSecret();
            }
          });
      },
      resetClientSecret() {
        AccessService.resetClientSecret(this.project.projectId)
          .then((clientSecret) => {
            this.project.clientSecret = clientSecret;
          });
      },
    },
  };
</script>

<style scoped>
</style>
