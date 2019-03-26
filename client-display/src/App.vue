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
