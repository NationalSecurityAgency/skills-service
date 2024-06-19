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