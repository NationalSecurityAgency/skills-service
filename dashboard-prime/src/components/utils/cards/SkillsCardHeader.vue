<script setup>
defineProps(['title']);
</script>

<template>
  <div class="border-bottom-1 p-3 surface-border surface-100 flex">
    <span class="font-bold flex-1"><slot name="headerIcon" />{{ title }}</span>
    <slot name="headerContent" />
  </div>
</template>

<style scoped>

</style>