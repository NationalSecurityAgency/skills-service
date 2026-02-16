/*
Copyright 2026 SkillTree

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

import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import {object, string} from "yup";
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";
import SkillsInputSwitch from "@/components/utils/inputForm/SkillsInputSwitch.vue";
import QuizService from "@/components/quiz/QuizService.js";
import {useRoute} from "vue-router";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

const model = defineModel()
const props = defineProps({
  userId: String,
  question: Object,
})
const emit = defineEmits(['grade-overridden'])
const route = useRoute()
const appConfig = useAppConfig()
const announcer = useSkillsAnnouncer()

const initialData = {
  feedbackTxt: '',
  notifyUser: false
}

const schema = object({
  'feedbackTxt': string()
      .max(appConfig.maxGraderFeedbackMessageLength)
      .customDescriptionValidator('Feedback')
      .label('Feedback'),
})

const overrideGrade = (attributes) => {
  const quizId = route.params.quizId
  const runId = route.params.runId
  const answerDefId = props.question.answers[0].id
  const gradingInfo = {
    isCorrect: !props.question.isCorrect,
    feedback: attributes.feedbackTxt || '',
    changeGrade: true,
    notifyUser: attributes.notifyUser
  }
  return QuizService.gradeQuizAnswerAttempt(quizId, props.question.userId, runId, answerDefId, gradingInfo)
}

const afterSave = (res) => {
  emit('grade-overridden', res)
  const isCorrect = !props.question.isCorrect
  const newCorrectness = isCorrect ? 'correct' : 'wrong'
  announcer.polite(`Question ${props.question.questionNum} grade changed to ${newCorrectness}`)
}
</script>

<template>
  <SkillsInputFormDialog
      :id="`overrideTextInputGrade-q${question.id}`"
      header="Override Grade"
      v-model="model"
      :save-data-function="overrideGrade"
      @saved="afterSave"
      :validation-schema="schema"
      :initial-values="initialData"
      :enable-return-focus="true"
      :enable-input-form-resiliency="false"
      :should-confirm-cancel="false"
      data-cy="addSkillTagDialog"
      save-button-label="Override Grade"
      save-button-icon="fa-solid fa-hammer">
    <Message  severity="warn" :closable="false">
      <div v-if="question.isCorrect" data-cy="overrideGradeWarningToWrong">This operation will change the answer to <Tag severity="danger">WRONG</tag> which is currently graded as <Tag>CORRECT</Tag>. Please proceed with caution.</div>
      <div v-else data-cy="overrideGradeWarningToCorrect">This operation will change the answer to <Tag>CORRECT</tag> which is currently graded as <Tag severity="danger">WRONG</Tag>. Please proceed with caution.</div>
    </Message>
    <MarkdownEditor data-cy="feedback"
                    :id="`qFeedback-${question.id}`"
                    :allow-community-elevation="true"
                    markdownHeight="150px"
                    label="Your Feedback (optional)"
                    :disable-ai-prompt="true"
                    :allow-attachments="false"
                    :allow-insert-images="false"
                    :aria-label="`optionally provide feedback for answer of question # ${question.questionNumber}`"
                    name="feedbackTxt"/>
    <div class="flex items-center gap-2 mt-2">
      <SkillsInputSwitch name="notifyUser" /><label for="notifyUser">Notify Quiz Taker</label>
    </div>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>