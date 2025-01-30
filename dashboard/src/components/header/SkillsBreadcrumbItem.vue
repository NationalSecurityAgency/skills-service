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
import { computed } from 'vue'

const props = defineProps({
  label: String,
  icon: String,
  value: {
    type: String,
    required: false
  },
  valueCss: {
    type: String,
    default: 'text-color'
  },
  showSeparator: {
    type: Boolean,
    default: false
  },
})

const maxWidth = computed(() => props.label ? 'max-width:13rem' : '')
</script>

<template>
  <div  class="sd-theme-breadcrumb-item flex items-center" data-cy="breadcrumb-item">
    <i v-if="showSeparator"
       class="fas fa-angle-right ml-1 mr-1"
       aria-hidden="true"/>
    <div v-if="icon" :class="[icon, 'text-color']" aria-hidden="true" class="mr-2"/>
    <div v-if="label" class="text-muted-color mr-1" data-cy="breadcrumbItemLabel">{{ label }}:</div>
    <div class="font-semibold whitespace-nowrap overflow-hidden text-ellipsis" :style="`${maxWidth}`"
          :class="valueCss"
          data-cy="breadcrumbItemValue">{{ value }}</div>
  </div>
</template>

<style scoped>

</style>
