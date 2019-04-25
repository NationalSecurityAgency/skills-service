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
      window.addEventListener('message', (event) => {
        const eventData = event.data && event.data.split ? event.data.split('::') : [];
        if (eventData.length === 3 && eventData[0] === 'skills' && eventData[1] === 'data-init') {
          const payload = JSON.parse(eventData[2]);

          UserSkillsService.setAuthenticationUrl(payload.authenticationUrl);
          UserSkillsService.setServiceUrl(payload.serviceUrl);
          UserSkillsService.setProjectId(payload.projectId);

          UserSkillsService.getAuthenticationToken()
            .then((result) => {
              this.$store.commit('authToken', result.access_token);
              UserSkillsService.setToken(result.access_token);
            });

          // No scroll bars for iframe.
          document.body.style['overflow-y'] = 'hidden';
        }
      });
      window.parent.postMessage(`skills::frame-initialized::`, '*');
    },
    methods: {
      onHeightChange() {
        onHeightChanged();
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
