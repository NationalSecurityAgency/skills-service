<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  import Vue from 'vue';

  import { debounce } from 'lodash';

  const getDocumentHeight = () => {
    const { body } = document;
    return Math.max(body.scrollHeight, body.offsetHeight);
  };

  const onHeightChanged = debounce(() => {
    const payload = {
      contentHeight: getDocumentHeight(),
    };
    window.parent.postMessage(`skills::height-change::${JSON.stringify(payload)}`, '*');
  }, 250);

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
    name: 'app',
    mounted() {
      this.onHeightChange();

      if (process.env.NODE_ENV === 'development') {
        this.configureDevelopmentMode();
      }

      window.addEventListener('message', (event) => {
        const eventData = event.data && event.data.split ? event.data.split('::') : [];
        if (eventData.length === 3 && eventData[0] === 'skills' && eventData[1] === 'data-init') {
          const payload = JSON.parse(eventData[2]);

          UserSkillsService.setAuthenticationUrl(payload.authenticationUrl);
          UserSkillsService.setServiceUrl(payload.serviceUrl);
          UserSkillsService.setProjectId(payload.projectId);

          this.storeAuthToken();

          // No scroll bars for iframe.
          document.body.style['overflow-y'] = 'hidden';
        }
      });
      window.parent.postMessage('skills::frame-initialized::', '*');
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

      isValidDevelopmentMode() {
        return process.env.VUE_APP_AUTHENTICATION_URL && process.env.VUE_APP_PROJECT_ID && process.env.VUE_APP_SERVICE_URL;
      },

      storeAuthToken() {
        UserSkillsService.getAuthenticationToken()
          .then((result) => {
            this.$store.commit('authToken', result.access_token);
            UserSkillsService.setToken(result.access_token);
          });
      },
    },
  };
</script>

<style>
  @import '../node_modules/animate.css/animate.min.css';
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
