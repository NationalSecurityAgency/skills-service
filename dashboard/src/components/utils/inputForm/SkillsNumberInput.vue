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
  <div class="flex flex-col gap-2" v-bind="fallthroughAttributes.rootAttrs.value">
    <label v-if="label" :for="`input${name}`" class="block"><span v-if="isRequired">*</span> {{ label }} </label>
      <InputNumber
          inputClass="sm:w-[6rem]"
          type="number"
          v-bind="fallthroughAttributes.inputAttrs.value"
          v-model="value"
          @keydown.enter="onEnter"
          @input="handleOnInput"
          :disabled="disabled"
          :data-cy="$attrs['data-cy'] || name"
          :autofocus="autofocus"
          show-buttons
          fluid
          :id="name"
          :inputId="`input${name}`"
          :class="{ 'p-invalid': errorMessage }"
          :aria-invalid="!!errorMessage"
          :aria-errormessage="`${name}Error`" />
    <Message v-if="errorMessage"
             severity="error"
             variant="simple"
             size="small"
             :data-cy="`${name}Error`"
             :id="`${name}Error`">{{ errorMessage || '' }}
    </Message>
  </div>
</template>

<style scoped>

</style>