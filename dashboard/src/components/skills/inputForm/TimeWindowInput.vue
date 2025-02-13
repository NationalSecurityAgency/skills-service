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
import { useStorage } from '@vueuse/core'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useField } from 'vee-validate';
import { inject, watch } from 'vue';

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  }
})
const appConfig = useAppConfig()
const timeWindowCollapsed = useStorage('editSkillTimeWindowCollapsed', true)
const setFieldValue = inject('setFieldValue')
const { value, errorMessage } = useField(() => 'timeWindowEnabled')

watch(() => value.value, (newValue) => {
  resetTimeWindow(newValue)
})
const updateCollapsed = (newState) => {
  timeWindowCollapsed.value = newState
}
const resetTimeWindow = (checked) => {
  if (!checked) {
    setFieldValue('pointIncrementIntervalHrs', 8)
    setFieldValue('pointIncrementIntervalMins', 0)
    setFieldValue('numPointIncrementMaxOccurrences', 1)
  }
}
</script>

<template>
  <Fieldset
    legend="Time Window"
    :pt="{ toggler: { class: 'py-2 px-3' }, content: { class: 'p-0' }, toggleableContent: { class: 'p-0' }, root: { class: 'm-0' } }"
    :toggleable="true"
    data-cy="timeWindowInput"
    @update:collapsed="updateCollapsed"
    :collapsed="timeWindowCollapsed">
    <div class="flex items-center mb-2 mt-4 mx-2">
      <SkillsCheckboxInput
        class="mb-2"
        :binary="true"
        v-model="value"
        :disabled="disabled"
        inputId="timeWindowEnabled"
        name="timeWindowEnabled"
        data-cy="timeWindowCheckbox"
        :value="true" />
      <label for="timeWindowEnabled" class="ml-2 italic" :class="{ 'text-color-secondary' : disabled }">Time Window Enabled</label>
    </div>
    <div class="flex m-0 flex-wrap flex-col lg:flex-row">
      <SkillsNumberInput
        class="flex-1 mx-2"
        :class="{ 'text-color-secondary' : disabled }"
        showButtons
        :disabled="!value"
        label="Hours"
        name="pointIncrementIntervalHrs" />

      <SkillsNumberInput
        class="flex-1 mx-2"
        :class="{ 'text-color-secondary' : disabled }"
        showButtons
        :min="0"
        :disabled="!value"
        label="Minutes"
        name="pointIncrementIntervalMins" />

      <SkillsNumberInput
        class="flex-1 mx-2"
        :class="{ 'text-color-secondary' : disabled }"
        :min="1"
        showButtons
        :disabled="!value"
        label="Window's Max Occurrences"
        name="numPointIncrementMaxOccurrences" />

    </div>
  </Fieldset>
</template>

<style scoped>

</style>