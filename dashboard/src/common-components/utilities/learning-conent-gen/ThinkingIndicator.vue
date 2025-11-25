/*
Copyright 2025 SkillTree

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
import {ref, onMounted, onBeforeUnmount} from 'vue'

const props = defineProps({
  value: String,
  speed: {
    type: Number,
    default: 180
  }
})

const activeIndex = ref(0)
const waveInterval = ref(null)


onMounted(() => {
  startWave()
})

onBeforeUnmount(() => {
  if (waveInterval) {
    clearInterval(waveInterval.value)
  }
})
const CHARS_TO_SKIP = [' ']
const startWave = () => {

  waveInterval.value = setInterval(() => {
    activeIndex.value = (activeIndex.value + 1) % props.value.length
    do {
      activeIndex.value = (activeIndex.value + 1) % props.value.length
    } while (CHARS_TO_SKIP.includes(props.value[activeIndex.value]))
  }, props.speed)
}
</script>

<template>
  <div class="inline">
      <span
          v-for="(char, index) in value"
          :key="index"
          class="inline-block transition-all duration-300"
          :class="{
          'text-gray-400': activeIndex === index,
          'text-gray-900': activeIndex !== index
        }"
      >
          {{ char === ' ' ? '\u00A0' : char }}
      </span>
  </div>
</template>

<style scoped>

</style>
