<template>
  <div id="app">
    <customizable-header></customizable-header>

    <div class="overall-container">
      <loading-container v-bind:is-loading="isLoading">
        <div v-if="!isLoading">
          <header-view/>
          <div v-if="isAuthenticatedUser">
            <div class="container is-fluid">
              <router-view/>
            </div>
          </div>
          <div v-else-if="!isAuthenticatedUser">
            <login-form v-if="!requestAccount"/>
            <request-account-form v-else/>
          </div>
        </div>
      </loading-container>
    </div>

    <customizable-footer></customizable-footer>
  </div>
</template>

<script>
  import HeaderView from './components/header/Header';
  import LoginForm from './components/access/Login';
  import RequestAccountForm from './components/access/RequestAccess';
  import LoadingContainer from './components/utils/LoadingContainer';
  import CustomizableHeader from './components/customization/CustomizableHeader';
  import CustomizableFooter from './components/customization/CustomizableFooter';
  import IconManagerService from './components/utils/iconPicker/IconManagerService';

  export default {
    name: 'App',
    components: {
      CustomizableFooter,
      CustomizableHeader,
      LoginForm,
      RequestAccountForm,
      HeaderView,
      LoadingContainer,
    },
    data() {
      return {
        isLoading: false,
        serverErrors: [],
      };
    },
    computed: {
      isAuthenticatedUser() {
        return this.$store.getters.isAuthenticated;
      },
      requestAccount() {
        return this.$route.query.requestAccount;
      },
      activeProjectId() {
        return this.$store.state.projectId;
      },
    },
    watch: {
      activeProjectId(projectId) {
        if (projectId) {
          this.addCustomIconCSS();
        }
      },
    },
    created() {
      if (this.isAuthenticatedUser && this.activeProjectId) {
        this.addCustomIconCSS();
      }
    },
    methods: {
      addCustomIconCSS() { // This must be done here AFTER authentication
        IconManagerService.refreshCustomIconCss(this.activeProjectId);
      },
    },
  };
</script>


<style lang="scss">
  @import "styles/buefy-custom";
</style>

<style>
  /*@import '../node_modules/bulma/css/bulma.min.css';*/
  @import '../node_modules/@fortawesome/fontawesome-free/css/all.css';
  @import '../node_modules/material-icons/iconfont/material-icons.css';
  @import '../node_modules/material-icons/css/material-icons.css';
  /*@import '../node_modules/buefy/dist/buefy.css';*/
  @import '../node_modules/animate.css/animate.css';
  @import './styles/utils.css';

  .overall-container {
    min-height: calc(100vh - 50px);
  }
</style>
