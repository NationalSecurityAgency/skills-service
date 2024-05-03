<script setup>
defineProps({
  label: String,
  icon: String,
  value: {
    type: String,
    required: false
  },
  valueCss: {
    type: String,
    default: 'text-color'
  },
})
</script>

<template>
  <div :data-cy="`breadcrumb-${value}`">
    <span v-if="icon" :class="[icon, 'text-color']" aria-hidden="true" class="mr-2"/>
    <span v-if="label" class="text-color-secondary mr-1" data-cy="breadcrumbItemLabel">{{ label }}:</span>
    <span class="font-semibold" :class="valueCss" data-cy="breadcrumbItemValue">{{ value }}</span>
  </div>
</template>

<style scoped>

</style>
