<template>
  <div>

  </div>
</template>

<script>
  export default {
    name: 'SkillsDisplayMixin',
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
        return `${this.serviceUrl}/api/projects/${encodeURIComponent(this.projectId)}/token`;
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
