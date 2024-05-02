<script setup>
const props = defineProps({
  badge: Object
})
</script>

<template>
  <div>
    <i v-if="badge.gem" class="fas fa-gem text-purple-600 text-2xl" aria-label="This is a gem badge"></i>
    <i v-if="badge.global" class="fas fa-globe text-cyan-600 text-2xl" aria-label="this is a global badge"></i>
  </div>
</template>

<style scoped>

</style>