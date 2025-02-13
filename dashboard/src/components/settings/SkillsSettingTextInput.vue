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
import { computed } from 'vue'
import SettingsItem from "@/components/settings/SettingsItem.vue";

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: false,
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
  labelWidthInRem: {
    type: Number,
    default: 10
  }
})
const emit = defineEmits(['input'])

const { value, errorMessage } = useField(() => props.name);

const labelClass = computed(() => {
  return props.label ? 'text-secondary w-min-11rem max-w-44' : null
})

const inputClass = computed(() => {
  return props.label ? '' : 'w-full'
})

const inputId = computed(() => {
  return `input${props.name}`
})

</script>
<!--v-bind="projectDisplayNameAttrs"-->
<template>
  <settings-item :label="label" :input-id="inputId" :label-width-in-rem="labelWidthInRem">
    <div class="flex flex-col flex-1">
      <InputText v-model="value"
                 :data-cy="`${name}TextInput`"
                 :id="inputId"
                 type="text"
                 @input="emit('input', [name, $event.target.value])"
                 class="w-full"
                 :placeholder="placeholder"
                 :class="{ 'p-invalid': errorMessage }"
                 :aria-invalid="!!errorMessage"
                 :aria-errormessage="`${name}Error`" />
      <small role="alert" class="p-error" :id="`${name}Error`" :data-cy="`${name}Error`" v-if="errorMessage">{{ errorMessage || '&nbsp;' }}</small>
    </div>
  </settings-item>
</template>

<style scoped>

</style>