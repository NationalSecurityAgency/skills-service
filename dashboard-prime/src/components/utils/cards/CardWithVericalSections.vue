<script setup>

</script>

<template>
  <Card :pt="{ content: { class: 'p-0' }, body: { class: 'p-0' } }">
    <template #content>
      <slot name="header"></slot>
      <slot name="content"></slot>
      <slot name="footer"></slot>
    </template>
  </Card>
</template>

<style scoped>

</style>