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
import {onMounted, ref, computed, onBeforeMount, nextTick, onUnmounted, watch} from 'vue';
import Sortable from 'sortablejs';
import {useField} from "vee-validate";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";
import {useElementHelper} from "@/components/utils/inputForm/UseElementHelper.js";

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
const elementHelper = useElementHelper(250, 40)

const matchedAnswersOrig = ref([])
const matchedAnswersCurrent = ref([])

const answerBankOrig =  ref([])
const answerBankCurrent = ref([])

const matchedListId = `matchedList-q${props.q.id}`
const availableAnswersListId = `availableAnswersList-q${props.q.id}`
const answerOptionsList = `answerOptionsList-q${props.q.id}`
let sortableInstances = []
onBeforeMount(() => {
  matchedAnswersOrig.value = props.q.answerOptions
      .map((opt) => ({id: opt.id, answerOption: opt.answerOption, matchedAnswer: opt.currentAnswer || ''}))
  answerBankOrig.value =  props.q.matchingTerms
      .filter(term => !matchedAnswersOrig.value.some(matched => matched.matchedAnswer === term))
      .map((term, index) => ({id: index, answerOption: term}))
})
onMounted(() => {
  initSortableJsInstances()
})
watch(() => props.quizComplete, (newVal, oldVal) => {
  if (!oldVal && newVal) {
    destroySortableJsInstances()
  }
})

onUnmounted(() => {
  destroySortableJsInstances()
})

const initSortableJsInstances = () => {
  matchedAnswersCurrent.value = matchedAnswersOrig.value.map((opt) => ({...opt, initialId: opt.id}))
  answerBankCurrent.value = answerBankOrig.value.map((opt) => ({...opt, initialId: opt.id}))

  if (props.quizComplete) {
    return
  }

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

  const matchedSortable = new Sortable(matchedList, commonOptions);
  const availableSortable = new Sortable(availableAnswersList, {...commonOptions, group: {
      name: 'shared',
      put: false // Do not allow items to be put into this list
    }});
  sortableInstances = [matchedSortable, availableSortable]
}

const destroySortableJsInstances = () => {
  if (sortableInstances.value) {
    sortableInstances.forEach((sortable) => sortable.destroy())
    sortableInstances.value = []
  }
}
const recordMatchedAnswer = (index, newValue, newId) => {
  matchedAnswersCurrent.value[index].matchedAnswer = newValue
  matchedAnswersCurrent.value[index].id = newId
  value.value[index].currentAnswer = newValue;
  value.value[index].id = newId
  const answerPair = emitAnswerUpdate(index)
  if (answerPair.value) {
    announcer.polite(`${answerPair.value} has been matched to ${answerPair.term}`)
  } else {
    announcer.polite(`${answerPair.value} has been matched to ${answerPair.term}`)
  }
}

const itemAddedToList = (evt) => {
  const isMatchedListTo = evt.to.id === matchedListId
  const isMatchedListFrom = evt.from.id === matchedListId

  const fromVal = isMatchedListFrom ? matchedAnswersCurrent.value[evt.oldIndex].matchedAnswer : answerBankCurrent.value[evt.oldIndex].answerOption
  const fromId = isMatchedListFrom ? matchedAnswersCurrent.value[evt.oldIndex].id : answerBankCurrent.value[evt.oldIndex].id
  const existingToVal = isMatchedListTo ? matchedAnswersCurrent.value[evt.newIndex].matchedAnswer : answerBankCurrent.value[evt.newIndex].answerOption
  const existingToId = isMatchedListTo ? matchedAnswersCurrent.value[evt.newIndex].id : answerBankCurrent.value[evt.newIndex].id

  if (isMatchedListTo) {
    recordMatchedAnswer(evt.newIndex, fromVal, fromId)
  } else {
    answerBankCurrent.value[evt.newIndex].answerOption = fromVal
    answerBankCurrent.value[evt.newIndex].id = fromId
  }
  if (isMatchedListFrom) {
    recordMatchedAnswer(evt.oldIndex, existingToVal, existingToId)
  } else {
    answerBankCurrent.value[evt.oldIndex].answerOption = existingToVal
    answerBankCurrent.value[evt.oldIndex].id = existingToId
  }

  // Check if ALL answers are filled (none are empty)
  if (matchedAnswersCurrent.value.every(a => a.matchedAnswer !== '')) {
    validate()
  }

  return true
}

