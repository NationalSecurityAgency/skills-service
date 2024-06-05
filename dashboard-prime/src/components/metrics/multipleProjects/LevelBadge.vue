<script setup>
defineProps(['level']);
</script>

<template>
  <div>
    <Badge v-if="level <= 2" severity="warning">Level {{ level }}</Badge>
    <Badge v-if="level > 2 && level <=4" severity="info">Level {{ level }}</Badge>
    <Badge v-if="level >= 5" severity="success">Level {{ level }}</Badge>
  </div>
</template>

<style scoped>

</style>