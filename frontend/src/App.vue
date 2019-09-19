<template>
  <div id="app" class="container-fluid">
    <customizable-header></customizable-header>

    <div class="overall-container">
      <loading-container v-bind:is-loading="isLoading">
        <div v-if="!isLoading">
          <header-view v-if="isAuthenticatedUser"/>
          <div>
            <router-view/>
          </div>
        </div>
      </loading-container>
    </div>

    <customizable-footer></customizable-footer>
  </div>
</template>

<script>
  import HeaderView from './components/header/Header';
  import LoadingContainer from './components/utils/LoadingContainer';
  import CustomizableHeader from './components/customization/CustomizableHeader';
  import CustomizableFooter from './components/customization/CustomizableFooter';
  import IconManagerService from './components/utils/iconPicker/IconManagerService';
  import InceptionConfigurer from './InceptionConfigurer';
  import AccessService from './components/access/AccessService';

  export default {
    name: 'App',
    components: {
      CustomizableFooter,
      CustomizableHeader,
      HeaderView,
      LoadingContainer,
    },
    data() {
      return {
        isLoading: false,
        isSupervisor: false,
        serverErrors: [],
      };
    },
    computed: {
      isAuthenticatedUser() {
        return this.$store.getters.isAuthenticated;
      },
      activeProjectId() {
        return this.$store.state.projectId;
      },
      userInfo() {
        return this.$store.getters.userInfo;
      },
    },
    created() {
      if (this.isAuthenticatedUser) {
        AccessService.hasRole('ROLE_SUPERVISOR')
          .then((response) => {
            this.isSupervisor = response;
            this.addCustomIconCSS();
          });
      }
    },
    watch: {
      activeProjectId() {
        this.addCustomIconCSS();
      },
      isAuthenticatedUser() {
        if (this.isAuthenticatedUser) {
          AccessService.hasRole('ROLE_SUPERVISOR')
            .then((response) => {
              this.isSupervisor = response;
              this.addCustomIconCSS();
            });
        }
      },
      userInfo(newUserInfo) {
        if (newUserInfo) {
          InceptionConfigurer.configure();
        }
      },
    },
    methods: {
      addCustomIconCSS() { // This must be done here AFTER authentication
        IconManagerService.refreshCustomIconCss(this.activeProjectId, this.isSupervisor);
      },
    },
  };
</script>


<style lang="scss">
  @import "~bootstrap/scss/bootstrap";
  @import '~bootstrap-vue/src/index.scss';
</style>

<style>
  @import '../node_modules/@fortawesome/fontawesome-free/css/all.css';
  @import '../node_modules/material-icons/iconfont/material-icons.css';
  @import '../node_modules/material-icons/css/material-icons.css';
  @import '../node_modules/animate.css/animate.css';
  @import './styles/utils.css';

  #app {
    background-color: #f1f1f1;
  }

  .overall-container {
    min-height: calc(100vh - 50px);
  }

  /* vue-table-2s bug? - "Filter:" label is not left aligned, this is a workaround */
  .vue-table-2 .form-inline label {
    justify-content: left !important;
  }

</style>
