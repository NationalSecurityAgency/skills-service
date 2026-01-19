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
import { computed } from 'vue'
import { useField, useFieldError, useIsFieldDirty } from 'vee-validate'

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: false,
  },
  isRequired: {
    type: Boolean,
    default: false,
  },
  autofocus: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: ''
  },
  textAreaRows: {
    type: Number,
    default:8
  }
})
const emit = defineEmits(['resetToDefault', 'updateSetting'])
const { value: isDefault } = useField(() => `${props.name}IsDefault`);
const isDirty = useIsFieldDirty(props.name)
const errorMessage = useFieldError(props.name)

const  isDefaultTagLabel = computed(() => {
  return isDefault.value ? 'Default' : 'Modified'
})
const isDefaultTagIcon = computed(() => {
  return isDefault.value ? 'fas fa-check-circle' : 'fas fa-pen'
})
const isDefaultTagSeverity = computed(() => {
  return isDefault.value ? 'success' : 'warn'
})

</script>

<template>
  <div :data-cy="`aiPromptSetting-${name}`">
    <SkillsTextarea :name="name" :rows="textAreaRows" @input="isDefault = false">
      <template #label>
        <div>
          <span>{{ label }}</span>
          <Tag
              :severity="isDefaultTagSeverity"
              class="ml-2" :data-cy="`isDefaultValueTag-${name}`">
            <i :class="`${isDefaultTagIcon} mr-1`" aria-hidden="true"></i> {{ isDefaultTagLabel }}
          </Tag>
        </div>
      </template>
    </SkillsTextarea>

    <SkillsButton label="Save"
                  :data-cy="`updateSetting-${name}`"
                  icon="fas fa-arrow-circle-right"
                  @click="emit('updateSetting', name)"
                  outlined
                  class="mb-4"
                  :disabled="!isDirty || !!errorMessage"
                  severity="info"
                  size="small"/>
    <SkillsButton label="Reset to Default"
                  :data-cy="`resetToDefault-${name}`"
                  icon="fas fa-rotate-left"
                  @click="emit('resetToDefault', name)"
                  outlined
                  class="mt-1 mb-6 ml-2"
                  :disabled="!!isDefault"
                  severity="info"
                  size="small"/>
    <Tag v-if="isDirty" class="ml-2" severity="warn" :data-cy="`unsavedChanges-${name}`"><i class="fas fa-exclamation-circle mr-1" aria-hidden="true"></i>Unsaved Changes</Tag>
  </div>
</template>

<style scoped>

</style>