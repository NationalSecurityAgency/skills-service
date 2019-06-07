<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import store from '@/store';

  import Vue from 'vue';

  import Postmate from 'postmate';

  import { debounce } from 'lodash';

  const getDocumentHeight = () => {
    const { body } = document;
    return Math.max(body.scrollHeight, body.offsetHeight);
  };

  const onHeightChanged = debounce(() => {
    if (process.env.NODE_ENV !== 'development') {
      store.state.parentFrame.emit('height-changed', getDocumentHeight());
    }
  }, 0);

  Vue.use({
    install() {
      Vue.mixin({
        updated() {
          onHeightChanged();
        },
      });
    },
  });

  export default {
    mounted() {
      const vm = this;
      if (this.isDevelopmentMode()) {
        this.configureDevelopmentMode();
      } else {
        const handshake = new Postmate.Model({
          updateAuthenticationToken(authToken) {
            store.commit('authToken', authToken);
            UserSkillsService.setToken(authToken);
          },
          updateVersion(newVersion) {
            UserSkillsService.setVersion(newVersion);
            vm.$router.push({
              name: 'home',
            });
          },
        });

        handshake.then((parent) => {
          // Make sure to freeze the parent object so Vuex won't try to make it reactive
          // CORs won't allow this because parent object can't be changed from an iframe
          this.$store.commit('parentFrame', Object.freeze(parent));
          window.addEventListener('resize', onHeightChanged);
          this.onHeightChange();

          UserSkillsService.setServiceUrl(parent.model.serviceUrl);
          UserSkillsService.setProjectId(parent.model.projectId);
          UserSkillsService.setVersion(parent.model.version);
          UserSkillsService.setUserId(parent.model.userId);

          this.$store.state.parentFrame.emit('needs-authentication');

          // No scroll bars for iframe.
          document.body.style['overflow-y'] = 'hidden';
        });
      }
    },
    methods: {
      onHeightChange() {
        onHeightChanged();
      },

      configureDevelopmentMode() {
        if (!this.isValidDevelopmentMode()) {
          const errorMessage = `
            Development mode is not properly configured
            You must create a local file '.env.development.local' that defines:

            VUE_APP_AUTHENTICATION_URL
            VUE_APP_PROJECT_ID
            VUE_APP_SERVICE_URL

            For an example see .env.development.local.example
          `;

          // eslint-disable-next-line no-alert
          alert(errorMessage);
        } else {
          UserSkillsService.setAuthenticationUrl(process.env.VUE_APP_AUTHENTICATION_URL);
          UserSkillsService.setServiceUrl(process.env.VUE_APP_SERVICE_URL);
          UserSkillsService.setProjectId(process.env.VUE_APP_PROJECT_ID);
          this.storeAuthToken();
        }
      },

      isDevelopmentMode() {
        return process.env.NODE_ENV === 'development';
      },

      isValidDevelopmentMode() {
        return process.env.VUE_APP_AUTHENTICATION_URL && process.env.VUE_APP_PROJECT_ID && process.env.VUE_APP_SERVICE_URL;
      },

      storeAuthToken() {
        UserSkillsService.getAuthenticationToken()
          .then((result) => {
            this.$store.commit('authToken', result);
            UserSkillsService.setToken(result.access_token);
          });
      },
    },
  };
</script>

<style>
  @import '../node_modules/animate.css/animate.min.css';
  @import '../node_modules/material-icons/iconfont/material-icons.css';
  @import '../node_modules/material-icons/css/material-icons.css';
  @import '../node_modules/@fortawesome/fontawesome-free/css/all.css';

  #app {
    max-width: 1100px;
    margin: 0 auto;
    text-align: center;
    overflow: hidden;
  }
</style>

<style lang="scss">
  @import "./assets/_common.scss";
  @import "~bootstrap/scss/bootstrap";
</style>
