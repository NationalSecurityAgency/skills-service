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
import { inject } from 'vue'
import { useField } from 'vee-validate'
import DatePicker from "primevue/datepicker";
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
    <label v-if="label" :for="`input${name}`" class="block"><span v-if="isRequired">*</span> {{ label }} </label>
    <DatePicker v-model="value"
              v-bind="fallthroughAttributes.inputAttrs.value"
              @keydown.enter="onEnter"
              @input="handleOnInput"
              :inputId="`input${name}`"
              :id="name"
              class="w-full"
              :data-cy="$attrs['data-cy'] || name" />
    <Message v-if="errorMessage"
             severity="error"
             variant="simple"
             size="small"
             :closable="false"
             :data-cy="`${name}Error`"
             :id="`${name}Error`">{{ errorMessage || '' }}</Message>

  </div>
</template>

<style scoped>

</style>