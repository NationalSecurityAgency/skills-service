<template>
  <div>
    {{ userInfo }}
    <skills-display :version="skillsVersion"/>
  </div>
</template>

<script>
  import { SkillsDisplay } from '@skills/skills-client-vue';
  import SkillsConfiguration from '@skills/skills-client-configuration';

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
        return `${this.serviceUrl}/admin/projects/${encodeURIComponent(this.projectId)}/token/${encodeURIComponent(this.userId)}`;
      },
      userId() {
        return this.$store.getters.userInfo.userId;
      },
    },
  };
</script>

<style scoped>

</style>
