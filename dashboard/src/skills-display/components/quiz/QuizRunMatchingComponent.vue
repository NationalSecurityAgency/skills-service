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
import Sortable from 'sortablejs/modular/sortable.complete.esm.js';
import { onMounted, ref } from 'vue';

const props = defineProps(['q', 'answerOptions'])
const emit = defineEmits(['updateAnswerOrder'])

const answerOrder = ref([])

onMounted(() => {
  let answers = document.getElementById(`answers-${props.q.id}`)
  let answerBank = document.getElementById(`answer-bank-${props.q.id}`)
  answerOrder.value = props.answerOptions;

  Sortable.create(answers, {
    animation: 150,
    swap: true,
    ghostClass: 'skills-sort-order-ghost-class',
    group: {
      name: 'answers',
      put: ['bank'],
    },
    onUpdate() {
      answerOrderChanged();
    },
    onAdd() {
      answerOrderChanged();
    }
  });
  Sortable.create(answerBank, {
    animation: 150,
    ghostClass: 'skills-sort-order-ghost-class',
    group: {
      name: 'bank',
      put: false
    },
  });
})

const answerOrderChanged = () => {
  let answers = document.getElementById(`answers-${props.q.id}`).children
  let termCounter = 0
  let pairs = []
  for(const answer of answers) {
    let answerPair = {term: props.answerOptions[termCounter]?.answerOption, value: answer.innerHTML}
    pairs.push(answerPair)
    termCounter++
  }
  emit('updateAnswerOrder', pairs);
}

</script>

<template>
  <div class="flex gap-4">
    <div class="flex flex-col">
      <div v-for="answer in answerOptions" class="mb-2 p-1" style="border: 1px solid transparent">
        {{answer.answerOption}}
      </div>
    </div>
    <div class="flex flex-col matching-question border-l-1" :id="`answers-${q.id}`" style="min-width: 100px;">

    </div>
    <div class="flex flex-col matching-question border-l-1" :id="`answer-bank-${q.id}`">
      <div v-for="term in q.matchingTerms" class="border-1 ml-2 mb-2 p-1 answer" style="cursor: pointer">
        {{ term }}
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>