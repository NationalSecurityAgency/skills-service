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
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTU1NzI1MDU1LCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6IjViMGJjZGVjLWI4MjgtNGY0OC05MjdmLTU2MzQ1MWY4ZGNhZSIsImNsaWVudF9pZCI6Im1vdmllcyJ9.QvjPGBdtFQnJrf5oLRwZpNQPNgfP6Fa88xTgIQIe9IH4dYafvHpxQ4BEYCHftZ1e7mRqScgWZJZL4hjU6r8sYMlMpVLh6eFPI4h5f2S7PtBLf3fekQpngCWNPFfSuoNL9qoVRowqKuu0JQnW5ZPDE5Hxlqczvxnx_5t9JjqWq4HPFXjv5w4K8mjiDEZgxjfj2LTl1-iZQSMIfd0Ye6cAvGGPLQt9ZmpL5bM2hTlM8lkXmqXIjTT25YbMArsYTH9BEustApX8IU5EvJtwpXbwFCKypH7Ubn4d6NC2AeoPY6lfDkx3DDWVil5dhZZztp-Io_vHdFSn6CwfXmRFu_vXBA',
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
