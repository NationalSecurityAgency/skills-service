<template>
    <div id="app">
        <div class="overall-container">
        <home-page/>
        </div>
    </div>
</template>

<script>
  import HomePage from './components/HomePage';

  export default {
    name: 'App',
    components: {
      HomePage,
    },
  };
</script>

<style lang="scss">
    @import "styles/buefy-custom";
</style>

<style>
    @import '../node_modules/@fortawesome/fontawesome-free/css/all.css';
    @import '../node_modules/material-icons/iconfont/material-icons.css';
    @import '../node_modules/material-icons/css/material-icons.css';
    @import '../node_modules/animate.css/animate.css';

    .overall-container {
        min-height: calc(100vh - 50px);
    }
</style>
