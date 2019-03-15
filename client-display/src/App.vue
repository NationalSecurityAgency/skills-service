<template>
  <div id="app" style="width: 1000px; margin-left:auto; margin-right: auto;">
    <user-skills
      v-if="token"
      :service-url="serviceUrl"
      :project-id="projectId"
      :token="token"/>
  </div>
</template>

<script>
  import UserSkills from '@/userSkills/UserSkills.vue';

  import '@fortawesome/fontawesome-free/css/all.css';
  import 'bootstrap/dist/css/bootstrap.css';

  const getDocumentHeight = () => {
    const { body } = document;
    const html = document.documentElement;

    return Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight);
  };

  export default {
    name: 'app',
    components: {
      UserSkills,
    },
    data() {
      return {
        serviceUrl: null,
        projectId: null,
        token: null,
      };
    },
    mounted() {
      window.addEventListener('message', (event) => {
        const eventData = event.data && event.data.split ? event.data.split('::') : [];
        if (eventData.length === 3 && eventData[0] === 'skills' && eventData[1] === 'data-init') {
          const payload = JSON.parse(eventData[2]);
          this.serviceUrl = payload.serviceUrl;
          this.projectId = payload.projectId;
          this.token = payload.authToken;
        }
      });
      const payload = {
        contentHeight: getDocumentHeight(),
      };
      window.parent.postMessage(`skills::frame-loaded::${JSON.stringify(payload)}`, '*');
    },
  };
</script>

