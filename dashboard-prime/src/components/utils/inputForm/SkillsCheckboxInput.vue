<script setup>
import { useField } from 'vee-validate'
const props = defineProps({
  name: {
    type: String,
    required: true
  },
})
const { value } = useField(() => props.name)
</script>

<template>
  <Checkbox
    v-model="value" />
</template>

<style scoped>

</style>