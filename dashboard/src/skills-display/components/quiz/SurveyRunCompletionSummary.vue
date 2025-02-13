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

import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js';
import QuizType from "@/skills-display/components/quiz/QuizType.js";

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
          <Message severity="success" icon="fas fa-handshake">Thank you for taking the time to complete the survey!</Message>
        </slot>
      </div>

      <div class="mb-1 mt-6 text-3xl">
        <span class="font-bold text-success mb-2 skills-page-title-text-color">{{ quizInfo.name }}</span>
      </div>

      <div class="flex gap-6 flex-col md:flex-row pt-2 pb-1 text-center">
        <div class="flex">
          <Card class="text-center skills-card-theme-border flex-1" :pt="{ body: { class: 'p-0' }, content: { class: 'py-2 px-3' } }">
            <template #content>
              <i class="fas fa-question-circle text-primary skills-theme-quiz-correct-answer" style="font-size: 1.3rem;" aria-hidden="true"></i>
              <span class="text-muted-color italic ml-1">Questions:</span>
              <span class="uppercase ml-1 font-bold">{{ quizInfo.questions.length }}</span>
            </template>
          </Card>
        </div>
        <div class="flex">
          <Card class="text-center skills-card-theme-border flex-1" :pt="{ body: { class: 'p-0' }, content: { class: 'py-2 px-3' } }">
            <template #content>
              <i class="fas fa-business-time text-primary skills-theme-quiz-correct-answer" style="font-size: 1.3rem;" aria-hidden="true"></i>
              <span class="text-muted-color italic ml-1">Completed In:</span>
              <span class="uppercase ml-1 font-bold">{{ timeUtils.formatDurationDiff(quizResult.gradedRes.started, quizResult.gradedRes.completed) }}</span>
            </template>
          </Card>
        </div>
        <div class="flex-1"></div>
      </div>

      <div class="mt-8">
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