const prepKeyboardMove = () => {
  destroySortableJsInstances()

  // sync orig to current
  answerBankOrig.value = answerBankCurrent.value.map((i) => ({...i}))
  matchedAnswersOrig.value = matchedAnswersCurrent.value.map((i) => ({...i}))
}

const insertElementIntoNextSlot = (availableId) => {
  const availableItemIndex = answerBankOrig.value.findIndex((item) => item.id === availableId)
  if (availableItemIndex >= 0) {
    prepKeyboardMove()

    const availableItem = answerBankOrig.value[availableItemIndex]
    answerBankOrig.value = answerBankOrig.value.filter((item) => item.id !== availableId)
    // find next available matched item after the current index
    let nextAvailableIndex = matchedAnswersOrig.value.findIndex((item, index) => index >= availableItemIndex && item.matchedAnswer === '')
    if (nextAvailableIndex === -1) {
      nextAvailableIndex = matchedAnswersOrig.value.findIndex((item) => item.matchedAnswer === '')
    }
    matchedAnswersOrig.value[nextAvailableIndex].matchedAnswer = availableItem.answerOption
    matchedAnswersOrig.value[nextAvailableIndex].id = availableItem.id
    initSortableJsInstances()
    recordMatchedAnswer(nextAvailableIndex, availableItem.answerOption, availableItem.id)

    focusOnItem(availableItem.id)
  }
}
const moveItem = (itemId, adjustByIndex) => {
  const index1 = matchedAnswersCurrent.value.findIndex((item) => item.id === itemId)
  const index2 = index1 + adjustByIndex
  if (index2 >= 0 && index2 < matchedAnswersCurrent.value.length) {
    prepKeyboardMove()
    const item1 = {...matchedAnswersOrig.value[index1]}
    const item2 = {...matchedAnswersOrig.value[index2]}
    matchedAnswersOrig.value[index1] = item2
    matchedAnswersOrig.value[index2] = item1
    initSortableJsInstances()
    recordMatchedAnswer(index1, item2.matchedAnswer, item2.id)
    recordMatchedAnswer(index2, item1.matchedAnswer, item1.id)
    focusOnItem(item1.id)
  }
}

const focusOnItem = (itemId) => {
  nextTick(() => {
    elementHelper.getElementById(`matchValBtn-${props.q.id}-${itemId}`).then((focusOn) => {
      if (focusOn) {
        focusOn.focus()
      }
    })
  })
}

const emitAnswerUpdate = (index) => {
  const answerPair = {term: value.value[index].answerOption, value: value.value[index].currentAnswer}
  emit('updateAnswerOrder', [answerPair]);
  return answerPair
}
const { value, errorMessage, validate } = useField(() => props.name, undefined, {syncVModel: true});
const isAnswerCorrect = (id) => {
  const answerOption = props.q.answerOptions.find((a) => a.id === id)
  return answerOption.isCorrect
}
</script>

