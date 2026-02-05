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
const isMatchingType = computed(() => {
  return props.question.questionType === QuestionType.Matching;
})
const hasAnswer = computed(() => {
  if (isMatchingType.value) {
    return props.question.answers.find((a) => a.answer?.selectedMatch === null || a.answer?.selectedMatch === undefined || a.answer?.selectedMatch === '') === undefined;
  }
  return props.question.answers.find((a) => a.isSelected === true) !== undefined;
})
const needsGrading = computed(() => {
  return props.question.needsGrading
})
const isWrong = computed(() => {
  return hasAnswer.value && !needsGrading.value && !props.question.isCorrect
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
const isAiGraded = computed(() => {
  const aiAssistant = 'AI Assistant'
  const gradeInfo = manuallyGradedInfo.value
  return gradeInfo?.graderUserIdForDisplay === aiAssistant || gradeInfo?.graderUserId === aiAssistant
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
          <div v-if="!isTextInputType && !isRatingType && !isMatchingType">
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

          <div v-if="isMatchingType">
            <InlineMessage v-if="!hasAnswer" data-cy="noAnswerYet">Answer is not provided yet</InlineMessage>
            <div v-else class="flex gap-2">
              <ul>
                <li v-for="(answer, index) in question.answers"
                    :key="answer.id"
                    :data-cy="`term-${index}`"
                    class="min-h-[3rem] px-3 py-1 flex items-center mb-2">{{ answer.answer.term }}:</li>
              </ul>
              <ul>
                <li v-for="(answer, index) in question.answers"
                    :key="answer.id"
                    :class="{
                      'bg-red-50 border-red-200 dark:bg-red-900 text-red-950 dark:text-red-100': !answer.isSelected && isWrong,
                      'bg-green-50 border-green-200 dark:bg-green-800 text-green-950 dark:text-green-100': !(!answer.isSelected && isWrong),
                    }"
                    :aria-label="`Answer ${answer.matchedAnswer} is ${!answer.isSelected && isWrong ? 'wrong' : 'correct'}`"
                    :data-cy="`match-${index}`"
                    class="min-h-[3rem] items-center mb-2 border-2 rounded px-3 py-1 flex gap-2">
                  <i v-if="!answer.isSelected && isWrong" class="fas fa-ban text-red-500" aria-hidden="true" data-cy="matchIsWrong"></i>
                  <i v-else class="fas fa-check text-green-500" aria-hidden="true" data-cy="matchIsCorrect"></i>
                  <span data-cy="matchVal">{{ answer.answer.selectedMatch }}</span>
                </li>
              </ul>
            </div>
          </div>
          <div v-if="manuallyGradedInfo" class="mt-4 w-full border p-4 rounded-border border-surface sd-theme-primary-color" data-cy="manuallyGradedInfo">

            <div class="text-xl mb-4 font-semibold">
              <div v-if="isAiGraded" class="flex gap-2">
                <Avatar icon="fa-solid fa-wand-magic-sparkles" shape="circle" class="bg-indigo-600! text-purple-100!" aria-hidden="true"/>
                <div>AI Graded</div>
              </div>
              <div v-else class="flex gap-2">
                <Avatar icon="fa-solid fa-pen-to-square" shape="circle" class="bg-teal-600! text-teal-100!" aria-hidden="true"/>
                <div>Manually Graded</div>
              </div>
            </div>

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