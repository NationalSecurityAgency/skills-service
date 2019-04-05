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
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTU0NTEwNjgxLCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6ImM3NmI3ZjNiLTMxYjMtNGU4NS1iMDM0LTZiZTVhNzkwZWQ3NiIsImNsaWVudF9pZCI6Im1vdmllcyJ9.XpfbmfWwhVEP5qXivTW8DeZFsiWHXdz9xzSKPiH6z9AXsKDba3O-jZSiUgkPzn3b58pbOQyKlE4dr_w7qNwzQtjVT_tnuA7QYY3m4GZdMLi-M0pRGET8CO_J-OTHO0pewWXzdxaixhCVEbaQayAHKbOKwA-UFt_YaPtWUlzFXKH4YSAk0TYRRCX_xoq1WtkhQu8wk9db4lBT5te3Or8YJDRFFqXZiRhoyTLonoSE0THZ26QiDAo6GNDIM5Z6wypuFi5st7jXOyAtjkgXEUeXUiR2N7bzkDg0L6iaGapbjmM3fHzpVIEjp6MUlKVOiAZRcEjfqEjthfEYEfwdmfLg9A',
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
