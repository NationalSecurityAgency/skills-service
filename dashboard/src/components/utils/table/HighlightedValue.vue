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
import DOMPurify from 'dompurify'
import StringHighlighter from '@/common-components/utilities/StringHighlighter.js'

const props = defineProps({
  value: {
    type: String,
    required: true
  },
  filter: {
    type: String,
    default: ''
  }
})

const trimmedFilter = computed(() => props.filter?.trim() ?? '')
const highlightValue = computed(() => {
  if (!trimmedFilter.value || props.value == null) {
    return null
  }
  const raw = StringHighlighter.highlight(String(props.value), trimmedFilter.value)
  if (!raw) {
    return null
  }
  return DOMPurify.sanitize(raw, { ALLOWED_TAGS: ['mark'], ALLOWED_ATTR: [] })
})
</script>

<template>
  <div v-if="highlightValue != null" v-html="highlightValue" data-cy="highlightedValue" class="max-wrap"/>
  <div v-else data-cy="highlightedValue" class="max-wrap">{{ value }}</div>
</template>

<style scoped>
.max-wrap {
  overflow-wrap: anywhere;
}
</style>