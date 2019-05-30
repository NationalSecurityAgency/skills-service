<template>
    <div id="app" class="container-fluid">
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
    @import "~bootstrap/scss/bootstrap";
    @import '~bootstrap-vue/src/index.scss';
</style>

<style>
    #app {
        background-color: #f1f1f1;
    }

    @import '../node_modules/@fortawesome/fontawesome-free/css/all.css';
    @import '../node_modules/material-icons/iconfont/material-icons.css';
    @import '../node_modules/material-icons/css/material-icons.css';
    @import '../node_modules/animate.css/animate.css';

    .overall-container {
        min-height: calc(100vh - 50px);
    }
</style>
