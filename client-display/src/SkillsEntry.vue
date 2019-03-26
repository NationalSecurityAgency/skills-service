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
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTUzNjcyNjgwLCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6IjdhZjJkYjNiLTU3NjQtNGYxOS1hOGM4LTk4MTA4MWQxZWMxMyIsImNsaWVudF9pZCI6Im1vdmllcyJ9.fGq0lnG-HdVhbfdsgG70dKgLWwePM7OuRSN47jVMxNAaiGhLpH2fAHcxls60P_JR-1sgG0MigS0uTmXLjWnMRx0rQZU8yYZdc3xHmvcJkbJvL-jfXPGMoUTVWxEsaOode8mqhAIlrgeNbShR705LmgeIJR0H3X2_GxQm8kTe-AvXguTK6EzZXJ7YmVX8gXec0G-GbcKCFirnTh2ridFS2l9M1lYX7Lqe4QvhVqDyqUcIIKmzFDnskzaM5OAtOGHg7TlLQQJVZGn84qWmofOTZeo0cIf8i7fGmfp1q3dSUQuzGa-d-UYAYA51w3nT5XfhF-HF7jSZ9HNQ7aTofLqTEQ',
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

</style>
