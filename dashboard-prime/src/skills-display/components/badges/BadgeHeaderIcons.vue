<script setup>

defineProps({
  badge: Object
})

</script>

<template>
  <div aria-hidden="true">
    <i v-if="badge.gem" class="fas fa-gem text-purple-600 text-2xl"></i>
    <i v-if="badge.global" class="fas fa-globe text-cyan-600 text-2xl"></i>
  </div>
</template>

<style scoped>

</style>