<template>
  <div>
    <skills-display :version="skillsVersion"/>
  </div>
</template>

<script>
  import { SkillsDisplay, SkillsConfiguration } from '@skills/skills-client-vue';

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
    created() {
      SkillsConfiguration.configure({
        projectId: this.projectId,
        authenticator: this.authenticator,
        serviceUrl: this.serviceUrl,
      });
    },
    computed: {
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
        return this.$store.getters.userInfo.userId;
      },
    },
  };
</script>

<style scoped>

</style>
