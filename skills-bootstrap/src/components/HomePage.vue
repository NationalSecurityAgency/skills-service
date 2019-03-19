<template>
    <div v-if="initialized">
        <root-registration v-if="!isPki" v-on:login="onLogin"/>
        <root-pki v-else v-on:proceed="onProceed"/>
    </div>
</template>

<script>
  import BootstrapService from './BootstrapService';
  import RootPki from './RootPki';
  import RootRegistration from './RootRegistration';

  export default {
    name: 'HomePage',
    components: { RootPki, RootRegistration },
    data() {
      return {
        initialized: false,
        isPki: false,
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
            this.serverErrors.push(error);
            this.initialized = true;
            throw error;
        });
      }
    },
    methods: {
      onLogin(loginFields) {
        BootstrapService.registerUser(loginFields)
          .then(() => {
            window.location = '/';
          })
          .catch((error) => {
            this.serverErrors.push(error);
            throw error;
        });
      },
      onProceed() {
        BootstrapService.grantRoot()
          .then(() => {
            window.location = '/';
          })
          .catch((error) => {
            this.serverErrors.push(error);
            throw error;
        });
      },
    },
  };
</script>

<style scoped>

</style>
