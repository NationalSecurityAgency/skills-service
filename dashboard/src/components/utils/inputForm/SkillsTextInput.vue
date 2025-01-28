/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { inject, useSlots } from 'vue'
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
const slots = useSlots()
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
    <InputGroup>
      <InputGroupAddon v-if="slots.addOnBefore">
        <slot name="addOnBefore"></slot>
      </InputGroupAddon>
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
          :aria-invalid="!!errorMessage"
          :aria-errormessage="`${name}Error`"
          :aria-describedby="`${name}Error`"/>
      <InputGroupAddon v-if="slots.addOnAfter">
        <slot name="addOnAfter"></slot>
      </InputGroupAddon>
    </InputGroup>
    <Message
        severity="error"
        variant="simple"
        size="small"
        :data-cy="`${name}Error`"
        :id="`${name}Error`">{{ errorMessage || '' }}
    </Message>
    <slot name="footer"/>
  </div>
</template>

<style scoped>

</style>