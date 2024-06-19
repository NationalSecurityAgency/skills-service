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
import { onMounted, computed, ref, inject } from 'vue'
import QuizService from '@/components/quiz/QuizService.js';
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';

const props = defineProps({
  initiallySelectedQuizId: {
    type: String,
    default: null,
  },
})
const emit = defineEmits(['changed'])
const setFieldValue = inject('setFieldValue')

const isLoading = ref(true)
const selectedInternal = ref(null)
const availableQuizzes = ref([])

const noQuizzes = computed(() => {
  return availableQuizzes.value && availableQuizzes.value.length === 0;
})
onMounted(() => {
  loadData()
})
const loadData = () => {
  isLoading.value = true
  QuizService.getQuizDefs()
      .then((res) => {
        availableQuizzes.value = res;
        if (props.initiallySelectedQuizId) {
          const found = availableQuizzes.value.find((q) => q.quizId === props.initiallySelectedQuizId);
          if (found) {
            selectedInternal.value = found;
            setFieldValue('associatedQuiz', found);
          }
        }
      }).finally(() => {
        isLoading.value = false
      });
}
const quizSelected = (quiz) => {
  emit('changed', quiz ? quiz.quizId : null);
}
</script>

<template>
  <div class="w-full">
    <SkillsDropDown
        name="associatedQuiz"
        data-cy="quizSelector"
        v-model="selectedInternal"
        showClear
        filter
        optionLabel="name"
        @update:modelValue="quizSelected"
        :emptyMessage="noQuizzes ? 'You currently do not administer any quizzes or surveys.' : 'No results. Please refine your search string.'"
        :isRequired="true"
        :options="availableQuizzes">
      <template #value="slotProps">
        <div v-if="slotProps.value" class="p-1" :data-cy="`quizSelected-${slotProps.value.quizId}`">
          <span class="text-secondary">{{ slotProps.value.type }}:</span><span class="ml-1">{{ slotProps.value.name }}</span>
        </div>
        <span v-else>
            Search available quizzes and surveys...
        </span>
      </template>
      <template #option="slotProps">
        <div :data-cy="`availableQuizSelection-${slotProps.option.quizId}`">
          <span class="text-secondary">{{ slotProps.option.type }}:</span><span class="h6 ml-2">{{ slotProps.option.name }}</span>
        </div>
      </template>
    </SkillsDropDown>
  </div>
</template>

<style scoped>

</style>