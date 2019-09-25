<template>
  <div>
    <skills-display
      :options="options"
      :version="skillsVersion"/>
  </div>
</template>

<script>
  import { SkillsDisplay } from '@skills/skills-client-vue';

  export default {
    name: 'InceptionSkills',
    components: {
      SkillsDisplay,
    },
    data() {
      return {
        projectId: 'Inception',
        skillsVersion: 0,
      };
    },
    computed: {
      options() {
        return {
          projectId: this.projectId,
          authenticator: this.authenticator,
          serviceUrl: this.serviceUrl,
          autoScrollStrategy: 'top-of-page',
        };
      },
      serviceUrl() {
        return window.location.origin;
      },
      authenticator() {
        if (this.$store.getters.isPkiAuthenticated) {
          return 'pki';
        }
        return `${this.serviceUrl}/app/projects/${encodeURIComponent(this.projectId)}/users/${encodeURIComponent(this.userId)}/token`;
      },
      userId() {
        let id = null;
        if (this.$store.getters.userInfo) {
          id = this.$store.getters.userInfo.userId;
        }
        return id;
      },
    },
  };
</script>

<style scoped>

</style>
