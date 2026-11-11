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
import { computed, watch, ref, nextTick } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue';
import SkillsTextInput from '@/components/utils/inputForm/SkillsTextInput.vue';

defineOptions({
  inheritAttrs: false
})

const props = defineProps({
  a: Object,
  value: Boolean,
  answerNum: Number,
  qNum: Number,
  canSelectMoreThanOne: {
    type: Boolean,
    default: true,
  },
  // form field for the text response of an answer that allows text
  textInputName: String,
})
const emit = defineEmits(['input', 'selection-changed'])
const announcer = useSkillsAnnouncer()
const selected = ref(props.a.selected ? props.a.selected : props.value)
watch(() => props.a.selected, (newValue, oldValue) => {
  selected.value = newValue;
})

const themeState = useSkillsDisplayThemeState()
const selectionIconObject = computed(() => {
  return {
    'text-primary skills-theme-quiz-selected-answer': selected.value,
    'far fa-square': props.canSelectMoreThanOne && !selected.value,
    'far fa-check-square': props.canSelectMoreThanOne && selected.value,
    'far fa-circle': !props.canSelectMoreThanOne && !selected.value,
    'far fa-check-circle': !props.canSelectMoreThanOne && selected.value,
  };
})
const themePrimaryColor = computed(() => {
  return themeState.theme.value?.textPrimaryColor;
})
const themeBackgroundColor = computed(() => {
  return themeState.theme.value?.backgroundColor;
})
const themeTilesBackgroundColor = computed(() => {
  return themeState.theme.value?.tiles?.backgroundColor;
})
const styleObject = computed(() => {
  let res = {};
  if (selected.value) {
    if (themePrimaryColor.value) {
      res = { ...res, 'background-color': themePrimaryColor.value };
    }
    const color = themeTilesBackgroundColor.value ? themeTilesBackgroundColor.value : themeBackgroundColor.value;
    if (color) {
      res = { ...res, color };
    }
  }
  return res;
})
const ariaLabel = computed(() => {
  let res = `Answer number ${props.answerNum} of the question number ${props.qNum}. The answer is ${props.a.answerOption}. Currently ${!selected.value ? 'not ' : ''}selected.`;
  if (props.a.isGraded) {
    if (props.a.isCorrect && selected.value) {
      res = `${res} Answer was correctly selected.`;
    }
    if (!props.a.isCorrect && selected.value) {
      res = `${res} Answer was incorrectly selected.`;
    }
    if (props.a.isCorrect && !selected.value) {
      res = `${res} Answer requires selection but was not selected.`;
    }
    if (!props.a.isCorrect && !selected.value) {
      res = `${res} Answer does not require selection.`;
    }
  }
  return res;
})
const showTextInput = computed(() => props.a.allowCustomText && selected.value && props.textInputName)
const textChanged = (answerText) => {
  emit('selection-changed', { id: props.a.id, selected: selected.value, answerText, isTextEdit: true });
}
const flipSelected = () => {
  if (!props.a.isGraded) {
    selected.value = !selected.value;
    emit('input', selected.value);
    emit('selection-changed', {
      id: props.a.id,
      selected: selected.value,
      answerText: selected.value ? props.a.answerText : undefined,
    });
    if (selected.value && props.a.allowCustomText) {
      nextTick(() => document.getElementById(props.textInputName)?.focus())
    }
    const announceMsg = selected.value
        ? `Selected answer number ${props.answerNum} as the correct answer for the question number ${props.qNum}`
        : `Removed selection from the answer number ${props.answerNum}`;
    announcer.polite(announceMsg);
  }
}
</script>

<template>
  <div class="answer-row"
       v-bind="$attrs"
       @keydown.prevent.space="flipSelected"
       @click="flipSelected"
       :tabindex="a.isGraded ? -1 : 0"
       :class="{ 'bg-gray-100 dark:bg-gray-800 border border-green-400! rounded-border font-semibold sd-theme-tile-background': selected, 'point-cursor hover:border-green-400! border-dashed answer-row-editable skills-theme-quiz-selected-answer-row' : !a.isGraded }"
       :style="styleObject"
       :aria-label="ariaLabel">
    <div class="flex gap-0" :data-cy="`selected_${selected}`">
      <div class="flex">
        <SkillsOverlay v-if="a.isGraded && a.selected !== a.isCorrect" show opacity="0">
          <template #overlay>
            <i v-if="a.selected" class="fa fa-ban text-red-800 skills-theme-quiz-incorrect-answer" style="font-size: 1.5rem;" data-cy="wrongSelection" aria-hidden="true"></i>
            <i v-else class="fa fa-check text-red-800 skills-theme-quiz-incorrect-answer" style="font-size: 1rem;" data-cy="missedSelection" aria-hidden="true"></i>
          </template>
          <span class="checkmark">
             <i :class="selectionIconObject" aria-hidden="true"/>
          </span>
        </SkillsOverlay>
        <span v-else class="checkmark">
           <i :class="selectionIconObject" aria-hidden="true"/>
        </span>
      </div>
      <div class="flex-1 ml-2">
        <span class="answerText" data-cy="answerText">{{ a.answerOption }}</span>
      </div>
    </div>
  </div>
  <SkillsTextInput v-if="showTextInput"
                   class="ml-10 mr-4 mt-1 mb-1"
                   :name="textInputName"
                   :disabled="a.isGraded"
                   :aria-label="`Text response for answer number ${answerNum} of the question number ${qNum}: ${a.answerOption}`"
                   placeholder="Please enter your response"
                   data-cy="customAnswerText"
                   @input="textChanged" />
</template>

<style scoped>
.answer-row {
  padding: 0.2rem 1rem 0rem 1rem  !important;
  margin-bottom: 0.1rem;
  border: 1px dotted transparent;
}

.answer-row-editable:hover {
  border-radius: 0.25rem;
}

.point-cursor {
  cursor: pointer;
}

.checkmark {
  font-size: 1.2rem;
}

.answerText {
  font-size: 0.8rem;
}
</style>