<script setup>
defineProps({
  title: String,
})
</script>

<template>
  <Card :pt="{ content: { class: 'p-0' }, body: { class: 'p-3' } }">
    <template #content>
      <div class="flex">
        <div class="flex-1">
          <span class="text-2xl uppercase" role="heading" aria-level="1">{{ title }}</span>
        </div>
        <div>
          <slot name="rightContent"></slot>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>