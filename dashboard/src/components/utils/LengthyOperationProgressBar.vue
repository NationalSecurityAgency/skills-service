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
  showValue: {
    type: Boolean,
    default: true,
  },
})

const max = 100
const current = ref(0)

const isPausedForReset = ref(false)

// Destructure pause and resume control hooks from VueUse
const { pause, resume } = useIntervalFn(() => {
  // If we are currently holding at 100%, do nothing during this tick
  if (isPausedForReset.value) return

  // Calculate the next step increment safely
  const nextValue = current.value + props.increment

  if (nextValue >= max) {
    current.value = max // Snap explicitly to 100%
    isPausedForReset.value = true
    pause() // Stop the interval ticks temporarily

    // Wait 500ms at full completion before starting over
    setTimeout(() => {
      current.value = 0
      isPausedForReset.value = false
      resume() // Start the interval engine back up
    }, 500)

  } else {
    current.value = nextValue
  }
}, props.timeout)
</script>

<template>
  <ProgressBar :value="current" :show-value="showValue"></ProgressBar>
</template>

<style scoped>

</style>