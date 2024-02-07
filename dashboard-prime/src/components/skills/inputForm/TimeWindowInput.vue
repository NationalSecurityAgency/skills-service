<script setup>
import { useStorage } from '@vueuse/core'

const timeWindowCollapsed = useStorage('editSkillTimeWindowCollapsed', true)
const updateCollapsed = (newState) => {
  timeWindowCollapsed.value = newState
}
</script>

<template>
  <Fieldset
    legend="Time Window"
    :pt="{ toggler: { class: 'py-2 px-4' }, content: { class: 'p-0' }, toggleableContent: { class: 'p-0' }, root: { class: 'pt-3' } }"
    :toggleable="true"
    @update:collapsed="updateCollapsed"
    :collapsed="timeWindowCollapsed">
    <div class="flex m-0">
      <SkillsNumberInput
        class="flex-1"
        :min="0"
        showButtons
        label="Hours"
        name="pointIncrementIntervalHrs" />

      <SkillsNumberInput
        class="flex-1 mx-3"
        showButtons
        :min="0"
        :max="60"
        label="Minutes"
        name="pointIncrementIntervalMins" />

      <SkillsNumberInput
        class="flex-1"
        :min="0"
        showButtons
        label="Window's Max Occurrences"
        name="numPointIncrementMaxOccurrences" />

    </div>
  </Fieldset>
</template>

<style scoped>

</style>