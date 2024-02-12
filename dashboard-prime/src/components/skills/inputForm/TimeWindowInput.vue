<script setup>
import { ref } from 'vue'
import { useStorage } from '@vueuse/core'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'

const props = defineProps({
  timeWindowEnabledDefault: {
    type: Boolean,
    default: false
  }
})
const appConfig = useAppConfig()
const timeWindowCollapsed = useStorage('editSkillTimeWindowCollapsed', true)
const updateCollapsed = (newState) => {
  timeWindowCollapsed.value = newState
}
const timeWindowEnabled = ref(props.timeWindowEnabledDefault)
</script>

<template>
  <Fieldset
    legend="Time Window"
    :pt="{ toggler: { class: 'py-2 px-4' }, content: { class: 'p-0' }, toggleableContent: { class: 'p-0' }, root: { class: 'pt-3' } }"
    :toggleable="true"
    @update:collapsed="updateCollapsed"
    :collapsed="timeWindowCollapsed">
    <div class="flex align-items-center mb-2">
      <SkillsCheckboxInput
        class="mb-2"
        :binary="true"
        v-model="timeWindowEnabled"
        inputId="timeWindowEnabled"
        name="timeWindowEnabled"
        :value="true" />
      <label for="timeWindowEnabled" class="ml-2 font-italic">Time Window Enabled</label>
    </div>
    <div class="flex m-0">
      <SkillsNumberInput
        class="flex-1"
        :min="0"
        :max="appConfig.maxTimeWindowInHrs"
        showButtons
        :disabled="!timeWindowEnabled"
        label="Hours"
        name="pointIncrementIntervalHrs" />

      <SkillsNumberInput
        class="flex-1 mx-3"
        showButtons
        :min="0"
        :max="60"
        :disabled="!timeWindowEnabled"
        label="Minutes"
        name="pointIncrementIntervalMins" />

      <SkillsNumberInput
        class="flex-1"
        :min="1"
        :max="appConfig.maxNumPointIncrementMaxOccurrences"
        showButtons
        :disabled="!timeWindowEnabled"
        label="Window's Max Occurrences"
        name="numPointIncrementMaxOccurrences" />

    </div>
  </Fieldset>
</template>

<style scoped>

</style>