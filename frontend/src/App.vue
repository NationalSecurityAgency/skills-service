/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <div id="app" class="container-fluid">
    <new-software-version-component/>
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
  import InceptionProgressMessagesMixin from './components/inception/InceptionProgressMessagesMixin';
  import NewSoftwareVersionComponent from './components/header/NewSoftwareVersion';

  export default {
    name: 'App',
    mixins: [InceptionProgressMessagesMixin],
    components: {
      NewSoftwareVersionComponent,
      CustomizableFooter,
      CustomizableHeader,
      HeaderView,
      LoadingContainer,
    },
    data() {
      return {
        isLoading: false,
        isSupervisor: false,
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
        this.$store.dispatch('access/isSupervisor').then((result) => {
          this.isSupervisor = result;
          this.addCustomIconCSS();
        });
      }
    },
    mounted() {
      this.registerToDisplayProgress();
    },
    watch: {
      activeProjectId() {
        if (this.isAuthenticatedUser) {
          this.addCustomIconCSS();
        }
      },
      isAuthenticatedUser(newVal) {
        if (newVal) {
          this.$store.dispatch('access/isSupervisor').then((result) => {
            this.isSupervisor = result;
          });
          this.addCustomIconCSS();
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
