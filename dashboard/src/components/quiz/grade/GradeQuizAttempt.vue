<script setup>
import {onMounted, ref} from "vue";
import QuizService from "@/components/quiz/QuizService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import {useRoute} from "vue-router";
import QuizRunQuestionCard from "@/components/quiz/runsHistory/QuizRunQuestionCard.vue";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
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
</script>

<template>
  <div>
    <skills-spinner v-if="loadingQuestionsToGrade" :is-loading="loadingQuestionsToGrade"/>
    <div v-else>
      <div v-for="(q, index) in questionsToGrade" :key="q.id">
        <grade-single-question :question="q" :user-id="userId" :quiz-attempt-id="quizAttemptId"/>
        <hr v-if="index < questionsToGrade.length - 1" class="mb-6"/>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>