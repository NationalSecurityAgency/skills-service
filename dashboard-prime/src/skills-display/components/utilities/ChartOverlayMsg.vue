<script setup>

</script>

<template>
  <div class="absolute left-0 right-0" role="alert">
    <div class="flex justify-content-center">
      <div class="bg-primary-reverse py-2 px-3 border-1 border-round text-center" style="z-index: 1000 !important;">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>