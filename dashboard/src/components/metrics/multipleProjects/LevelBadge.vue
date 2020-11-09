<template>
  <div>
    <b-badge v-if="level <= 2" variant="warning">Level {{ level }}</b-badge>
    <b-badge v-if="level > 2 && level <=4" variant="info">Level {{ level }}</b-badge>
    <b-badge v-if="level >= 5" variant="success">Level {{ level }}</b-badge>
  </div>
</template>

<script>
  export default {
    name: 'LevelBadge',
    props: ['level'],
  };
</script>

<style scoped>

</style>
