<template>
  <div>
    {{ userInfo }}
    <skills-display
      :authentication-url="authenticationUrl"
      :version="skillsVersion"
      :project-id="projectId"
      :service-url="serviceUrl"/>
  </div>
</template>

<script>
  import { SkillsDisplay } from '@skills/skills-client-vue/src/index';

  export default {
    name: 'InceptionSkills',
    components: {
      SkillsDisplay,
    },
    data() {
      return {
        projectId: 'Inception',
        authenticationUrl: '',
        skillsVersion: 0,
      };
    },
    created() {
      this.authenticationUrl = `${this.serviceUrl}/app/projects/inception/users/${encodeURIComponent(this.userId)}/token`;
    },
    computed: {
      serviceUrl() {
        return window.location.origin;
      },
      userId() {
        return this.$store.getters.userInfo.userId;
      },
    },
  };
</script>

<style scoped>

</style>
