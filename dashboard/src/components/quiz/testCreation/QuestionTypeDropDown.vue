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
import SkillsDropDown from "@/components/utils/inputForm/SkillsDropDown.vue";

const props = defineProps({
  name: String,
  options: Array,
  dataCy: String,
  isRequired: {
    type: Boolean,
    default: true,
  },
})
const model = defineModel()
const emit = defineEmits(['selection-changed'])

function questionTypeChanged(inputItem) {
  emit('selection-changed', inputItem)
}
</script>

<template>
  <SkillsDropDown
      :name="name"
      :data-cy="dataCy"
      v-model="model"
      aria-label="Selection Question Type"
      @update:modelValue="questionTypeChanged"
      :isRequired="isRequired"
      :options="options">
    <template #value="slotProps">
      <div v-if="slotProps.value" class="flex gap-1 items-center" :data-cy="`selectionItem_${slotProps.value.id}`" :aria-label="`Select ${slotProps.value.label}`">
        <div class="border rounded-sm px-1 min-w-5">
          <i :class="slotProps.value.icon" aria-hidden="true"></i>
        </div>
        <span class="">{{ slotProps.value.label }}</span>
      </div>
    </template>

    <template #option="slotProps">
      <div class="flex gap-1 items-center py-1" :data-cy="`selectionItem_${slotProps.option.id}`">
        <div class="border rounded-sm px-1 min-w-5">
          <i :class="slotProps.option.icon" aria-hidden="true"></i>
        </div>
        <span class="">{{ slotProps.option.label }}</span><span class="hidden sm:inline">: {{ slotProps.option.description }}</span>
      </div>
    </template>
  </SkillsDropDown>
</template>

<style scoped>

</style>