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
  showSeparator: {
    type: Boolean,
    default: false
  },
})
</script>

<template>
  <div  class="sd-theme-breadcrumb-item" data-cy="breadcrumb-item">
    <i v-if="showSeparator"
       class="fas fa-angle-right ml-1 mr-1"
       aria-hidden="true"/>
    <span v-if="icon" :class="[icon, 'text-color']" aria-hidden="true" class="mr-2"/>
    <span v-if="label" class="text-color-secondary mr-1" data-cy="breadcrumbItemLabel">{{ label }}:</span>
    <span class="font-semibold" :class="valueCss" data-cy="breadcrumbItemValue">{{ value }}</span>
  </div>
</template>

<style scoped>

</style>
