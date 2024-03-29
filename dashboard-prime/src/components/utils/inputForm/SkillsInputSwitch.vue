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
  <InputSwitch
    v-model="value" />
</template>

<style scoped>

</style>