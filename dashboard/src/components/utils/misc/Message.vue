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
import Message from 'primevue/message'
import {computed, useAttrs, useSlots} from "vue";

const props = defineProps({
  icon: String,
  marginY: {
    type: Number,
    default: 3
  },
  closable: {
    type: Boolean,
    default: true
  }
})
const attrs = useAttrs()
const slots = useSlots()

const msgIcon = computed(() => {
  if (props.icon) {
    return props.icon
  }
  if (attrs.variant === 'simple') {
    return null
  }
  const severityIcons = {
    success: 'fas fa-check-circle',
    error: 'fas fa-exclamation-triangle',
  };
  return severityIcons[props.severity] || 'fas fa-exclamation-circle';
})

const marginCss = computed(() => {
  if (attrs.variant === 'simple') {
    return ''
  }
  return `my-${props.marginY}`
})
</script>

<template>
  <Message
      :pt="{ text: { class: 'flex-1' } }"
      :closable="closable"
      :class="marginCss" :icon="msgIcon"><slot /><template v-if="slots.container" #container><slot name="container" /></template></Message>
</template>

<style scoped>

</style>