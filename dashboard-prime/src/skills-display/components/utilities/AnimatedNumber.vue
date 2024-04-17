<script setup>
import { ref } from 'vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useIntervalFn } from '@vueuse/core'

const props = defineProps({
  num: Number,
  timeout: {
    type: Number,
    default: 20
  }
})

const numFormat = useNumberFormat()
const displayNumber = ref(0)

const { pause } = useIntervalFn(() => {
  displayNumber.value++
  if (displayNumber.value >= props.num) {
    pause()
    displayNumber.value = props.num
  }
}, props.timeout)
</script>

<template>
  <span :aria-label="displayNumber">{{ numFormat.pretty(displayNumber) }}</span>
</template>

<style scoped>

</style>