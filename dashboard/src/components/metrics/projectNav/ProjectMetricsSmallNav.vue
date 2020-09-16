<template>
  <b-button-group>
    <b-button :to="{ name: navItem.pathName }" v-for="navItem in navCards" :key="navItem.title"
              :class="{'bg-primary' : $route.name === navItem.pathName, 'skills-white-text' : $route.name === navItem.pathName}"
              variant="outline-info"> <i :class="navItem.icon"/></b-button>
  </b-button-group>
</template>

<script>
  export default {
    name: 'ProjectMetricsSmallNav',
    props: ['navCards'],
    data() {
      return {
        routerPageName: this.$route.name,
      };
    },
  };
</script>

<style scoped>
.skills-white-text i {
  color: white !important;
}
</style>
