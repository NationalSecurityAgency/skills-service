<template>
  <div id="app">
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

  import '@fortawesome/fontawesome-free/css/all.css';
  import 'bootstrap/dist/css/bootstrap.css';

  const getDocumentHeight = () => {
    const html = document.documentElement;
    return html.offsetHeight;
  };

  export default {
    name: 'app',
    components: {
      UserSkills,
    },
    data() {
      return {
        serviceUrl: 'http://localhost:8080',
        projectId: 'MyProject',
        // eslint-disable-next-line max-len
        token: null,
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
        }
      });
    },
    methods: {
      onHeightChange() {
        const payload = {
          contentHeight: getDocumentHeight(),
        };
        window.parent.postMessage(`skills::frame-loaded::${JSON.stringify(payload)}`, '*');
      },
    },
  };
</script>

