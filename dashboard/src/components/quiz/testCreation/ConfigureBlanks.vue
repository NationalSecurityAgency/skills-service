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
import { computed, ref, watch, onMounted } from 'vue'
import { useFieldArray } from "vee-validate";
import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer.vue';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import {useLog} from "@/components/utils/misc/useLog.js";

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
  numberOfBlanks: {
    type: Number,
    required: true,
    default: 0,
  }
})

onMounted(() => {
  if(props.numberOfBlanks === 0) {
    replace([])
  } else if( props.numberOfBlanks > 0) {
    const existingFields = fields.value.map(item => ({
      id: item.value.id,
      answer: item.value.answer,
      isCorrect: item.value.isCorrect,
    }))
    replace([])

    for(let x = 0; x < props.numberOfBlanks; x++) {
      if(existingFields[x]) {
        push(existingFields[x]);
      } else {
        push({
          id: null,
          answer: '',
          isCorrect: true,
        })
      }
    }
  }
})

watch(() => props.numberOfBlanks, (newValue, oldValue) => {
  if(oldValue < newValue) {
    for(let x = oldValue; x < newValue; x++) {
      push({
        id: null,
        answer: '',
        isCorrect: true,
      })
    }
  } else if(oldValue > newValue) {
    for(let x = newValue; x < oldValue; x++) {
      remove(fields.value.length - 1)
    }
  }
})

const { remove, push, replace, fields } = useFieldArray('answers');
const appConfig = useAppConfig()
const log = useLog()
const isQuizType = computed(() => {
  return props.quizType === 'Quiz';
})
const maxAnswersAllowed = computed(() => {
  return appConfig.maxAnswersPerQuizQuestion;
})
const noMoreAnswers = computed(() => {
  return fields.value && fields.value.length >= maxAnswersAllowed.value
})

const replaceAnswers = (answers) => {
  const fieldSize = fields.value.length
  for(let x = 0; x < fieldSize; x++) {
    remove(0)
  }
  replace(answers)
}
const answerSelected = (answerNumber) => {
  if(QuestionType.isSingleChoice(props.questionType)) {
    resetAnswers(answerNumber);
  }
}

const resetAnswers = (answerToPreserve = null) => {
  const numFields = fields.value.length
  for(let index = 0; index < numFields; index++) {
    fields.value[index].value.isCorrect = false;
  }
  if (answerToPreserve) {
    const adjustedAnswer = answerToPreserve - 1
    fields.value[adjustedAnswer].value.isCorrect = true;
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
          v-if="isQuizType && !QuestionType.isFillInTheBlank(questionType)"
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
    </div>
  </div>
</template>

<style scoped>

</style>