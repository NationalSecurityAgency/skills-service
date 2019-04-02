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
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtdGtpcmtsQGV2b2ZvcmdlLm9yZyIsImV4cCI6MTU1NDI1MzM0NiwiYXV0aG9yaXRpZXMiOlsiUk9MRV9UUlVTVEVEX0NMSUVOVCJdLCJqdGkiOiJmMDRlNmFhNy1mMzM4LTQzYmUtYTkwMS1mZmNiMzkyMWY3YTciLCJjbGllbnRfaWQiOiJtb3ZpZXMifQ.KalXZeXlogCrHS40MXbcgoraUz8mb57HKiv65Z5uRYBzvmpDSrqJh4BqHXjggiqyxefR6xPeFdcWBVoxBTMsFc6Br3sLWVYq4dH_eM0wzLZId-k6g_PX5GuWnjtdj__26XT7xq2Dc4XlQ3cReS6M_94uq7Q1YL6E-Xkf5wWbwA-mfFCMYA_VNGtjvh9TZmEmn-_pvFWVl7gDpdR6QlCbPYmCo981c7nPypiKIjcVdCWGXH3r0q5ElmdlM4NnHthObFIuqucYhQ0fdVronemM934d2lbj_L89tqkInyIO_qrMXq-ezPIV4Vp991J_XuW8DRJUAbqT1DGDm87a_NBBBw',
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
    margin: 0 auto;
  }
</style>
