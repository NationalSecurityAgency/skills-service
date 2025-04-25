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
import { useIsSubmitting } from 'vee-validate'
import InputSanitizer from '@/components/utils/InputSanitizer.js'

const props = defineProps({
  nameLabel: {
    type: String,
    required: true
  },
  idLabel: {
    type: String,
    required: true
  },
  nameFieldName: {
    type: String,
    required: true
  },
  idFieldName: {
    type: String,
    required: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  nameToIdSyncEnabled: {
    type: Boolean,
    default: true
  },
  isInline: {
    type: Boolean,
    default: false
  },
  idSuffix: {
    type: String,
    default: ''
  },
  showIdField: {
    type: Boolean,
    default: true
  },
})
const emit = defineEmits(['keydown-enter'])

const canEditProjectId = ref(false)

function updateCanEditProjectId(newVal) {
  canEditProjectId.value = newVal
}

const skillsIdInput = ref(null)

function updateIdBasedOnName(name) {
  if (props.nameToIdSyncEnabled && !canEditProjectId.value) {
    const newId = InputSanitizer.removeSpecialChars(name)
    skillsIdInput.value.updateIdValue(`${newId}${props.idSuffix}`)
  }
}

const isSubmitting = useIsSubmitting()
</script>

<template>
  <div :class="{ 'flex flex-wrap md:flex-nowrap' : isInline }">
    <div :class="{ 'md:mr-1 flex-1' : isInline }">
      <div class="flex gap-4 flex-col sm:flex-row">
        <slot name="beforeName"></slot>
        <div class="flex-1">
          <SkillsTextInput
            class=""
            style="min-width: 16rem;"
            :label="nameLabel"
            :is-required="true"
            :disabled="disabled || isSubmitting"
            :name="nameFieldName"
            :autofocus="true"
            @input="updateIdBasedOnName"
            @keydown-enter="emit('keydown-enter')" />
        </div>
      </div>
    </div>
    <div v-show="showIdField" :class="{ 'md:ml-1 w-full md:w-min lg:w-auto md:flex-1' : isInline }" class="">
      <SkillsIdInput
        ref="skillsIdInput"
        style="min-width: 14rem;"
        :name="idFieldName"
        :disabled="disabled || isSubmitting"
        @can-edit="updateCanEditProjectId"
        :label="idLabel"
        @keydown-enter="emit('keydown-enter')" />
    </div>
  </div>
</template>

<style scoped>

</style>