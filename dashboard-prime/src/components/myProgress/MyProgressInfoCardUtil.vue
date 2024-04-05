<script setup>
defineProps({
  title: String
})
</script>

<template>
  <Card :pt="{ footer: { class: 'p-0 h-6rem' }, content: { class: 'p-0' }, body: { class: 'p-0' } }" class="h-full">
    <template #content>
      <div class="flex h-full gap-2">
        <div class="flex-1 pl-3 pt-3">
          <div class="uppercase">
            <slot name="title">{{ title }}</slot>
          </div>
          <div class="mt-2">
            <slot name="left-content"></slot>
          </div>
        </div>
        <div class="pr-3 text-right">
          <slot name="right-content"></slot>
        </div>
      </div>
      <div>
        <Divider />
        <div class="px-3 pb-3">
          <slot name="footer"></slot>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>