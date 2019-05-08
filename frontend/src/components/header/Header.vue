<template>
  <div>
    <div class="row p-3">
      <div class="col-sm text-center text-sm-left">
        <router-link class="h2 text-primary" to="/"><i class="fas fa-users"/> User Skills</router-link>
      </div>

      <div class="col text-center text-sm-right mt-3 mt-sm-0">
        <settings-button class="mr-2"/>
        <button v-if="isJwtAuthenticatedUser" class="btn btn-outline-info" v-on:click="signOut"
                v-b-tooltip.hover title="Sign Out">
          <i class="fas fa-sign-out-alt"/>
        </button>
      </div>
    </div>
    <breadcrumb></breadcrumb>
  </div>
</template>

<script>
  import SettingsButton from './SettingsButton';
  import Breadcrumb from './Breadcrumb';

  export default {
    name: 'Header',
    components: {
      Breadcrumb,
      SettingsButton,
    },
    methods: {
      signOut() {
        this.$store.dispatch('logout');
      },
    },
    computed: {
      isAuthenticated() {
        return this.$store.getters.isAuthenticated;
      },
      isJwtAuthenticatedUser() {
        return this.$store.getters.isAuthenticated && !this.$store.getters.isPkiAuthenticated;
      },
    },
  };
</script>

<style scoped>
  .header {
    padding-top: 1rem;
  }

  .title {
    font-size: 2rem;
    /*font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;*/
    font-family: 'Trocchi', serif;
    /*color: #7c795d;*/
    font-weight: normal;
    line-height: 1.8rem;
    /*margin: 0 0 1em;*/
  }

</style>
