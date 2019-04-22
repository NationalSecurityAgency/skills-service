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
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTU1OTc0MTk2LCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6IjU0Mjc4MTUwLTYyOTItNDY1NS1hMTgyLWE0NDFiZTlkNjMxZiIsImNsaWVudF9pZCI6Im1vdmllcyJ9.G84IMpc-Q2xx5fQzxnghZqe9nGNdCqDxqJ1Y2PCz5z7o58YnQDyHyC4_bkPFKhhN94jKDsGUWFg3GIyWD2BQ75tkSkD6-G1yT35nL35dXFmg4fzHZwVx0D5RkSpc3HKIQJve7Hu9vSpU1J9CaF0ymEuKJoaB9d8f163lARyJ6IogakLxRK69Ah5EK3M2e3qq2u_Dr120l8DpA-r1h4LrfjoTX84Ec-tdgldW9yae5cAS8xIjIld80-q3oEdmFUkHGeHmRpW4y85VCeN7ox7JNw7KQL1AQxEKswFP4JeREgIxNaoDExuScyuF64sYPcE65Niaw0ZB-keWnvBLbWCjxQ',
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
