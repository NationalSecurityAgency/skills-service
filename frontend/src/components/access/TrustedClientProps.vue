<template>
  <div class="box" id="trusted-client-props-panel">
    <div class="columns">
      <div class="column is-full">
        <span class="subtitle">Trusted Client Properties</span>
      </div>
    </div>
    <div class="columns">
      <div class="column is-one-fifth">
        <h1>Client ID: </h1>
      </div>
      <div class="column">
        <h1>{{ project.projectId }}</h1>
      </div>
    </div>
    <div class="columns">
      <div class="column is-one-fifth">
        <h1>Client Secret: </h1>
      </div>
      <div class="column">
        <h1>{{ project.clientSecret }}</h1>
      </div>
    </div>
    <div class="columns">
      <div class="column has-text-left">
        <button class="button is-primary is-outlined" v-on:click="confirmResetClientSecret">
          <span class="icon">
            <i class="fas fa-sync-alt"></i>
          </span>
          <span>Reset Client Secret</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
  import AccessService from './AccessService';

  export default {
    name: 'TrustedClientProps',
    props: ['project'],
    methods: {
      confirmResetClientSecret() {
        this.$dialog.confirm({
          title: 'Reset Client Secret',
          message: 'Are you sure you want reset the client secret? Your current client secret will no longer work after reset and you will need to update any application configuration using the old secret.',
          confirmText: 'Reset',
          type: 'is-danger',
          hasIcon: true,
          onConfirm: () => this.resetClientSecret(),
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
