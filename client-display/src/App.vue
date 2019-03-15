<template>
  <div id="app" style="width: 1000px; margin-left:auto; margin-right: auto;">
    <user-skills
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
    const body = document.body;
    const html = document.documentElement;

    return Math.max( body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight );
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
      const payload = {
        contentHeight: getDocumentHeight(),
      };
      parent.postMessage(`skills::frame-loaded::${JSON.stringify(payload)}`, '*');
    }
  };
</script>

