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
import { ref, inject } from 'vue'
import { useField } from 'vee-validate'
import InputGroup from 'primevue/inputgroup';
import InputGroupAddon from 'primevue/inputgroupaddon';


const props = defineProps({
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    required: true
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['can-edit', 'keydown-enter'])
const canEdit = ref(false)

function notifyAboutEditStateChange(newValue) {
  emit('can-edit', newValue)
  // if (newValue) {
  //   const element = document.getElementById('idInput');
  //   if (element) {
  //     element.focus()
  //   }
  // }
}

const { value, errorMessage } = useField(() => props.name)
const doSubmitForm = inject('doSubmitForm', null)
const onEnter = (event) => {
  if (doSubmitForm) {
    doSubmitForm()
  }
  emit('keydown-enter', event.target.value)
}

const updateIdValue = (newValue) => {
  value.value = newValue
}
defineExpose({
  updateIdValue
})
</script>

<template>
  <div class="flex flex-col gap-2">
    <label for="idInput">* {{ label }}</label>
    <InputGroup>
      <InputGroupAddon>
        <div style="width: 3.3rem !important;">
          <ToggleSwitch
            v-model="canEdit"
            style="height:1rem !important;"
            size="small"
            name="Enable Id"
            aria-label="Enable input to override auto-generated ID value."
            data-cy="enableIdInput"
            @input="notifyAboutEditStateChange" />
        </div>
      </InputGroupAddon>
    <InputText
      type="text"
      class="w-full"
      :class="{ 'surface-300': !canEdit, 'p-invalid': errorMessage }"
      id="idInput"
      ref="idInput"
      v-model="value"
      @keydown.enter="onEnter"
      :disabled="!canEdit || disabled"
      aria-required="true"
      :aria-invalid="!!errorMessage"
      aria-errormessage="idError"
      aria-describedby="idError"
      data-cy="idInputValue" />

    </InputGroup>
    <Message severity="error"
             variant="simple"
             size="small"
             data-cy="idError"
             :closable="false"
             id="idError">{{ errorMessage || '' }}</Message>
  </div>
</template>

<style scoped>

</style>