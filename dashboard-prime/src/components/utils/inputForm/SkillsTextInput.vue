<script setup>
import { inject } from 'vue'
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
    required: true,
  },
  label: {
    type: String,
    required: false,
  },
  isRequired: {
    type: Boolean,
    default: false,
  },
  autofocus: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: ''
  },
})
const emit = defineEmits(['input', 'keydown-enter'])

const { value, errorMessage } = useField(() => props.name);
const doSubmitForm = inject('doSubmitForm', null)
const onEnter = (event) => {
  if (doSubmitForm) {
    doSubmitForm()
  }
  emit('keydown-enter', event.target.value)
}
const fallthroughAttributes = useSkillsInputFallthroughAttributes()
</script>

<template>
  <div class="field text-left" v-bind="fallthroughAttributes.rootAttrs.value">
    <label v-if="label" :for="name"><span v-if="isRequired">*</span> {{ label }} </label>
    <InputText
      class="w-full"
      type="text"
      v-model="value"
      v-bind="fallthroughAttributes.inputAttrs.value"
      @input="emit('input', $event.target.value)"
      @keydown.enter="onEnter"
      :data-cy="$attrs['data-cy'] || name"
      :autofocus="autofocus"
      :id="name"
      :disabled="disabled"
      :placeholder="placeholder"
      :class="{ 'p-invalid': errorMessage }"
      :aria-invalid="errorMessage ? null : true"
      :aria-errormessage="`${name}Error`"
      :aria-describedby="`${name}Error`" />
      <slot name="footer" />
      <small
        role="alert"
        class="p-error"
        :data-cy="`${name}Error`"
        :id="`${name}Error`">{{ errorMessage || '' }}</small>
  </div>
</template>

<style scoped>

</style>