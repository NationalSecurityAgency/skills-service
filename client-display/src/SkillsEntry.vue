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

  import '@fortawesome/fontawesome-free/css/all.css';
  import 'bootstrap/dist/css/bootstrap.css';

  import Vue from 'vue';
  import { debounce } from 'lodash';

  const getDocumentHeight = () => {
    const html = document.documentElement;
    return html.offsetHeight;
  };

  const onHeightChanged = debounce(() => {
    const payload = {
      contentHeight: getDocumentHeight(),
    };
    window.parent.postMessage(`skills::frame-loaded::${JSON.stringify(payload)}`, '*');
  }, 50);

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
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTUzNzUzMzc2LCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6IjVmZjBjNGYyLTRlNjUtNGNmNC1hNjJmLTNkMjBlNTVhYzA4OCIsImNsaWVudF9pZCI6Im1vdmllcyJ9.PAI9050Ux5C8c-Pn8sIMZ1iKdvE8A1J4nhofmoMVRSjGcWtKoASMMR8a46wQfYt_7sqocIXUW1Tgb3RyDo1CGgi23_bI4xghBhZQ-niYnrQhLAjrBEOJBAnpK7EsfTpgFU3X2liOTfSOiFlU5e8yyyH1zJHhGMbi2yKShWrfsKCheTNLo-xToDsBTQ9D0agxpcGjC4PV0rKJ8Gdd_RPCF5bH-vdDR8bO-ZUtITj1EGy0_gVcHUfdg1uqrGYWIiQTBjmP3Edpb5FWRI8hZY5tRvYK5nrNXVsMCQsonsOaX6sksrYENrmDlHaUdCVWbH9l_bFjNJeK5FK2At7bFz2Vmg',
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
        onHeightChanged();
      },
    },
  };
</script>

<style scoped>
  .skills-container {
    max-width: 1100px;
  }
</style>
