<script setup>
import { computed, inject, useAttrs } from 'vue'
import { useField } from 'vee-validate'
import Calendar from "primevue/calendar";
import {
  useSkillsInputFallthroughAttributes
} from '@/components/utils/inputForm/UseSkillsInputFallthroughAttributes.js'

defineOptions({
  inheritAttrs: false
})
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

const fallthroughAttributes = useSkillsInputFallthroughAttributes()
const handleOnInput = (event) => {
  value.value = event.value
}
</script>

<template>
  <div class="field" v-bind="fallthroughAttributes.rootAttrs.value">
    <label :for="`input${name}`" class="block"><span v-if="isRequired">*</span> {{ label }} </label>
    <Calendar v-model="value" inline
              v-bind="fallthroughAttributes.inputAttrs.value"
              @keydown.enter="onEnter"
              @input="handleOnInput"
              :inputId="`input${name}`"
              :id="name"
              :data-cy="$attrs['data-cy'] || name" />
    <small
        role="alert"
        class="p-error block"
        :data-cy="`${name}Error`"
        :id="`${name}Error`">{{ errorMessage || '' }}</small>

  </div>
</template>

<style scoped>

</style>