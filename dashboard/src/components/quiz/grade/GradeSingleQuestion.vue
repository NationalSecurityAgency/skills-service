<script setup>
import { ref } from 'vue';
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";
import QuizService from "@/components/quiz/QuizService.js";
import {useRoute} from "vue-router";

const props = defineProps({
  question: Object,
  userId: {
    type: String,
    required: true
  },
  quizAttemptId: {
    type: Number,
    required: true,
  },
})
const route = useRoute()

const isSubmitting = ref(false)
const isGraded = ref(false)
const grade = (isCorrect) => {
  const gradingInfo = {
    isCorrect,
    feedback: null
  }
  return QuizService.gradeQuizAnswerAttempt(route.params.quizId, props.userId, props.quizAttemptId, props.question.answers[0].id, gradingInfo)
      .then((result) => {
        console.log(result)
      }).finally(() => {
        isGraded.value = true
      })
}
</script>

<template>
  <div class="mb-4">
    <div class="">
      <div class="font-bold text-xl">Question #{{ question.questionNumber }}:  <Tag v-if="isGraded">GRADED</Tag></div>
      <div v-if="!isGraded" class="">
        <MarkdownText
            :text="question.question"
            :instance-id="`${question.id}_question`"
            data-cy="questionDisplayText"/>
      </div>
    </div>
    <pre>quizId: {{ route.params.quizId }}</pre>
    <pre>userId: {{ userId }}</pre>
    <pre>attemptid: {{ quizAttemptId }}</pre>
    <pre>question id: {{ question.answers[0].id }}</pre>
    <div v-if="!isGraded">
      <div class="">
        <div class="font-bold">User's Answer:</div>
        <div class="mt-2 border-1 border-round border-dotted surface-border px-4 py-2">
        <MarkdownText
            :data-cy="`answer_${question.questionNumber}displayText`"
            :text="question.answers[0].answer"
            :instance-id="`${question.id}_answer`"
            data-cy="answerText"/>
        </div>
      </div>
      <markdown-editor class="form-text mt-3"
                       :id="`qFeedback-${question.questionNumber}`"
                       :data-cy="`qFeedback-${question.questionNumber}`"
                       markdownHeight="120px"
                       label="Your Feedback (optional):"
                       name="feedbackTxt"
                       :allow-attachments="false"
                       :allow-insert-images="false"
                       :aria-label="`optionally provide feedback for answer of question # ${question.questionNumber}`"
                       :resizable="true" />
      <div class="flex gap-2 mt-3">
      <SkillsButton
          size="small"
          label="Wrong"
          icon="fas fa-times"
          severity="danger"
          @click="grade(false)"
      />
      <SkillsButton
          label="Correct"
          @click="grade(true)"
          icon="fas fa-check"
          size="small"
      />
    </div>
    </div>
  </div>
</template>

<style scoped>

</style>