/*
Copyright 2025 SkillTree

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

import {ref} from "vue";

const model = defineModel()
const props = defineProps({
  inputId: String,
  label: String,
})
const emits = defineEmits(['change'])
const keys = ref(model.value.split(' + '))
const inputName = props.label.replace(/\s+/g, '_');

const handleKeydown = (event) => {
  event.preventDefault(); // Prevent default behavior
  if (event.ctrlKey) {
    keys.value.push('Ctrl')
  } else {
    keys.value.push(event.key);
  }
  model.value = keys.value.join(' + ');
  emits('change', model.value)
};

const clear = () => {
  keys.value = []
  model.value = '';
};

</script>

<template>
  <div :data-cy="`${inputName}Shortcut`" class="flex flex-col gap-1">
    <label
        :for="`${inputName}ShortcutLabelInput`"
        :id="`${inputName}ShortcutLabel`">{{  label }}:</label>
    <InputGroup>
      <InputText
          v-model="model"
          @keydown="handleKeydown"
          size="small"
          :id="`${inputName}ShortcutLabelInput`"
          placeholder="Press keys..."
          aria-label="Keyboard Shortcut Input"
      />
      <InputGroupAddon class="p-0 m-0">
        <SkillsButton
            icon="fa-solid fa-xmark"
            size="small"
            text
            @click="clear"/>
      </InputGroupAddon>
    </InputGroup>
  </div>
</template>

<style scoped>

</style>