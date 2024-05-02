<script setup>
import { computed, inject, useAttrs } from 'vue'
import { useField } from 'vee-validate'
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
    <label v-if="label" :for="`input${name}`" class="block"><span v-if="isRequired">*</span> {{ label }} </label>
    <InputGroup>
      <slot name="addOnBefore"></slot>
      <InputNumber
          inputClass="sm:w-3rem"
          class="w-full"
          type="number"
          v-bind="fallthroughAttributes.inputAttrs.value"
          v-model="value"
          @keydown.enter="onEnter"
          @input="handleOnInput"
          :disabled="disabled"
          :data-cy="$attrs['data-cy'] || name"
          :autofocus="autofocus"
          :id="name"
          :inputId="`input${name}`"
          :class="{ 'p-invalid': errorMessage }"
          :aria-invalid="errorMessage ? null : true"
          :aria-errormessage="`${name}Error`" />
        <slot name="addOnAfter"></slot>
    </InputGroup>
    <small v-if="errorMessage"
      role="alert"
      class="p-error block"
      :data-cy="`${name}Error`"
      :id="`${name}Error`">{{ errorMessage || '' }}</small>
  </div>
</template>

<style scoped>

</style>