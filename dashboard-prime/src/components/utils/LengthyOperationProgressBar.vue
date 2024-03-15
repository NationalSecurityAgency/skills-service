<script setup>
import { ref } from 'vue'
import ProgressBar from 'primevue/progressbar'
import { useIntervalFn } from '@vueuse/core'

const props = defineProps({
  timeout: {
    type: Number,
    default: 600,
  },
  increment: {
    type: Number,
    default: 4,
  },
})

const max = 100
const current = ref(0)
useIntervalFn(() => {
  if (current.value >= max) {
    current.value = 0;
  } else {
    current.value += props.increment;
  }
}, props.timeout)
</script>

<template>
  <ProgressBar :value="current"></ProgressBar>
</template>

<style scoped>

</style>