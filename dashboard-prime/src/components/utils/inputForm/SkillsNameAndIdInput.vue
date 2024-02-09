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
  }
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
  <div :class="{ 'flex' : isInline }">
    <div :class="{ 'mr-1 flex-1' : isInline }">
      <SkillsTextInput
        class=""
        :label="nameLabel"
        :is-required="true"
        :disabled="disabled || isSubmitting"
        :name="nameFieldName"
        :autofocus="true"
        @input="updateIdBasedOnName"
        @keydown-enter="emit('keydown-enter')" />
    </div>
    <div :class="{ 'ml-1' : isInline }">
      <SkillsIdInput
        ref="skillsIdInput"
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