<script setup>
import { computed, inject, useAttrs } from 'vue'
import { useField } from 'vee-validate'
import Textarea from 'primevue/textarea'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const props = defineProps({
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    required: false
  },
  isRequired: {
    type: Boolean,
    default: false
  },
  autofocus: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: ''
  },
  maxNumChars: Number
})
const emit = defineEmits(['input', 'keydown-enter'])

const numberFormat = useNumberFormat()

const filterAttrs = ['class']
const attrs = useAttrs()
const inputNumFallthroughAttrs = computed(() => {
  return Object.fromEntries(
    Object.entries(attrs).filter(
      ([key]) => !filterAttrs.includes(key)
    )
  )
})

const { value, errorMessage } = useField(() => props.name)
const doSubmitForm = inject('doSubmitForm', null)
const onEnter = (event) => {
  if (doSubmitForm) {
    doSubmitForm()
  }
  emit('keydown-enter', event.target.value)
}

const charactersRemaining = computed(() => {
  return props.maxNumChars - (value?.value ? value.value.length : 0)
})
</script>

<template>
  <div class="field text-left">
    <label :for="name">
      <span v-if="isRequired" class="mr-1 text-color-secondary" aria-label="Required field">*</span>
      <slot name="label">{{ label }}</slot>
    </label>
    <Textarea
      class="w-full"
      type="text"
      v-model="value"
      v-bind="inputNumFallthroughAttrs"
      @input="emit('input', $event.target.value)"
      @keydown.enter="onEnter"
      :data-cy="name"
      :autofocus="autofocus"
      :id="name"
      :maxlength="maxNumChars"
      :disabled="disabled"
      :placeholder="placeholder"
      :class="{ 'p-invalid': errorMessage }"
      :aria-invalid="errorMessage ? null : true"
      :aria-errormessage="`${name}Error`"
      :aria-describedby="`${name}Error`" />
    <div class="sm:flex">
      <div class="flex-1">
        <small
          role="alert"
          class="p-error"
          :data-cy="`${name}Error`"
          :id="`${name}Error`">{{ errorMessage || '' }}</small>
      </div>
      <div v-if="maxNumChars">
        <small
          role="alert"
          :data-cy="`${name}NumCharsRemaining`"
          :id="`${name}RemainingChars`">
          <strong>{{ numberFormat.pretty(charactersRemaining) }}</strong> characters remaining
        </small>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>