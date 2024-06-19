/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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