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
import { ref } from 'vue';
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";
import QuizService from "@/components/quiz/QuizService.js";
import {useRoute} from "vue-router";
import { useForm } from 'vee-validate'
import {object, string} from "yup";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

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
const emit = defineEmits(['on-graded'])
const route = useRoute()
const appConfig = useAppConfig()
const announcer = useSkillsAnnouncer()

const validationSchema = object({
  'feedbackTxt': string()
      .max(appConfig.maxGraderFeedbackMessageLength)
      .customDescriptionValidator('Feedback')
      .label('Feedback'),
})
const { meta, handleSubmit, isSubmitting } = useForm({
  validationSchema: validationSchema,
  initialValues: { feedbackTxt : ''}
})
const submitCorrect = handleSubmit(formValues => {
  return grade(true, formValues.feedbackTxt)
})
const submitWrong = handleSubmit(formValues => {
  return grade(false, formValues.feedbackTxt)
})

const isGraded = ref(false)
const questionAlreadyGraded = ref(false)
const grade = (isCorrect, feedback) => {
  const gradingInfo = {
    isCorrect,
    feedback
  }
  return QuizService.gradeQuizAnswerAttempt(route.params.quizId, props.userId, props.quizAttemptId, props.question.answers[0].id, gradingInfo)
      .then((result) => {
        emit('on-graded', result);
        if (isCorrect) {
          announcer.polite(`Question number ${props.question.questionNumber} has been graded as correct`)
        } else {
          announcer.polite(`Question number ${props.question.questionNumber} has been graded as incorrect`)
        }
        return result
      }).catch((err) => {
        const quizAlreadyCompleted = err?.response?.data?.errorCode === 'QuizAlreadyCompleted'
        if (quizAlreadyCompleted) {
          questionAlreadyGraded.value = true
        } else {
          throw err;
        }
      }).finally(() => {
        isGraded.value = true
      })
}
</script>

<template>
  <div class="mb-4" :data-cy="`question_${question.questionNumber}`">
    <div v-if="!questionAlreadyGraded">
      <div class="">
        <div class="font-bold text-lg">Question #{{ question.questionNumber }}:  <Tag v-if="isGraded" data-cy="gradedTag"><i class="fas fa-check mr-1" aria-hidden="true" /> GRADED</Tag></div>
        <div v-if="!isGraded" class="">
          <MarkdownText
              :text="question.question"
              :instance-id="`${quizAttemptId}_${question.id}_question`"
              data-cy="questionDisplayText"/>
        </div>
      </div>
      <div v-if="!isGraded">
      <div class="">
        <div class="font-semibold">User's Answer:</div>
        <div class="mt-2 border rounded-border border-dotted border-surface px-6 py-2">
        <MarkdownText
            :data-cy="`answer_${question.questionNumber}displayText`"
            :text="question.answers[0].answer"
            :instance-id="`${quizAttemptId}_${question.id}_answer`"
            data-cy="answerText"/>
        </div>
      </div>
      <InlineMessage
          v-if="appConfig.enableOpenAIIntegration && question.aiGradingConfigured"
          class="mt-2"
          data-cy="aiManualGradingLongMsg"
          severity="warn">AI grading is enabled for this question. Manual grading is available but not recommended.</InlineMessage>

      <markdown-editor class="form-text mt-4"
                       :id="`qFeedback-${quizAttemptId}_${question.questionNumber}`"
                       :allow-community-elevation="true"
                       markdownHeight="120px"
                       label="Your Feedback (optional)"
                       name="feedbackTxt"
                       :disabled="isSubmitting"
                       :allow-attachments="false"
                       :allow-insert-images="false"
                       :aria-label="`optionally provide feedback for answer of question # ${question.questionNumber}`"
                       :disable-ai-prompt="true"
                       :resizable="false" />
      <div class="flex gap-2 mt-4">
      <SkillsButton
          size="small"
          label="Wrong"
          icon="fas fa-times"
          severity="danger"
          @click="submitWrong"
          :loading="isSubmitting"
          :disabled="!meta.valid || isSubmitting"
          :aria-label="`Mark question number ${question.questionNumber} as wrong`"
          data-cy="markWrongBtn"
      />
      <SkillsButton
          label="Correct"
          @click="submitCorrect"
          icon="fas fa-check"
          size="small"
          :disabled="!meta.valid || isSubmitting"
          :loading="isSubmitting"
          :aria-label="`Mark question number ${question.questionNumber} as correct`"
          data-cy="markCorrectBtn"
      />
    </div>
    </div>
    </div>
    <Message v-if="questionAlreadyGraded" severity="warn" :closable="false" data-cy="singleQuestionGradedSinceLoadedMsg">This question has been graded since the form was loaded. Please refresh the page.</Message>
  </div>
</template>

<style scoped>

</style>