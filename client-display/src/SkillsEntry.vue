<template>
  <div
    ref="containerElement"
    class="skills-container">
    <user-skills
      v-if="token"
      :service-url="serviceUrl"
      :project-id="projectId"
      :token="token"
      @height-change="onHeightChange"/>
  </div>
</template>

<script>
  import UserSkills from '@/userSkills/UserSkills.vue';

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
    window.parent.postMessage(`skills::frame-loaded::${JSON.stringify(payload)}`, '*');
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
    name: 'SkillsEntry',
    components: {
      UserSkills,
    },
    data() {
      return {
        serviceUrl: 'http://localhost:8080',
        projectId: 'movies',
        // eslint-disable-next-line max-len
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJVc2VyIDAiLCJleHAiOjE1NTYxNDY4ODYsImF1dGhvcml0aWVzIjpbIlJPTEVfVFJVU1RFRF9DTElFTlQiXSwianRpIjoiM2JmMWNiMjYtYmM3Ny00OTJhLWJiYTMtODBjODU3NzgyN2I5IiwiY2xpZW50X2lkIjoibW92aWVzIn0.gmz047XyySi9Lbrw4hEksSY1ly2rUpHWqKYsEtUMh8IOi7XQBxLCbiFCzcknjzLrbLf-2vIaRWoc1czottwyIElq3c2SK1sZWO--5sVmaXb2-YwG3rMJVazPUFTnKwPzVoQ9cjNmihrJqbia1ozc7wFDqUlYuVq1TZ-CV1C2W5v9PEClgdc3onaynxIQa0CL28zomkyvv31CVzr3S3yc7HyVrT6ZDnp9QD2KF2S3nQV_aP_af9m_O3OuWrOkCWztFBTnKbAc7czvNd1LN1iobssBUKZaj3fbMRfcCWiBLZ0j7gQvZ9sBhBE29uIs7dUvpx0zvkY56I8JSojb_Z5GHA',
      };
    },
    mounted() {
      this.onHeightChange();
      window.addEventListener('message', (event) => {
        const eventData = event.data && event.data.split ? event.data.split('::') : [];
        if (eventData.length === 3 && eventData[0] === 'skills' && eventData[1] === 'data-init') {
          const payload = JSON.parse(eventData[2]);
          this.serviceUrl = payload.serviceUrl;
          this.projectId = payload.projectId;
          this.token = payload.authToken;

          // No scroll bars for iframe.
          document.body.style['overflow-y'] = 'hidden';
        }
      });
    },
    methods: {
      onHeightChange() {
        onHeightChanged();
      },
    },
  };
</script>

<style scoped>

</style>
