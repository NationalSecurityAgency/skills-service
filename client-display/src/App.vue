<template>
  <div id="app">
    <transition name="slide">
      <router-view />
    </transition>
  </div>
</template>

<script>
  import 'bootstrap';
  import 'jquery/dist/jquery.slim';

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
  }
</style>

<style lang="scss">
  @import "./assets/_common.scss";
  @import "~bootstrap/scss/bootstrap";
</style>
