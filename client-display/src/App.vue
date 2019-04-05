<template>
  <div id="app">
    <transition name="slide">
      <router-view />
    </transition>
  </div>
</template>

<script>
  export default {
    name: 'app',
  };
</script>

<style>
  @import '../node_modules/animate.css/animate.min.css';
</style>

<style lang="scss">
  @import "./assets/_common.scss";
</style>
