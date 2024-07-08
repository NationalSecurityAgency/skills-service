<script setup>

</script>

<template>
  <Card :pt="{ content: { class: 'p-0 h-full' }, body: { class: 'p-0 h-full' } }">
    <template #content>
      <div class="flex flex-column h-full">
        <div>
          <slot name="header"></slot>
        </div>
        <div class="flex-1">
          <slot name="content"></slot>
        </div>
        <div>
          <slot name="footer"></slot>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>