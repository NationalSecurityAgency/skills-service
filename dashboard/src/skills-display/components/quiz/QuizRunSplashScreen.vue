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
import { computed } from 'vue'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue';
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";
import QuizCompletedMessage from "@/skills-display/components/quiz/QuizCompletedMessage.vue";

const props = defineProps({
  quizInfo: Object,
  multipleTakes: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['cancelQuizAttempt', 'start'])
const timeUtils = useTimeUtils()

const quizTimeLimit = computed(() => {
  return props.quizInfo.quizTimeLimit * 1000;
})
const numQuestions = computed(() => {
  return props.quizInfo.quizLength;
})
const isSurveyType = computed(() => {
  return props.quizInfo.quizType === 'Survey';
})
const maxAttemptsDisplay = computed(() => {
  return props.quizInfo.maxAttemptsAllowed > 0 ? props.quizInfo.maxAttemptsAllowed : 'Unlimited';
})
const allAttemptsExhausted = computed(() => {
  return props.quizInfo.maxAttemptsAllowed > 0 && props.quizInfo.maxAttemptsAllowed <= props.quizInfo.userNumPreviousQuizAttempts;
})
const minNumQuestionsToPass = computed(() => {
  return props.quizInfo.minNumQuestionsToPass > 0 ? props.quizInfo.minNumQuestionsToPass : numQuestions.value;
})

const canStartQuiz = computed(() => {
  return (!props.quizInfo.userQuizPassed || props.multipleTakes) && !allAttemptsExhausted.value && numQuestions.value > 0 && props.quizInfo.canStartQuiz;
})
const needsGrading = computed(() => props.quizInfo.needsGrading)
const questionsToTake = computed(() => props.quizInfo.numIncorrectQuestions)
const numberCorrect = computed(() => {
  return numQuestions.value - questionsToTake.value;
})
const remainingQuestions = computed(() => {
  if(props.quizInfo.multipleTakes && props.quizInfo.userQuizPassed) {
    return minNumQuestionsToPass.value - questionsToTake.value
  } else {
    return minNumQuestionsToPass.value - numberCorrect.value
  }
});
const onlyIncorrect = computed(() => {
  return props.quizInfo.onlyIncorrectQuestions && ((!props.quizInfo.userQuizPassed && props.quizInfo.userLastQuizAttemptDate) || props.quizInfo.multipleTakes)
})

const cancel = () => {
  emit('cancelQuizAttempt');
}
const start = () => {
  emit('start');
}
</script>

<template>
  <Card data-cy="quizSplashScreen" :pt="{ content: { class: 'p-0!' } }">
    <template #content>
      <div class="text-xl">
        <Message v-if="quizInfo.userQuizPassed && !multipleTakes " :closable="false" severity="success">
          <template #messageicon>
            <i class="fas fa-gift" aria-hidden="true"></i>
          </template>
          <div class="flex items-center">
            <span class="mx-2">
              Good News! You already <span v-if="!isSurveyType">passed this quiz</span><span v-else>completed this survey</span> <span class="font-bold">{{ timeUtils.timeFromNow(quizInfo.userLastQuizAttemptDate) }}</span>!
            </span>
            <SkillsButton @click="cancel"
                          icon="fas fas fa-times-circle"
                          outlined
                          size="small"
                          data-cy="closeQuizAttemptInAlert"
                          class="uppercase"
                          :label="`Close ${quizInfo.quizType}`">
            </SkillsButton>
          </div>
        </Message>
        <slot name="aboveTitle" />
        <div class="mb-1 mt-1 text-3xl">
          <h2 class="font-bold text-success skills-page-title-text-color">{{ quizInfo.name }}</h2>
        </div>

        <Card v-if="!isSurveyType && canStartQuiz" class="my-2 text-xl skills-card-theme-border" :pt="{ content: { class: 'p-0!' } }" data-cy="quizPassInfo">
          <template #content>
            <i class="fas fa-check-circle text-primary" aria-hidden="true"></i>
            Must get <Tag severity="success">{{ minNumQuestionsToPass }}</Tag> / <Tag severity="secondary">{{ numQuestions }}</Tag> questions <span class="text-muted-color italic">({{ quizInfo.percentToPass }}%)</span> to <span class="text-primary uppercase">pass</span>. Good Luck!
          </template>
        </Card>

        <Message v-if="onlyIncorrect" data-cy="onlyIncorrectMessage">
          You only need to retake the questions you did not answer correctly on your last attempt.
          <span v-if="!quizInfo.userQuizPassed">You've already answered <Tag severity="success">{{ numberCorrect }}</Tag> correctly, so you need to answer <Tag severity="warn">{{ remainingQuestions }}</Tag> question(s) to pass.</span>
          <span v-else>You need to answer <Tag severity="warn">{{ remainingQuestions }}</Tag> question(s) to pass.</span>
        </Message>

        <div class="flex flex-col flex-wrap md:flex-row gap-6 pt-2">
            <Card class="skills-card-theme-border flex-1 min-w-80" :pt="{ body: { class: 'p-0!' }, content: { class: 'py-2!' } }" data-cy="quizInfoCard">
              <template #content>
                <div class="px-4">
                  <i class="fas fa-question-circle text-primary" style="font-size: 1.3rem;" aria-hidden="true"></i>
                  <span class="text-muted-color font-italic ml-1">Questions:</span>
                  <span class="uppercase ml-1 font-bold" data-cy="numQuestions">{{ onlyIncorrect ? questionsToTake : numQuestions }}</span>
                </div>
              </template>
            </Card>
            <Card v-if="!isSurveyType"  class="skills-card-theme-border flex-1 min-w-80" :pt="{ body: { class: 'p-0!' }, content: { class: 'py-2' } }" data-cy="quizTimeLimitCard">
              <template #content>
                <div class="px-4">
                  <i class="fas fa-business-time text-primary" style="font-size: 1.3rem;"></i>
                  <span class="text-muted-color italic ml-1">Time Limit:</span>
                  <span v-if="quizInfo.quizTimeLimit > 0" class="uppercase ml-1 font-bold">{{ timeUtils.formatDuration(quizTimeLimit) }}</span>
                  <span v-else class="uppercase ml-1 font-bold">NONE</span>
                </div>
              </template>
            </Card>
            <Card v-if="!isSurveyType"  class="skills-card-theme-border flex-1 min-w-80" :pt="{ body: { class: 'p-0!' }, content: { class: 'py-2' } }" data-cy="quizInfoCard">
              <template #content>
                <div class="px-4 ">
                  <i class="fas fa-redo-alt text-primary" style="font-size: 1.3rem;" aria-hidden="true"></i>
                  <span class="text-muted-color italic ml-1">Attempts:</span>
                  <span class="uppercase ml-1 font-bold text-sm" data-cy="numAttempts"><Tag severity="secondary">{{quizInfo.userNumPreviousQuizAttempts}}</Tag> / <Tag severity="secondary">{{ maxAttemptsDisplay }}</Tag></span>
                </div>
              </template>
            </Card>
        </div>

        <Message v-if="(!quizInfo.userQuizPassed || quizInfo.multipleTakes) && allAttemptsExhausted" severity="error" :closable="false" data-cy="noMoreAttemptsAlert">
          No more attempts available. This quiz allows <Tag severity="secondary">{{quizInfo.maxAttemptsAllowed}}</Tag> maximum attempt<span v-if="quizInfo.maxAttemptsAllowed > 1">s</span>.
        </Message>
        <Message v-if="numQuestions === 0" severity="error" :closable="false" data-cy="quizHasNoQuestions">
          This {{ quizInfo.quizType }} has no questions declared and unfortunately cannot be completed.
        </Message>
        <Message v-if="!quizInfo.canStartQuiz && quizInfo.errorMessage" severity="error" :closable="false" data-cy="cantStartQuiz">
          {{ quizInfo.errorMessage }}
        </Message>
        <quiz-completed-message v-if="needsGrading" :attempt-timestamp="quizInfo.needsGradingAttemptDate" />

        <p v-if="quizInfo.description && !allAttemptsExhausted" class="mt-8" data-cy="quizDescription">
          <MarkdownText :text="quizInfo.description" />
        </p>

        <div class="mt-12">
          <SkillsButton v-if="canStartQuiz"
                        @click="cancel"
                        icon="fas fas fa-times-circle"
                        outlined
                        severity="danger"
                        :aria-label="`Cancel ${quizInfo.quizType} run`"
                        size="small"
                        data-cy="cancelQuizAttempt"
                        class="uppercase mr-2 skills-theme-btn"
                        label=" Cancel">
          </SkillsButton>
          <SkillsButton v-if="canStartQuiz"
                        @click="start"
                        icon="fas fa-play-circle"
                        outlined
                        severity="success"
                        :aria-label="`Start ${quizInfo.quizType} run`"
                        size="small"
                        data-cy="startQuizAttempt"
                        class="uppercase skills-theme-btn"
                        label=" Start">
          </SkillsButton>
          <SkillsButton v-if="!canStartQuiz"
                        @click="cancel"
                        icon="fas fas fa-times-circle"
                        outlined
                        :aria-label="`Close ${quizInfo.quizType} run`"
                        size="small"
                        data-cy="closeQuizAttempt"
                        class="uppercase mr-2 skills-theme-btn"
                        label=" Close">
          </SkillsButton>
        </div>

      </div>
    </template>
  </Card>

</template>

<style scoped>

</style>