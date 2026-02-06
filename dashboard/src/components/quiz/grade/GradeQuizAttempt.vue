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
import {computed, onMounted, ref} from "vue";
import QuizService from "@/components/quiz/QuizService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import {useRoute} from "vue-router";
import GradeSingleQuestion from "@/components/quiz/grade/GradeSingleQuestion.vue";

const props = defineProps({
  quizAttemptId: {
    type: Number,
    required: true,
  },
  userId: {
    type: String,
    required: true,
  },
})
const emit = defineEmits(['on-graded'])
const route = useRoute()

const quizType = ref('')
const questionsToGrade = ref([])
const loadingQuestionsToGrade = ref(true)
const loadQuizAttempt = () => {
  return QuizService.getSingleQuizHistoryRun(route.params.quizId, props.quizAttemptId).then((res) => {
    const questionsWithNumber = res.questions.map((q, index) => {
      return {...q, questionNumber: index + 1}
    })
    questionsToGrade.value = questionsWithNumber.filter((q) => q.needsGrading)
    quizType.value = res.quizType
  }).finally(() => {
    loadingQuestionsToGrade.value = false
  })
}

onMounted(() => {
  loadQuizAttempt()
})

const onGraded = (gradedInfo) => {
  emit('on-graded', gradedInfo)
}
const hasQuestionsToGrade = computed(() => questionsToGrade.value?.length > 0)
</script>

<template>
  <div>
    <skills-spinner v-if="loadingQuestionsToGrade" :is-loading="loadingQuestionsToGrade"/>
    <div v-else class="mb-16">
      <Message v-if="!hasQuestionsToGrade"
               severity="warn"
               data-cy="questionGradedSinceLoadedMsg"
               :closable="false">All questions have been graded since the table was loaded. Please refresh the page.</Message>
      <div v-if="hasQuestionsToGrade" v-for="(q, index) in questionsToGrade" :key="q.id">
        <grade-single-question
            :question="q"
            :user-id="userId"
            :quiz-attempt-id="quizAttemptId"
            @on-graded="onGraded"
        />
        <hr v-if="index < questionsToGrade.length - 1" class="mb-12"/>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>