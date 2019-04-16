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
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTU1NDcxNDE4LCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6IjQzYjdhZjlhLTAzNjMtNDMyYi05MWVjLTVjMzkyMzg5MjJiMyIsImNsaWVudF9pZCI6Im1vdmllcyJ9.D_iwKvEkLbXQj5mI9wF_AEHUr8ObGt9IDIKspfbCUG8pUzzcs0DkpvljH9WnrBND-0PFDF1rbhsY6SZM_ZYs9xLNRMi3xLBJZSRamAwirJkPMH0ZcYVAqqR8mvJagjZFuXDAVV6mmAi3iqLnwo0rCIdk3V-lAc8TkR6rBEFj7G-KRDp6rqjTwNil8wl-pRmtw8XuJv3cV7txfBS-TjkZ4sqIFuK6jJXyvP0QR0N5bJabBVwtJxQ6szuhUdh6QWhbxfwvZAI5XyrNVeDMoS-axkH9SzhvlH8lBVbvwRh5uZOVKLKx5DxFtHBe6-p-pYkHqYCFT0nl87nJeEVYwaj9zQ',
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
