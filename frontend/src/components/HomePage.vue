<template>
  <div>
    <my-projects v-if="isAuthenticatedUser"/>
    <div v-else>
      <login-form v-if="!requestAccount"/>
      <request-account-form v-else/>
    </div>
  </div>
</template>

<script>
  import MyProjects from './projects/MyProjects';
  import LoginForm from './access/Login';
  import RequestAccountForm from './access/RequestAccess';

  export default {
    name: 'HomePage',
    components: {
      MyProjects,
      LoginForm,
      RequestAccountForm,
    },
    computed: {
      isAuthenticatedUser() {
        return this.$store.getters.isAuthenticated;
      },
      requestAccount() {
        return this.$route.query.requestAccount;
      },
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
