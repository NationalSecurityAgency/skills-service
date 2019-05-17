<template>
    <div>
        <div v-if="initialized && !accountCreated">
            <root-registration v-if="!isPki" v-on:registerUser="onRegisterUser"/>
            <root-pki v-else v-on:grantRoot="onGrantRoot"/>
        </div>
        <div v-if="(initialized && accountCreated)">
            <success @proceed="onProceed"/>
        </div>
    </div>
</template>

<script>
  import BootstrapService from './BootstrapService';
  import RootPki from './RootPki';
  import RootRegistration from './RootRegistration';
  import Success from './Success';

  export default {
    name: 'HomePage',
    components: { RootPki, RootRegistration, Success },
    data() {
      return {
        initialized: false,
        isPki: false,
        accountCreated: false,
      };
    },
    created() {
      if (!this.initialized) {
        BootstrapService.isLoggedIn()
          .then((response) => {
            if (response) {
              this.isPki = true;
            }
            this.initialized = true;
          })
          .catch((error) => {
            this.initialized = true;
            throw error;
        });
      }
    },
    methods: {
      onRegisterUser(loginFields) {
        BootstrapService.registerUser(loginFields)
          .then(() => {
            this.accountCreated = true;
          })
          .catch((error) => { throw error; });
      },
      onGrantRoot() {
        BootstrapService.grantRoot()
          .then(() => {
            this.accountCreated = true;
          })
          .catch((error) => { throw error; });
      },
      onProceed() {
        window.location = '/';
      },
    },
  };
</script>

<style scoped>

</style>
