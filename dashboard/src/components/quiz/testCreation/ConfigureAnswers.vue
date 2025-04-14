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
import { computed, ref } from 'vue'
import { useFieldArray } from "vee-validate";
import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer.vue';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';

const model = defineModel()
const props = defineProps({
  quizType: {
    type: String,
    required: true,
  },
  questionType: {
    type: String,
    required: true,
  },
})
const { remove, insert, push, replace, fields } = useFieldArray('answers');
const appConfig = useAppConfig()
const isQuizType = computed(() => {
  return props.quizType === 'Quiz';
})
const maxAnswersAllowed = computed(() => {
  return appConfig.maxAnswersPerQuizQuestion;
})
const noMoreAnswers = computed(() => {
  return fields.value && fields.value.length >= maxAnswersAllowed.value
})
const twoOrLessAnswers = computed(() => {
  return !fields.value || fields.value.length <= 2
})

function addNewAnswer(index) {
  const initialValue = {
    id: null,
    answer: '',
    isCorrect: false,
  };
  insert(index + 1, initialValue)
}
function removeAnswer(index) {
  remove(index)
}
const replaceAnswers = (answers) => {
  replace(answers)
}
const answerSelected = (answerNumber) => {
  if(QuestionType.isSingleChoice(props.questionType)) {
    resetAnswers(answerNumber);
  }
}

const resetAnswers = (answerToPreserve = null) => {
  for(let answerValue in fields.value) {
    const answerValueAsInt = parseInt(answerValue);
    if(answerToPreserve) {
      const adjustedAnswer = answerToPreserve - 1
      if(answerValueAsInt !== adjustedAnswer) {
        if(fields.value[answerValueAsInt].value.isCorrect) {
          answersRef.value[answerValueAsInt].resetValue()
        }
      }
    } else {
      if(fields.value[answerValueAsInt].value.isCorrect) {
        answersRef.value[answerValueAsInt].resetValue()
      }
    }
  }
}

const answersRef = ref([]);

defineExpose( {
  replaceAnswers,
  resetAnswers
})
</script>

<template>
  <div v-if="model && model.length > 0" class="mt-2">
    <div v-for="(answer, index) in fields" :key="answer.key" class="flex flex-wrap items-center gap-0" :data-cy="`answer-${index}`">
      <SelectCorrectAnswer
          v-if="isQuizType"
          :id="`answers[${index}].isCorrect`"
          :answer-number="index+1"
          ref="answersRef"
          :name="`answers[${index}].isCorrect`"
          v-model="answer.value.isCorrect"
          :is-radio-icon="QuestionType.isSingleChoice(questionType)"
          @answerSelected="answerSelected"
          class="flex flex-initial mr-2 field"/>
      <SkillsTextInput
          class="flex flex-1"
          placeholder="Enter an answer"
          v-model="answer.value.answer"
          :initialValue="answer.value.answer"
          :aria-label="`Enter answer number ${index+1}`"
          data-cy="answerText"
          :id="`answer_${index}`"
          :name="`answers[${index}].answer`"/>

      <ButtonGroup class="ml-1">
        <SkillsButton
          :disabled="noMoreAnswers"
          :aria-label="`Add New Answer at index ${index}`"
          data-cy="addNewAnswer"
          outlined
          icon="fas fa-plus"
          @click="addNewAnswer(index)">
        </SkillsButton>
        <SkillsButton
          :disabled="twoOrLessAnswers"
          :aria-label="`Delete Answer at index ${index}`"
          data-cy="removeAnswer"
          outlined
          icon="fas fa-minus"
          @click="removeAnswer(index)">
        </SkillsButton>
      </ButtonGroup>
    </div>
  </div>
</template>

<style scoped>

</style>