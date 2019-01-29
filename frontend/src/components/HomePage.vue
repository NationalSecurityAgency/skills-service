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
    mounted() {
      this.addCustomIconCSS();
    },
    methods: {
      addCustomIconCSS() { // This must be done here AFTER authentication
        const head = document.getElementsByTagName('head')[0];
        const linkTag = document.createElement('style');

        linkTag.rel = 'stylesheet';
        linkTag.href = '/icons/custom-icon';

        head.appendChild(linkTag);
      },
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
