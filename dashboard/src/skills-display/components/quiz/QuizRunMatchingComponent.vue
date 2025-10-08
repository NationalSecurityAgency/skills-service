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
import { onMounted, ref, computed } from 'vue';
import draggable from 'vuedraggable';
import {useFocusState} from "@/stores/UseFocusState.js";

const props = defineProps(['q', 'answerOptions', 'questionNumber', 'quizComplete'])
const emit = defineEmits(['updateAnswerOrder'])

const focusState = useFocusState()
const answerBank = ref([])
const answers = ref([])

onMounted(() => {
  answerBank.value = props.q.matchingTerms;
  props.q.matchingTerms.forEach((term) => {
    answers.value.push([ ])
  });
  props.q.answerOptions.forEach((option) => {
    if(option.currentAnswer) {
      answers.value[option.id - 1] = [option.currentAnswer]
      const positionInBank = answerBank.value.indexOf(option.currentAnswer)
      answerBank.value.splice(positionInBank, 1)
    }
  })
})

const answerOrderChanged = (elementToFocus) => {
  let termCounter = 0
  let pairs = []
  for(const answer of answers.value) {
    let answerPair = {term: props.answerOptions[termCounter]?.answerOption, value: answer[0]}
    pairs.push(answerPair)
    termCounter++
  }
  emit('updateAnswerOrder', pairs);

  if(elementToFocus) {
    focusState.waitForElement(elementToFocus).then((el) => {
      const child = el?.children[0]
      focusState.setElementId(child.id)
      focusState.focusOnLastElement()
    })
  }
}

const removeElement = (element) => {
  answerBank.value.push(answers.value[element][0])
  answers.value[element] = []
  const newIndex = answerBank.value.length - 1
  const newId = `bank-${props.questionNumber}-${newIndex}`
  answerOrderChanged()
  focusState.setElementId(newId);
  focusState.focusOnLastElement()
}

const addElement = (element) => {
  let spaceFound = false
  let index = 0

  while(index !== answers.value.length && !spaceFound) {
    if(answers.value[index].length === 0) {
      answers.value[index] = [element]
      spaceFound = true
    }
    if(!spaceFound) {
      index++;
    }
  }

  const elementAt = answerBank.value.indexOf(element)
  answerBank.value.splice(elementAt, 1);
  const newId = `answer-${props.questionNumber}-${index}`
  answerOrderChanged(newId)
}

const moveElementUp = (index) => {
  if(index > 0) {
    const newIndex = index - 1;
    [answers.value[index], answers.value[newIndex]] = [answers.value[newIndex], answers.value[index]]
    const newId = `answer-${props.questionNumber}-${newIndex}`
    answerOrderChanged(newId)
  }
}

const moveElementDown = (index) => {
  if(index + 1 < answers.value.length) {
    const newIndex = index + 1;
    [answers.value[index], answers.value[newIndex]] = [answers.value[newIndex], answers.value[index]]
    const newId = `answer-${props.questionNumber}-${newIndex}`
    answerOrderChanged(newId)
  }
}

const answerSections = computed(() => {
  let sections = []
  for(let i = 0; i < answers.value.length; i++) {
    sections.push(`answers-${props.questionNumber}-${i}`)
  }
  sections.push(`bank-${props.questionNumber}`)
  return sections;
})

const computedName = computed(() => {
  return 'bank-' + props.questionNumber;
})

</script>

<template>
  <div class="flex gap-4">
    <div class="flex flex-col">
      <div v-for="answer in answerOptions" class="mb-2 p-1" style="border: 1px solid transparent">
        {{answer.answerOption}}
      </div>
    </div>
    <div class="flex flex-col border-l-1" :id="`answers-${questionNumber}`" style="min-width: 100px;">
      <div v-for="answer in answers" class="ml-2 mb-2 p-1" style="min-height: 34px;" v-if="quizComplete">
        {{ answer[0] }}
      </div>
      <div v-for="(answer, index) in answers" class="border-1 ml-2 mb-2 p-1" style="min-height: 34px;" v-if="!quizComplete">
        <draggable :list="answers[index]"
                   itemKey=""
                   class="answer-section"
                   :id="`answer-${questionNumber}-${index}`"
                   @add="answerOrderChanged"
                   @end="answerOrderChanged"
                   :group="{ name: computedName, put: answers[index].length === 0 ? answerSections : false, pull: answerSections }">
          <template #item="{element}">
            <div style="cursor: pointer;"
                 tabindex="0"
                 :id="`answers-${questionNumber}-${index}`"
                 :data-cy="`answers-${questionNumber}-${index}`"
                 @keyup.up="moveElementUp(index)"
                 @keyup.down="moveElementDown(index)"
                 @keyup.right="removeElement(index)">
              {{element}}
            </div>
          </template>
        </draggable>
      </div>
    </div>
    <draggable v-if="!quizComplete" :list="answerBank" :id="`bank-${questionNumber}`" itemKey="" :data-cy="`bank-${questionNumber}`" class="flex flex-col matching-question border-l-1" :group="{ name: computedName, put: answerSections, pull: answerSections }">
      <template #item="{element, index}">
        <div class="ml-2 answer" style="cursor: pointer" :id="`bank-${questionNumber}-${index}`" :data-cy="`bank-${questionNumber}-${index}`" tabindex="0" @keyup.left="addElement(element)"> {{element}} </div>
      </template>
    </draggable>

  </div>
</template>

<style scoped>
.matching-question > .answer {
  margin-bottom: 2px;
  padding: 2px;
  border: 1px solid #c0c0c0;
}

.answer-section {
  min-height: 100%;
}
</style>