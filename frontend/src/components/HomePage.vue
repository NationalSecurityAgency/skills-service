<template>
  <div>
    <my-projects/>
  </div>
</template>

<script>
  import MyProjects from './projects/MyProjects';

  export default {
    name: 'HomePage',
    components: {
      MyProjects,
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
