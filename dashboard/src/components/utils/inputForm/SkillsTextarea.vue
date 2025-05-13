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
import { computed, inject, useAttrs } from 'vue'
import { useField } from 'vee-validate'
import Textarea from 'primevue/textarea'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
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
  placeholder: {
    type: String,
    default: ''
  },
  submitOnEnter: {
    type: Boolean,
    default: true,
  },
  maxNumChars: Number
})
const emit = defineEmits(['input', 'keydown-enter'])

const numberFormat = useNumberFormat()

const { value, errorMessage } = useField(() => props.name)
const doSubmitForm = inject('doSubmitForm', null)
const onEnter = (event) => {
  if (doSubmitForm && props.submitOnEnter) {
    doSubmitForm()
  }
  emit('keydown-enter', event.target.value)
}

const charactersRemaining = computed(() => {
  return props.maxNumChars - (value?.value ? value.value.length : 0)
})
const fallthroughAttributes = useSkillsInputFallthroughAttributes()
</script>

<template>
  <div class="flex flex-col gap-2 text-left" v-bind="fallthroughAttributes.rootAttrs.value">
    <label :for="name">
      <span v-if="isRequired" class="mr-1 text-muted-color" aria-label="Required field">*</span>
      <slot name="label">{{ label }}</slot>
    </label>
    <Textarea
      class="w-full"
      type="text"
      v-model="value"
      v-bind="fallthroughAttributes.inputAttrs.value"
      @input="emit('input', $event.target.value)"
      @keydown.enter="onEnter"
      :data-cy="$attrs['data-cy'] || name"
      :autofocus="autofocus"
      :id="name"
      :maxlength="maxNumChars"
      :disabled="disabled"
      :placeholder="placeholder"
      :class="{ 'p-invalid': errorMessage }"
      :aria-invalid="!!errorMessage"
      :aria-errormessage="`${name}Error`"
      :aria-describedby="`${name}Error`" />
    <div class="sm:flex">
      <div class="flex-1">
        <Message severity="error"
               variant="simple"
               size="small"
               :closable="false"
               :data-cy="`${name}Error`"
               :id="`${name}Error`">{{ errorMessage || '' }}</Message>
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