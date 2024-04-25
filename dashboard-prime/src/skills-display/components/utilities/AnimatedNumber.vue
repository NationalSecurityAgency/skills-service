<script setup>
import { onMounted, ref, watch } from 'vue'
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

watch(() => props.num, () => {
  doAnimate()
})
const doAnimate = () => {
  const { pause } = useIntervalFn(() => {
    let change = (props.num - displayNumber.value) / 10;
    change = change >= 0 ? Math.ceil(change) : Math.floor(change);
    displayNumber.value += change;

    if (displayNumber.value >= props.num) {
      pause()
      displayNumber.value = props.num
    }
  }, props.timeout)
}

onMounted(() => {
  doAnimate()
})
</script>

<template>
  <span :aria-label="displayNumber">{{ numFormat.pretty(displayNumber) }}</span>
</template>

<style scoped>

</style>