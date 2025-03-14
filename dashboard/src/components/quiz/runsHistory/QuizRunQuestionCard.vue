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
import { computed, ref } from 'vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue';
import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer.vue';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import SkillsOverlay from "@/components/utils/SkillsOverlay.vue";
import { useTimeUtils } from "@/common-components/utilities/UseTimeUtils.js";

const props = defineProps({
  quizType: String,
  question: Object,
  questionNum: Number,
})

const timeUtils = useTimeUtils();

const answerText = ref(props.question.answers[0].answer)

const isSingleChoiceType = computed(() => {
  return props.question.questionType === QuestionType.SingleChoice;
})
const isTextInputType = computed(() => {
  return props.question.questionType === QuestionType.TextInput;
})
const isRatingType = computed(() => {
  return props.question.questionType === QuestionType.Rating;
})
const hasAnswer = computed(() => {
  return props.question.answers.find((a) => a.isSelected === true) !== undefined;
})
const needsGrading = computed(() => {
  return props.question.needsGrading
})
const isWrong = computed(() => {
  return !needsGrading.value && !props.question.isCorrect
})
const isSurvey = computed(() => {
  return props.quizType === 'Survey';
})
const surveyScore = computed(() => {
  const answer = props.question.answers.find((a) => a.isSelected === true);
  return Number(answer?.answer);
})
const numberOfStars = computed(() => {
  return props.question.answers ? props.question.answers.length : 3;
})
const manuallyGradedInfo = computed(() => {
  if (!props.question || props.question.length === 0) {
    return null
  }
  return props.question.answers[0].gradingResult
})
</script>

<template>
  <div data-cy="questionDisplayCard">
    <div :data-cy="`questionDisplayCard-${questionNum}`">
      <div v-if="needsGrading" class="flex flex-row" data-cy="noAnswer">
        <Tag severity="warn" class="uppercase" data-cy="needsGradingTag"><i class="fas fa-user-check mr-1" aria-hidden="true"></i> Needs Grading</Tag>
      </div>
      <div v-if="!hasAnswer" class="flex flex-row" data-cy="noAnswer">
        <Tag severity="warn">No Answer</Tag>
      </div>
      <div class="flex flex-row flex-wrap gap-0 mb-4">
        <div class="col-auto py-4 pr-2">
          <SkillsOverlay :show="!isSurvey && isWrong" opacity="0">
            <template #overlay>
              <i class="fa fa-ban text-red-500" style="font-size: 2.1rem; opacity: 0.8"
                 data-cy="wrongAnswer"></i>
            </template>
            <Tag severity="primary" :aria-label="`Question number ${questionNum}`">{{ questionNum }}</Tag>
          </SkillsOverlay>
        </div>
        <div class="flex flex-col flex-1 items-start px-2 py-1">
          <div class="flex flex-1">
            <MarkdownText
                :text="question.question"
                :instance-id="`question_${question.id}`"
                data-cy="questionDisplayText"/>
          </div>
          <div v-if="!isTextInputType && !isRatingType">
            <div v-for="(a, index) in question.answers" :key="a.id" class="flex flex-row flex-wrap mt-1 pl-1">
              <div class="flex items-center justify-center pb-1" :data-cy="`answerDisplay-${index}`">
                <SelectCorrectAnswer v-model="a.isSelected"
                                     :name="`answers[${index}].isSelected`"
                                     :read-only="true"
                                     :is-radio-icon="isSingleChoiceType"
                                     :markIncorrect="!isSurvey && hasAnswer && a.isConfiguredCorrect !== a.isSelected"
                                     font-size="1.3rem"/>
              </div>
              <div class="flex items-center justify-center ml-2 pb-1">
                <div class="answerText" :data-cy="`answer-${index}_displayText`">{{ a.answer }}</div>
              </div>
            </div>
          </div>
          <div v-if="isRatingType" class="flex">
            <Rating class="flex-initial py-4 px-6" v-model="surveyScore" :stars="numberOfStars" readonly :cancel="false"/>
          </div>
          <div v-if="isTextInputType" class="border border-surface-300 dark:border-surface-500 rounded-border p-4 w-full" data-cy="TextInputAnswer">
            <MarkdownText
                :text="answerText"
                :instance-id="`${question.id}_answer`"/>
          </div>
          <div v-if="manuallyGradedInfo" class="mt-4 w-full border p-4 rounded-border border-surface" data-cy="manuallyGradedInfo">
            <div class="text-xl mb-4 font-semibold">Manually Graded</div>

            <div class="flex gap-4">
              <div class="flex-1" data-cy="grader">Grader:
                {{ manuallyGradedInfo.graderUserIdForDisplay || manuallyGradedInfo.graderUserId }}
              </div>
              <div>On: {{ timeUtils.formatDate(manuallyGradedInfo.gradedOn) }}</div>
            </div>
            <div class="mt-4">Feedback:</div>
            <div v-if="manuallyGradedInfo.feedback" class="border border-surface-300 dark:border-surface-500 rounded-border border-dashed p-4 mt-1">
              <MarkdownText
                  data-cy="feedback"
                  :text="manuallyGradedInfo.feedback"
                  :instance-id="`${question.id}_feedback`"
                  :data-cy="`feedbackDisplayText_q${question.id}`"/>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
pre {
  overflow-x: auto;
  white-space: pre-wrap;
  white-space: -moz-pre-wrap;
  white-space: -pre-wrap;
  white-space: -o-pre-wrap;
  word-wrap: break-word;
}
</style>