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
import { useField } from 'vee-validate'
import {
  useSkillsInputFallthroughAttributes
} from '@/components/utils/inputForm/UseSkillsInputFallthroughAttributes.js'
import {onMounted, toRaw, watch} from "vue";

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
  options: {
    type: Object,
    required: true,
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
  ignoreVeeValidate: {
    type: Boolean,
    default: false
  },
})
const model = defineModel()

const { value, errorMessage } = useField(() => props.name);
const fallthroughAttributes = useSkillsInputFallthroughAttributes()

onMounted(() => {
  if (!props.ignoreVeeValidate) {
    const rawValue = toRaw(value.value)
    if (rawValue && model.value !== rawValue) {
      model.value = {...rawValue, isInitialLoad: true}
    }
  } else {
    value.value = model.value
  }
})
watch(value, (newValue) => {
  const rawValue = toRaw(newValue)
  if (model.value !== rawValue) {
    model.value = rawValue
  }
})
</script>

<template>
  <div class="field" v-bind="fallthroughAttributes.rootAttrs.value">
    <label v-if="label" :for="name"><span v-if="isRequired">*</span> {{ label }}:</label>
    <Select v-model="value"
              :options="options"
              class="w-full"
              v-bind="fallthroughAttributes.inputAttrs.value"
              :autofocus="autofocus"
              :id="name"
              :disabled="disabled"
              :data-cy="$attrs['data-cy'] || name"
              :class="{ 'p-invalid': errorMessage }"
              :aria-invalid="!!errorMessage"
              :aria-errormessage="`${name}Error`">
      <template #value="slotProps">
        <slot name="value" :value="slotProps.value"></slot>
      </template>
      <template #option="slotProps">
        <slot name="option" :option="slotProps.option"></slot>
      </template>
    </Select>
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