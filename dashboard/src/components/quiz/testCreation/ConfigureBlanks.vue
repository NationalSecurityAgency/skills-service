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
import { watch, onMounted } from 'vue'
import { useFieldArray } from "vee-validate";

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
    default: 0,
  }
})

onMounted(() => {
  if(props.numberOfBlanks === 0) {
    replace([])
  } else {
    const answers = [];

    for(let x = 0; x < props.numberOfBlanks; x++) {
      if(fields.value[x]) {
        answers.push({
          id: fields.value[x].value.id,
          answer: fields.value[x].value.answer,
          isCorrect: true
        });
      } else {
        answers.push({
          id: null,
          answer: '',
          isCorrect: true
        })
      }
    }

    replace(answers);
  }
})

watch(() => props.numberOfBlanks, (newValue) => {
  if(fields.value.length < newValue) {
    const itemsToAdd = newValue - fields.value.length;

    for(let x = 0; x < itemsToAdd; x++) {
      push({
        id: null,
        answer: '',
        isCorrect: true,
      })
    }
  } else if(fields.value.length > newValue) {
    const itemsToRemove = fields.value.length - newValue
    for (let x = 0; x < itemsToRemove; x++) {
      remove(fields.value.length - 1)
    }
  }
})

const { remove, push, replace, fields } = useFieldArray('answers');

const replaceAnswers = (answers) => {
  const fieldSize = fields.value.length
  for(let x = 0; x < fieldSize; x++) {
    remove(0)
  }
  replace(answers)
}
const resetAnswers = () => {
  const numFields = fields.value.length
  for(let index = 0; index < numFields; index++) {
    fields.value[index].value.isCorrect = true;
  }
}

defineExpose( {
  replaceAnswers,
  resetAnswers
})
</script>

<template>
  <div v-if="model" class="mt-2">
    <div v-for="(answer, index) in fields" :key="answer.key" class="flex flex-wrap items-center gap-0" :data-cy="`answer-${index}`">
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