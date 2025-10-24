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
import Sortable from 'sortablejs';
import {useField} from "vee-validate";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

const props = defineProps({
  value: Array,
  q: Object,
  questionNumber: Number,
  quizComplete: Boolean,
  name: {
    type: String,
    required: true
  },
})

const emit = defineEmits(['updateAnswerOrder'])
const announcer = useSkillsAnnouncer();

const answerBank = ref([])
const matchedAnswers = ref([])
const matchedAnswersNotReactive = props.q.answerOptions.map((opt) => ({id: opt.id, answerOption: opt.answerOption, matchedAnswer: opt.currentAnswer || ''}))
const answerBankNotReactive =  props.q.matchingTerms
    .filter(term => !matchedAnswersNotReactive.some(matched => matched.matchedAnswer === term))
    .map((term, index) => ({id: index, answerOption: term}))

const matchedListId = `matchedList-q${props.q.id}`
const availableAnswersListId = `availableAnswersList-q${props.q.id}`
const answerOptionsList = `answerOptionsList-q${props.q.id}`

onMounted(() => {
  matchedAnswers.value = matchedAnswersNotReactive.map((opt) => ({...opt}))
  answerBank.value = answerBankNotReactive.map((opt) => ({...opt}))

  const commonOptions = {
    group: 'shared',
    put: false,
    swap: true,
    swapThreshold: 1, // Requires 100% overlap to swap
    invertSwap: true, // Forces a "drop-on-top" feel
    animation: 150,
    clone: false,
    onAdd: (evt) => {
      itemAddedToList(evt)
    },
    onUpdate: (evt) => {
      itemAddedToList(evt)
    }
  };

  const matchedList = document.getElementById(matchedListId);
  const availableAnswersList = document.getElementById(availableAnswersListId);

  new Sortable(matchedList, commonOptions);
  new Sortable(availableAnswersList, {...commonOptions, group: {
      name: 'shared',
      put: false // Do not allow items to be put into this list
    }});
})

const recordMatchedAnswer = (index, newValue) => {
  matchedAnswers.value[index].matchedAnswer = newValue
  value.value[index].currentAnswer = newValue;
  const answerPair = {term: value.value[index].answerOption, value: value.value[index].currentAnswer}
  emit('updateAnswerOrder', [answerPair]);
  if (answerPair.value) {
    announcer.polite(`${answerPair.value} has been matched to ${answerPair.term}`)
  } else {
    announcer.polite(`${answerPair.value} has been matched to ${answerPair.term}`)
  }
}

const itemAddedToList = (evt) => {
  const isMatchedListTo = evt.to.id === matchedListId
  const isMatchedListFrom = evt.from.id === matchedListId

  const fromVal = isMatchedListFrom ? matchedAnswers.value[evt.oldIndex].matchedAnswer : answerBank.value[evt.oldIndex].answerOption
  const existingToVal = isMatchedListTo ? matchedAnswers.value[evt.newIndex].matchedAnswer : answerBank.value[evt.newIndex].answerOption

  if (isMatchedListTo) {
    recordMatchedAnswer(evt.newIndex, fromVal)
  } else {
    answerBank.value[evt.newIndex].answerOption = fromVal
  }
  if (isMatchedListFrom) {
    recordMatchedAnswer(evt.oldIndex, existingToVal)
  } else {
    answerBank.value[evt.oldIndex].answerOption = existingToVal
  }

  // Check if ALL answers are filled (none are empty)
  if (matchedAnswers.value.every(a => a.matchedAnswer !== '')) {
    validate()
  }

  return true
}



const { value, errorMessage, validate } = useField(() => props.name, undefined, {syncVModel: true});

</script>

<template>
  <div>
    <div class="flex gap-4 wrap">
      <Fieldset legend="Matched">
        <div class="flex gap-2">
          <ul :id="answerOptionsList" class="min-w-[3rem]">
            <li v-for="answer in matchedAnswersNotReactive"
                :key="answer.id"
                class="min-h-[3rem] px-3 py-1 flex items-center mb-2 bg-white"
            >{{ answer.answerOption }}:
            </li>
          </ul>
          <ul :id="matchedListId" class="min-w-[10rem]">
            <li v-for="answer in matchedAnswersNotReactive"
                :key="answer.id"
                :class="{'bg-neutral-100 border-dotted': answer.matchedAnswer === '', 'border-blue-100': answer.matchedAnswer !== ''}"
                class="min-h-[3rem] border-2 rounded px-3 py-1 flex items-center mb-2 "
            >{{ answer.matchedAnswer }}
            </li>
          </ul>
        </div>
      </Fieldset>
      <Fieldset legend="Available">
        <div v-if="!answerBankNotReactive || answerBankNotReactive.length === 0"
             class="max-w-[15rem] text-center">
          <div class="text-surface-700">
            <i class="fas fa-check text-green-700" aria-hidden="true"></i> All Answers Placed
          </div>
          <div class="text-sm mt-2">You can still rearrange your answers in the matched list</div>
        </div>
        <ul :id="availableAnswersListId" class="min-w-[10rem]">
          <li v-for="available in answerBankNotReactive"
              :key="available.id"
              class="min-h-[3rem] border-2 border-blue-100 rounded px-3 py-1 flex items-center mb-2"
          >{{ available.answerOption }}
          </li>
        </ul>
      </Fieldset>
    </div>


    <Message v-if="errorMessage"
             severity="error"
             variant="simple"
             size="small"
             :closable="false"
             :data-cy="`${name}Error`"
             :id="`${name}Error`">{{ errorMessage || '' }}</Message>
  </div>
</template>

<style scoped>
.matching-question > .answer {
  margin-bottom: 2px;
  padding: 6px;
  border: 1px solid #c0c0c0;
  border-radius: 4px;
}

.answer-section {
  min-height: 100%;
}

.sortable-chosen {
  background: #e1f5fe;
}
.sortable-swap-highlight {
  background-color: #9AB6F1;
  border: 2px dashed #5c8ef2;
  border-radius: 4px;
}
</style>