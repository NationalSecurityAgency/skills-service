<script setup>
import { computed } from 'vue'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue';
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";

const props = defineProps({
  quizInfo: Object,
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
  return !props.quizInfo.userQuizPassed && !allAttemptsExhausted.value && numQuestions.value > 0;
})
const cancel = () => {
  emit('cancelQuizAttempt');
}
const start = () => {
  emit('start');
}
</script>

<template>
  <Card data-cy="quizSplashScreen" :pt="{ content: { class: 'p-0' } }">
    <template #content>
      <div class="text-xl">
        <Message v-if="quizInfo.userQuizPassed" :closable="false" severity="success">
          <template #messageicon>
            <i class="fas fa-gift" aria-hidden="true"></i>
          </template>
          <div class="flex align-items-center">
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
        <div class="mb-1 mt-4 text-4xl">
          <span class="font-bold text-primary skills-page-title-text-color">{{ quizInfo.name }}</span>
        </div>

        <Card v-if="!isSurveyType && canStartQuiz" class="mb-1 skills-card-theme-border" data-cy="quizPassInfo">
          <template #content>
            <i class="fas fa-check-circle text-primary" aria-hidden="true"></i>
            Must get <Tag severity="success">{{ minNumQuestionsToPass }}</Tag> / <Tag severity="secondary">{{ numQuestions }}</Tag> questions <span class="text-color-secondary font-italic">({{ quizInfo.percentToPass }}%)</span> to <span class="text-primary uppercase">pass</span>. Good Luck!
          </template>
        </Card>

        <div class="flex column-gap-4 pt-2">
            <Card class="skills-card-theme-border flex-1" :pt="{ body: { class: 'p-0' }, content: { class: 'py-2' } }" data-cy="quizInfoCard">
              <template #content>
                <div class="px-3">
                  <i class="fas fa-question-circle text-info" style="font-size: 1.3rem;" aria-hidden="true"></i>
                  <span class="text-color-secondary font-italic ml-1">Questions:</span>
                  <span class="uppercase ml-1 font-bold" data-cy="numQuestions">{{ numQuestions }}</span>
                </div>
              </template>
            </Card>
            <Card v-if="!isSurveyType"  class="skills-card-theme-border flex-1" :pt="{ body: { class: 'p-0' }, content: { class: 'py-2' } }" data-cy="quizTimeLimitCard">
              <template #content>
                <div class="px-3">
                  <i class="fas fa-business-time text-info" style="font-size: 1.3rem;"></i>
                  <span class="text-color-secondary font-italic ml-1">Time Limit:</span>
                  <span v-if="quizInfo.quizTimeLimit > 0" class="uppercase ml-1 font-bold">{{ timeUtils.formatDuration(quizTimeLimit) }}</span>
                  <span v-else class="uppercase ml-1 font-bold">NONE</span>
                </div>
              </template>
            </Card>
            <Card v-if="!isSurveyType"  class="skills-card-theme-border flex-1" :pt="{ body: { class: 'p-0' }, content: { class: 'py-2' } }" data-cy="quizInfoCard">
              <template #content>
                <div class="px-3">
                  <i class="fas fa-redo-alt text-info" style="font-size: 1.3rem;" aria-hidden="true"></i>
                  <span class="text-color-secondary font-italic ml-1">Attempts:</span>
                  <span class="uppercase ml-1 font-bold" data-cy="numAttempts"><Tag severity="secondary">{{quizInfo.userNumPreviousQuizAttempts}}</Tag> / <Tag severity="secondary">{{ maxAttemptsDisplay }}</Tag></span>
                </div>
              </template>
            </Card>
        </div>

        <Message v-if="!quizInfo.userQuizPassed && allAttemptsExhausted" severity="error" data-cy="noMoreAttemptsAlert">
          <template #messageicon>
            <i class="fas fa-exclamation-triangle text-2xl" aria-hidden="true"></i>
          </template>
          <span class="mx-2 text-2xl">No more attempts available. This quiz allows <Tag severity="secondary">{{quizInfo.maxAttemptsAllowed}}</Tag> maximum attempt<span v-if="quizInfo.maxAttemptsAllowed > 1">s</span>.</span>
        </Message>
        <Message v-if="!quizInfo.userQuizPassed && allAttemptsExhausted" severity="error" data-cy="noMoreAttemptsAlert">
          <template #messageicon>
            <i class="fas fa-exclamation-triangle text-2xl" aria-hidden="true"></i>
          </template>
          <span class="mx-2 text-2xl">This {{ quizInfo.quizType }} has no questions declared and unfortunately cannot be completed.</span>
        </Message>

        <p v-if="quizInfo.description && !allAttemptsExhausted" class="mt-3" data-cy="quizDescription">
          <MarkdownText :text="quizInfo.description" />
        </p>

        <div class="mt-6">
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