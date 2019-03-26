<template>
  <user-skills
    v-if="token"
    :service-url="serviceUrl"
    :project-id="projectId"
    :token="token"
    @height-change="onHeightChange"/>
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
    name: 'SkillsEntry',
    components: {
      UserSkills,
    },
    data() {
      return {
        serviceUrl: 'http://localhost:8080',
        projectId: 'MyProject',
        // eslint-disable-next-line max-len
        token: null,//'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTUzMzA1ODc1LCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6IjI1YWZlYjQ1LTljOTUtNGU1Ni1iYzEzLTI4MWMxNDUzMDlmMiIsImNsaWVudF9pZCI6Ik15UHJvamVjdCJ9.gVGnvHxyc3Jhn2TUeumuRPHIvU9xj9RTNExrnJqGSTpojFCzIR34_JFPN7FxHzevtMDkNrl-qT242Y6zFS8ItVvXMowjq8_iYCpdm541b47Sn8ZAJxJPVIwyYw1Ps18mQvtQTpQhWclx_y56cEJK7Z3ZEaxQLQf4E7P9fi1sB7zhNh4g4rq_gcN2k2VVcQ6dtVfqDUFezkM8N_Z0Hhd3hkhLdVutXccQaFBjW5ohs_74HorMvu4B8peiy5xa9zEgkpD2wGnQhk4VWrC386Afjn3TnQ1h16yBSTKf69wPgXuQacjERLbykhns6hQxCVz13Azq0fN7gqV6UCbz2zfQmw',
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

<style scoped>

</style>
