<script setup>
defineProps({
  name: String,
  iconClass: String,
})
</script>

<template>
  <div>
    <Avatar :icon="iconClass" size="large" v-badge.success="'✔'" aria-hidden="true"></Avatar>
    <div class="text-green-500 font-semibold"
         :aria-label="`You got the ${name} bonus`"
    >{{ name }}</div>
  </div>
</template>

<style scoped>

</style>