<template>
  <div>
    <div class="flex flex-col md:flex-row gap-4 wrap">
      <Fieldset :legend="`Matched answers for questions number ${questionNumber}`" :id="`matchedAnswersFieldset-${questionNumber}`">
        <template #legend>
          <div :id="`matchedAnswersFieldset-${questionNumber}_header`">Matched<span class="sr-only"> for question #{{ questionNumber }}</span></div>
        </template><div class="flex gap-2">
          <ul :id="answerOptionsList">
            <li v-for="answer in matchedAnswersOrig"
                :key="answer.id"
                class="min-h-[3rem] px-3 py-1 flex items-center mb-2"
            >{{ answer.answerOption }}:
            </li>
          </ul>
          <ul v-if="!quizComplete" :id="matchedListId" class="min-w-[10rem]" data-cy="matchedList">
            <li v-for="(answer, index) in matchedAnswersOrig"
                :key="answer.id"
                :class="{'bg-neutral-200 dark:bg-neutral-800 border-dotted border-1 border-surface-400 dark:border-surface-500 rounded px-3 py-1': answer.matchedAnswer === '', }"
                class="min-h-[3rem] flex items-center mb-2 "
                :data-cy="`matchedNum-${index}`"
            >
             <Button v-if="!quizComplete && answer.matchedAnswer"
                     :id="`matchValBtn-${q.id}-${answer.id}`"
                     outlined
                     @keyup.up="moveItem(answer.id, -1)"
                     @keyup.down="moveItem(answer.id, 1)"
                     class="w-full p-0"><div class="w-full text-left"
                     data-cy="matchedAnswer">{{ answer.matchedAnswer }}</div></Button>
             <div v-else data-cy="matchedAnswer">{{ answer.matchedAnswer }}</div>
            </li>
          </ul>
          <ul v-if="quizComplete" :id="matchedListId" class="min-w-[10rem]" data-cy="matchedList">
            <li v-for="(answer, index) in matchedAnswersCurrent"
                :key="answer.id"
                class="min-h-[3rem] items-center mb-2 border-2 rounded px-3 py-1 flex gap-2"
                :class="{
                  'bg-red-50 border-red-200 dark:bg-red-900 text-red-950 dark:text-red-100': !isAnswerCorrect(answer.initialId),
                  'bg-green-50 border-green-200 dark:bg-green-800 text-green-950 dark:text-green-100': isAnswerCorrect(answer.initialId),
                }"
                :aria-label="`Answer ${answer.matchedAnswer} is ${isAnswerCorrect(answer.initialId) ? 'correct' : 'wrong'}`"
                :data-cy="`matchedNum-${index}`"
            >
              <i v-if="!isAnswerCorrect(answer.initialId)" class="fas fa-ban text-red-500" aria-hidden="true" data-cy="matchIsWrong"></i>
              <i v-else class="fas fa-check text-green-500" aria-hidden="true" data-cy="matchIsCorrect"></i>
              {{ answer.matchedAnswer }}
            </li>
          </ul>
        </div>
      </Fieldset>
      <Fieldset v-if="!quizComplete" :id="`availableAnswersFieldset-${questionNumber}`">
        <template #legend>
          <div :id="`availableAnswersFieldset-${questionNumber}_header`">Available<span class="sr-only"> for question #{{ questionNumber }}</span></div>
        </template>
        <div v-if="!answerBankOrig || answerBankOrig.length === 0"
             class="max-w-[15rem] text-center" data-cy="allAnswersPlaced">
          <div>
            <i class="fas fa-check text-green-700" aria-hidden="true"></i> All Answers Placed
          </div>
          <div class="text-sm mt-2">You can still rearrange your answers in the matched list</div>
        </div>
        <ul :id="availableAnswersListId" class="min-w-[10rem]" data-cy="availableItems">
          <li v-for="available in answerBankOrig"
              :id="`matchVal-${q.id}-${available.id}`"
              :key="available.id"
              class="min-h-[3rem] flex items-center mb-2 cursor-move"
              :class="{'bg-neutral-100 border-dotted border-2 rounded px-3 py-1': available.answerOption === '', }"
              :data-cy="`bank-${available.id}`"
          ><Button v-if="available.answerOption"
                   :id="`matchValBtn-${q.id}-${available.id}`"
                   outlined
                   @keyup.left="insertElementIntoNextSlot(available.id)"
                   :data-cy="`available-${available.id}`"
                   class="w-full p-0"><div class="w-full text-left">{{ available.answerOption }}</div></Button>
            <div v-else>{{ available.answerOption }}</div>
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