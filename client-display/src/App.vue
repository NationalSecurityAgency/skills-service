<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
  export default {
    name: 'app',
  };
</script>

<style>
  @import '../node_modules/animate.css/animate.min.css';
  @import '../node_modules/@fortawesome/fontawesome-free/css/all.css';

  #app {
    max-width: 1100px;
    margin: 0 auto;
    text-align: center;
    overflow: hidden;
  }
</style>

<style lang="scss">
  @import "./assets/_common.scss";
  @import "~bootstrap/scss/bootstrap";
</style>
