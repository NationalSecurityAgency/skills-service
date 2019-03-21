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
    const { body } = document;
    const html = document.documentElement;

    console.log('body.scrollHeight ',body.scrollHeight);
    console.log('body.offsetHeight ',body.offsetHeight);
    console.log('html.clientHeight ',html.clientHeight);
    console.log('*** html.scrollHeight ',html.scrollHeight);
    console.log('html.scrollHeight ',html.offsetHeight);

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
        token: null,//'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTUzMTQ1NTE3LCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6ImIyNzBlYWI3LTIxMGMtNGFmOS1iZmU5LTIwZjk5MTU0ZTg2ZCIsImNsaWVudF9pZCI6Ik15UHJvamVjdCJ9.CWj7dcWGKFzy5qn8J8mxGbY3lUS05t-SWc9KdcjDSmyTm2MxlR9e6lwLwFsuvKHoY5Lz0orkXMcStu6ojCaDClg4DhgZoD1S4SdQVvHDN_84XMp5ppVLPBmadbc_hzW9p9Hz7iSNuQlotC2jNmzeRv5GZuYTyecnviGp-UusEPRJHE7S66ALifv-ogSMfbb-CX9gKkLEeV-D64YE6Ku7rcXRqC1eaw7ICyEvfTCh0M6yvvTdRr6fZElS1d3AY_QrpvRx0180eRrxWZVCNp82fgNjLx8qbHZV1wKWaqzF6Oe5dGhDHEPCcWiQCq52uhbJQUGY1IklOrStr2_s3OHDpg',
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
        console.log('sending contentHeight', payload.contentHeight);
        window.parent.postMessage(`skills::frame-loaded::${JSON.stringify(payload)}`, '*');
      },
    },
  };
</script>

