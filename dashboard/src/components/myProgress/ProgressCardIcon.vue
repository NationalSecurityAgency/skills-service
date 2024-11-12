<script setup>
const props = defineProps({
  icon: String,
})
</script>

<template>
  <div class="border-circle w-7rem h-7rem m-2 bg-blue-100 font-bold flex align-items-center justify-content-center" aria-hidden="true">
    <i class="text-cyan-800 text-7xl" :class="icon"></i>
  </div>
</template>

<style scoped>

</style>