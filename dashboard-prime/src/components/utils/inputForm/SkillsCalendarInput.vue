<script setup>
import { computed, inject, useAttrs } from 'vue'
import { useField } from 'vee-validate'
import Calendar from "primevue/calendar";

const props = defineProps({
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    required: true
  },
  isRequired: {
    type: Boolean,
    default: false
  },
  autofocus: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits(['input', 'keydown-enter'])

const { value, errorMessage } = useField(() => props.name)
const doSubmitForm = inject('doSubmitForm', null)
const onEnter = (event) => {
  if (doSubmitForm) {
    doSubmitForm()
  }
  emit('keydown-enter', event.target.value)
}

const filterAttrs = ['class']
const attrs = useAttrs()
const inputNumFallthroughAttrs = computed(() => {
  return Object.fromEntries(
      Object.entries(attrs).filter(
          ([key]) => !filterAttrs.includes(key)
      )
  )
})
const handleOnInput = (event) => {
  value.value = event.value
}
</script>

<template>
  <div class="field">
    <label :for="`input${name}`" class="block"><span v-if="isRequired">*</span> {{ label }} </label>
    <Calendar v-model="value" inline
              v-bind="inputNumFallthroughAttrs"
              @keydown.enter="onEnter"
              @input="handleOnInput"
              selectionMode="range"
              :inputId="`input${name}`"
              :id="name"
              :data-cy="name" />
    <small
        role="alert"
        class="p-error block"
        :data-cy="`${name}Error`"
        :id="`${name}Error`">{{ errorMessage || '' }}</small>

  </div>
</template>

<style scoped>

</style>