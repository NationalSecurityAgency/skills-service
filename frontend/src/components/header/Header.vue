<template>
  <div class="header">
    <nav class="level container is-fluid">
      <div class="level-left">
        <div class="level-item">
          <p class="title">
            <router-link to="/"><i class="fas fa-users"/> User Skills</router-link>
          </p>
        </div>
      </div>

      <div class="level-right">
        <div class="level-item">
          <settings-button/>
        </div>

        <div class="level-item" v-if="isAuthenticated">
          <global-settings-button/>
        </div>

        <div class="level-item" v-if="isJwtAuthenticatedUser">
          <b-tooltip label="Sign Out"
                     position="is-bottom" animanted="true" type="is-light">
            <button class="button is-outlined" v-on:click="signOut" >
              <span class="icon is-small"><i class="fas fa-sign-out-alt"/></span>
            </button>
          </b-tooltip>
        </div>

        <div class="level-item">

          <b-dropdown position="is-bottom-left">
            <button class="button is-outlined" slot="trigger">
                <span class="icon is-small">
                  <i class="fas fa-question-circle"/>
                </span>
            </button>
            <b-dropdown-item>
              <div class="media">
                <div class="media-left">
                  <span class="icon is-medium has-text-info">
                    <i class="fas fa-info-circle"/>
                  </span>
                </div>
                <div class="media-content">
                  <h2 class="has-text-info">About</h2>
                  <small>How do User Skills work</small>
                </div>
              </div>
            </b-dropdown-item>
            <b-dropdown-item>
              <div class="media">
                <div class="media-left">
                  <span class="icon is-medium has-text-info">
                    <i class="fas fa-code"/>
                  </span>
                </div>
                <div class="media-content">
                  <h2 class="has-text-info">API</h2>
                  <small>Docs for Programatic Interface</small>
                </div>
              </div>
            </b-dropdown-item>
          </b-dropdown>

        </div>

      </div>
    </nav>

    <breadcrumb></breadcrumb>

  </div>
</template>

<script>
  import SettingsButton from './SettingsButton';
  import Breadcrumb from './Breadcrumb';
  import GlobalSettingsButton from './GlobalSettingsButton';

  export default {
    name: 'Header',
    components: {
      GlobalSettingsButton,
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
