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
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js';
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue';
import QuizType from "@/skills-display/components/quiz/QuizType.js";
import QuizCompletedMessage from "@/skills-display/components/quiz/QuizCompletedMessage.vue";

const props = defineProps({
  quizResult: Object,
  quizInfo: Object,
})
const emit = defineEmits(['close', 'run-again'])

const timeUtils = useTimeUtils()
const unlimitedAttempts = computed(() => {
  return props.quizInfo.maxAttemptsAllowed <= 0;
})
const numAttemptsLeft = computed(() => {
  return props.quizInfo.maxAttemptsAllowed - props.quizInfo.userNumPreviousQuizAttempts - 1;
})
const needsGrading = computed(() => props.quizResult.gradedRes.needsGrading )

const close = () => {
  emit('close')
}
const runAgain = () => {
  emit('run-again')
}

</script>

<template>

  <Card data-cy="quizCompletion" :pt="{ content: { class: 'p-0' } }">
    <template #content>
      <div class="text-2xl" tabindex="-1" ref="completionSummaryTitle" data-cy="completionSummaryTitle">
        <slot name="completeAboveTitle" v-if="!quizResult.outOfTime">
          <Message v-if="quizResult.gradedRes.passed || QuizType.isSurvey(quizInfo.quizType)" severity="success" icon="fas fa-handshake">
            <span v-if="QuizType.isSurvey(quizInfo.quizType)">Thank you for taking time to take this survey! </span>
            <span v-else>Thank you for completing the Quiz!</span>
          </Message>
        </slot>
        <Message severity="error" v-if="quizResult.outOfTime" data-cy="outOfTimeMsg">You've run out of time!</Message>
      </div>
      <div class="mb-1 mt-6 text-3xl">
        <h2 class="font-bold text-success mb-2 skills-page-title-text-color inline">{{ quizInfo.name }}</h2>
        <div v-if="!needsGrading" class="text-3xl inline-block ml-2">
          <Tag v-if="!quizResult.gradedRes.passed" class="uppercase text-2xl" severity="warn" data-cy="quizFailed"><i class="far fa-times-circle mr-1" aria-hidden="true"></i>Failed</Tag>
          <Tag v-if="quizResult.gradedRes.passed" class="uppercase text-2xl" severity="success" data-cy="quizPassed"><i class="fas fa-check-double mr-1" aria-hidden="true"></i>Passed</Tag>
        </div>
      </div>
      
      <div v-if="!needsGrading" class="flex flex-wrap flex-col md:flex-row gap-6 pt-2">
        <Card class="text-center bg-surface-50 dark:bg-surface-800 skills-card-theme-border flex-1" data-cy="numCorrectInfoCard">
          <template #content>
            <div class="text-2xl" data-cy="numCorrect" v-if="!quizResult.outOfTime">
              <Tag class="text-xl p-2" severity="success">{{ quizResult.numCorrect }}</Tag> out of <Tag class="text-xl p-2" severity="secondary">{{ quizResult.numTotal }}</Tag>
            </div>
            <div class="text-2xl" data-cy="timedOut" v-else-if="quizResult.outOfTime">
              <i class="fas fa-hourglass-end"></i> Time Expired
            </div>
            <div class="text-muted-color mt-2" data-cy="subTitleMsg">
              <span v-if="!quizResult.gradedRes.passed && quizResult.missedBy > 0 && !quizResult.outOfTime">Missed by <Tag severity="warn">{{ quizResult.missedBy }}</Tag> question{{ quizResult.missedBy > 1 ? 's' : '' }}</span>
              <span v-else-if="!quizResult.gradedRes.passed && quizResult.outOfTime">You've run out of time!</span>

              <span v-else>Well done!</span>
            </div>
          </template>
        </Card>
        <Card  class="text-center bg-surface-50 dark:bg-surface-800 skills-card-theme-border flex-1" data-cy="percentCorrectInfoCard">
          <template #content>        
          <div v-if="!quizResult.outOfTime">
            <div class="text-2xl">
              <span data-cy="percentCorrect">{{ quizResult.percentCorrect }}%</span>
            </div>
            <div class="text-muted-color mt-2">
              <b data-cy="percentToPass">{{ quizInfo.percentToPass }}%</b> is required to pass
            </div>
          </div>
          <div v-else>
              <div class="text-2xl">
                <i class="fas fa-clock"></i> {{ timeUtils.formatDuration(quizInfo.quizTimeLimit * 1000) }}
              </div>
              <div class="text-muted-color mt-2">
                You must complete the quiz within the time limit.
              </div>
          </div>
          </template>
        </Card>
        
        <Card v-if="quizResult.gradedRes.passed" class="text-center bg-surface-50 dark:bg-surface-800 skills-card-theme-border flex-1" data-cy="quizRuntime">
          <template #content>
            <div class="text-2xl" data-cy="title">
              {{ timeUtils.formatDurationDiff(quizResult.gradedRes.started, quizResult.gradedRes.completed) }}
            </div>
            <div class="text-muted-color mt-2" data-cy="subTitle">
              Time to Complete
            </div>
          </template>
        </Card>

        <Card v-if="!quizResult.gradedRes.passed" class="text-center bg-surface-50 dark:bg-surface-800 skills-card-theme-border flex-1" data-cy="numAttemptsInfoCard">
          <template #content>
            <div class="text-2xl" data-cy="title">
              <span v-if="unlimitedAttempts" class=""><i class="fas fa-infinity" aria-hidden="true"></i> Attempts</span>
              <span v-if="!unlimitedAttempts">
              <span v-if="numAttemptsLeft === 0">No</span>
              <Tag v-else severity="success">{{ numAttemptsLeft }}</Tag> More Attempt{{ numAttemptsLeft !== 1 ? 's' : '' }}
            </span>
            </div>
            <div class="text-muted-color mt-2" data-cy="subTitle">
              <span v-if="unlimitedAttempts">Unlimited Attempts - <Tag severity="warn">{{ quizInfo.userNumPreviousQuizAttempts  + 1 }}</Tag> attempt so far</span>
              <span v-if="!unlimitedAttempts">Used <Tag severity="warn">{{ quizInfo.userNumPreviousQuizAttempts  + 1 }}</Tag> out of <Tag severity="success">{{ quizInfo.maxAttemptsAllowed }}</Tag> attempts</span>
            </div>
          </template>
        </Card>
      </div>

      <quiz-completed-message v-if="needsGrading" />

      <div v-if="!quizResult.gradedRes.passed && !needsGrading" class="mt-6">
        <div class="my-2" v-if="unlimitedAttempts || numAttemptsLeft > 0"><span class="text-primary">No worries!</span> Would you like to try again?</div>
        <SkillsButton icon="fas fa-times-circle"
                      outlined
                      severity="danger"
                      label="Close"
                      @click="close" 
                      class="uppercase font-bold mr-2 skills-theme-btn"
                      data-cy="closeQuizBtn">
        </SkillsButton>
        <SkillsButton v-if="unlimitedAttempts || numAttemptsLeft > 0"
                      icon="fas fa-redo"
                      outlined
                      severity="success"
                      label="Try Again"
                      @click="runAgain" 
                      class="uppercase font-bold skills-theme-btn"
                      data-cy="runQuizAgainBtn">
        </SkillsButton>
      </div>

      <div v-if="quizResult.gradedRes.passed || needsGrading" class="mt-6">
        <SkillsButton icon="fas fa-times-circle"
                      outlined
                      severity="success"
                      label="Close"
                      @click="close"
                      class="uppercase font-bold skills-theme-btn"
                      data-cy="closeQuizBtn">
        </SkillsButton>
      </div>
      
    </template>
  </Card>

</template>

<style scoped>

</style>