/*
Copyright 2020 SkillTree

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
import { computed } from 'vue';

const model = defineModel()
const props = defineProps({
  readOnly: {
    type: Boolean,
    default: false,
  },
  fontSize: {
    type: String,
    default: '2.1rem',
  },
})
const checkBinding = computed(() => {
  if (!props.readOnly) {
    return {
      tabindex: '0',
      "aria-label": 'Select as the correct answer'
    }
  }
  return { tabindex: '-1' }
})
const flipSelected = () => {
  if (!props.readOnly) {
    model.value = !model.value;
  }
}
</script>

<template>
  <span v-on:keydown.space="flipSelected" @click="flipSelected" v-bind="checkBinding" :class="{ 'cursorPointer': !readOnly}">
    <i v-if="!model" class="far fa-square" :style="{ 'font-size': fontSize }" aria-hidden="true"></i>
    <i v-if="model" class="far fa-check-square text-success" :style="{ 'font-size': fontSize }" aria-hidden="true"></i>
  </span>
</template>

<style scoped>
i {
  color: #b6b5b5;
}
.cursorPointer {
  cursor: pointer;
}
</style>
