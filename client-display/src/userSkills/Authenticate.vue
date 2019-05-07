<template>
  <div>
    Authenticating ...
  </div>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import router from '@/router.js';

  export default {
    props: {
      targetRoute: Object,
    },
    watch: {
      // call again the method if the route changes
      '$route': 'authenticate'
    },
    mounted() {
      this.authenticate();
    },
    methods: {
      authenticate() {
        if (!this.$store.state.isAuthenticating) {
          this.$store.commit('isAuthenticating', true);
          UserSkillsService.getAuthenticationToken()
            .then((result) => {
              this.$store.commit('authToken', result.access_token);
              UserSkillsService.setToken(result.access_token);
              router.push(this.targetRoute);
            })
            .finally(() => this.$store.commit('isAuthenticating', false));
        }
      },
    },
  }
</script>

<style scoped>

</style>
