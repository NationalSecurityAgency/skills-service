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
import { useField } from 'vee-validate';
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue';

const emit = defineEmits(['answer-selected'])
const props = defineProps({
  name: {
    type: String,
    required: false,
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
  fontSize: {
    type: String,
    default: '2.1rem',
  },
  isRadioIcon: {
    type: Boolean,
    default: false,
  },
  markIncorrect: {
    type: Boolean,
    default: false,
  },
  answerNumber: {
    type: Number,
  },
})

const { value, errorMessage } = useField(() => props.name, undefined, {syncVModel: true});
const model = defineModel()
const flipSelected = () =>{
  if (!props.readOnly && !props.isRadioIcon){
    value.value = !value.value
    model.value = !model.value
  } else if (!props.readOnly && props.isRadioIcon) {
    value.value = true;
    model.value = true
  }
  emit('answer-selected', parseInt(props.answerNumber))
}
const resetValue = () => {
  value.value = false;
}

defineExpose({
  resetValue
})
</script>

<template>
  <SkillsOverlay :show="readOnly && markIncorrect" opacity="0">
    <template #overlay>
      <i v-if="model" class="fa fa-ban text-red-500" style="font-size: 1.5rem;" data-cy="wrongSelection"></i>
      <i v-else class="fa fa-check text-red-500 " style="font-size: 1rem;" data-cy="missedSelection"></i>
    </template>
    <div v-on:keydown.space="flipSelected"
         @click="flipSelected"
         :tabindex="readOnly ? -1 : 0"
         role="checkbox"
         :aria-label="`Select answer number ${answerNumber} as the correct answer`"
         :aria-checked="`${model}`"
         :class="{ 'cursorPointer': !readOnly}"
         data-cy="selectCorrectAnswer">
      <i v-if="!model" data-cy="notSelected" class="far" :class="{ 'fa-square' : !isRadioIcon, 'fa-circle': isRadioIcon }" :style="{ 'font-size': fontSize }"></i>
      <i v-if="model" data-cy="selected" class="far text-primary" :class="{ 'fa-check-square' : !isRadioIcon, 'fa-check-circle': isRadioIcon }" :style="{ 'font-size': fontSize }"></i>
    </div>
  </SkillsOverlay>

</template>

<style scoped>
.cursorPointer {
  cursor: pointer;
}
</style>