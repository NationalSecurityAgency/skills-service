<template>
  <div>
    <my-projects v-if="isAuthenticatedUser"/>
  </div>
</template>

<script>
  import MyProjects from './projects/MyProjects';

  export default {
    name: 'HomePage',
    components: {
      MyProjects,
    },
    computed: {
      isAuthenticatedUser() {
        return this.$store.getters.isAuthenticated;
      },
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
