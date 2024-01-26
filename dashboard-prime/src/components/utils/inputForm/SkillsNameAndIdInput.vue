<script setup>
import { ref } from 'vue'
import { useIsSubmitting } from 'vee-validate';
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
  }
})
const emit = defineEmits(['keydown-enter'])

const canEditProjectId = ref(false)
function updateCanEditProjectId(newVal) {
  canEditProjectId.value = newVal
}

const skillsIdInput = ref(null)
function updateProjectId(projName) {
  if (props.nameToIdSyncEnabled && !canEditProjectId.value) {
    const newProjId = InputSanitizer.removeSpecialChars(projName)
    skillsIdInput.value.updateIdValue(newProjId)
  }
}

const isSubmitting = useIsSubmitting();
</script>

<template>
  <div>
    <SkillsTextInput
      :label="nameLabel"
      :is-required="true"
      :disabled="disabled || isSubmitting"
      :name="nameFieldName"
      :autofocus="true"
      @input="updateProjectId"
      @keydown-enter="emit('keydown-enter')" />

    <SkillsIdInput
      ref="skillsIdInput"
      :name="idFieldName"
      :disabled="disabled || isSubmitting"
      @can-edit="updateCanEditProjectId"
      :label="idLabel"
      @keydown-enter="emit('keydown-enter')" />
  </div>
</template>

<style scoped>

</style>