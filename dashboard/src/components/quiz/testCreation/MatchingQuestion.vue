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
import {useFieldArray} from "vee-validate";
import {computed} from "vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const { remove, insert, push, replace, update, fields } = useFieldArray('answers');
const appConfig = useAppConfig()
const model = defineModel()

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
    multiPartAnswer: {
      term: '',
      answer: '',
    },
  };
  insert(index + 1, initialValue)
}
function removeAnswer(index) {
  remove(index)
}

const resetAnswers = () => {
  replace([{
      id: null,
      answer: '',
      isCorrect: false,
      multiPartAnswer: { term: '', answer: ''}
    },
    {
      id: null,
      answer: '',
      isCorrect: false,
      multiPartAnswer: { term: '', answer: ''}
    }
  ])
}

defineExpose( {
  resetAnswers
})
</script>

<template>
  <div v-if="model && model.length > 0" class="mt-2">
    <div class="flex flex-1 gap-4 mb-2" v-for="(answer, index) in fields" :key="answer.key">
      <SkillsTextInput
          class="flex flex-1"
          placeholder="Enter a term"
          v-model="answer.value.multiPartAnswer.term"
          :initialValue="answer.value.multiPartAnswer.term"
          :aria-label="`Enter term number ${index+1}`"
          data-cy="termText"
          :id="`term_${index}`"
          :name="`answers[${index}].multiPartAnswer.term`"/>

      <SkillsTextInput
          class="flex flex-1"
          placeholder="Enter an answer"
          v-model="answer.value.multiPartAnswer.value"
          :initialValue="answer.value.multiPartAnswer.value"
          :aria-label="`Enter answer number ${index+1}`"
          data-cy="answerText"
          :id="`answer_${index}`"
          :name="`answers[${index}].multiPartAnswer.value`"/>
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