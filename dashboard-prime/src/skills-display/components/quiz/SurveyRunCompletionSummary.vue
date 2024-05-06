<script setup>

import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js';

const props = defineProps({
  quizResult: Object,
  quizInfo: Object,
})
const emit = defineEmits(['close'])

const timeUtils = useTimeUtils()

const close = () => {
  emit('close')
}
</script>

<template>

  <Card data-cy="surveyCompletion" :pt="{ content: { class: 'p-0' } }">
    <template #content>
      <div class="text-2xl">
        <slot name="completeAboveTitle">
          <i class="fas fa-handshake text-primary" aria-hidden="true"></i> Thank you for taking the time to complete the survey!
        </slot>
      </div>

      <div class="mb-1 mt-4 text-3xl">
        <span class="font-bold text-success mb-2 skills-page-title-text-color">{{ quizInfo.name }}</span>
      </div>

      <div class="flex column-gap-4 pt-2 pb-1 text-center">
        <div class="flex">
          <Card class="text-center skills-card-theme-border flex-1" :pt="{ body: { class: 'p-0' }, content: { class: 'py-2 px-3' } }">
            <template #content>
              <i class="fas fa-question-circle text-primary skills-theme-quiz-correct-answer" style="font-size: 1.3rem;" aria-hidden="true"></i>
              <span class="text-color-secondary font-italic ml-1">Questions:</span>
              <span class="uppercase ml-1 font-bold">{{ quizInfo.questions.length }}</span>
            </template>
          </Card>
        </div>
        <div class="flex">
          <Card class="text-center skills-card-theme-border flex-1" :pt="{ body: { class: 'p-0' }, content: { class: 'py-2 px-3' } }">
            <template #content>
              <i class="fas fa-business-time text-primary skills-theme-quiz-correct-answer" style="font-size: 1.3rem;" aria-hidden="true"></i>
              <span class="text-color-secondary font-italic ml-1">Completed In:</span>
              <span class="uppercase ml-1 font-bold">{{ timeUtils.formatDurationDiff(quizResult.gradedRes.started, quizResult.gradedRes.completed) }}</span>
            </template>
          </Card>
        </div>
        <div class="flex-1"></div>
      </div>

      <div class="mt-5">
        <SkillsButton severity="success" outlined
                      label="Close"
                      icon="fas fas fa-times-circle"
                      @click="close"
                      class="uppercase font-bold skills-theme-btn"
                      data-cy="closeSurveyBtn">
        </SkillsButton>
      </div>
    </template>
  </Card>

</template>

<style scoped>

</style>