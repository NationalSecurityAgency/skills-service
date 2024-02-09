<script setup>
import { useStorage } from '@vueuse/core'
import { ref } from 'vue'

const timeWindowCollapsed = useStorage('editSkillTimeWindowCollapsed', true)
const updateCollapsed = (newState) => {
  timeWindowCollapsed.value = newState
}
const enabled = ref(false)
</script>

<template>
  <Fieldset
    legend="Time Window"
    :pt="{ toggler: { class: 'py-2 px-4' }, content: { class: 'p-0' }, toggleableContent: { class: 'p-0' }, root: { class: 'pt-3' } }"
    :toggleable="true"
    @update:collapsed="updateCollapsed"
    :collapsed="timeWindowCollapsed">
    <div class="flex align-items-center mb-2">
      <Checkbox
        class="mb-2"
        :binary="true"
        v-model="enabled"
        inputId="timeWindowEnabled"
        name="Time Window"
        :value="true" />
      <label for="timeWindowEnabled" class="ml-2 font-italic">Time Window Enabled</label>
    </div>
    <div class="flex m-0">
      <SkillsNumberInput
        class="flex-1"
        :min="0"
        showButtons
        :disabled="!enabled"
        label="Hours"
        name="pointIncrementIntervalHrs" />

      <SkillsNumberInput
        class="flex-1 mx-3"
        showButtons
        :min="0"
        :max="60"
        :disabled="!enabled"
        label="Minutes"
        name="pointIncrementIntervalMins" />

      <SkillsNumberInput
        class="flex-1"
        :min="0"
        showButtons
        :disabled="!enabled"
        label="Window's Max Occurrences"
        name="numPointIncrementMaxOccurrences" />

    </div>
  </Fieldset>
</template>

<style scoped>

</style>