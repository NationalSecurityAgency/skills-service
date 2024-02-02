<script setup>
import { computed } from 'vue'
import StringHighlighter from '@/common-components/utilities/StringHighlighter.js'

const props = defineProps({
  value: {
    type: String,
    required: true
  },
  filter: {
    type: String,
    default: ''
  }
})

const highlightValue = computed(() =>{
  if (!props.filter) {
    return props.value
  }
  const filterValue = props.filter;
  if (filterValue && filterValue.trim().length > 0) {
    const highlighted = StringHighlighter.highlight(props.value, filterValue);
    return highlighted || props.value;
  } else {
    return props.value;
  }
})

</script>

<template>
  <span v-html="highlightValue" />
</template>

<style scoped>

</style>