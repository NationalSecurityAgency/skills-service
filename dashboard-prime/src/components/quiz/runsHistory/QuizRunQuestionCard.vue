<script setup>
import { computed, ref } from 'vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue';
import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer.vue';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import SkillsOverlay from "@/components/utils/SkillsOverlay.vue";

const props = defineProps({
  quizType: String,
  question: Object,
  questionNum: Number,
})

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
const isWrong = computed(() => {
  return props.question.answers.find((a) => hasAnswer.value && a.isConfiguredCorrect !== a.isSelected) !== undefined;
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
</script>

<template>
  <div data-cy="questionDisplayCard">
    <div :data-cy="`questionDisplayCard-${questionNum}`">
      <div v-if="!hasAnswer" class="flex flex-row" data-cy="noAnswer">
        <Tag severity="warning">No Answer</Tag>
      </div>
      <div class="flex flex-row flex-wrap gap-0 mb-3">
        <div class="col-auto py-2 pr-2">
          <SkillsOverlay :show="!isSurvey && isWrong" opacity="0">
            <template #overlay>
              <i class="fa fa-ban text-danger text-red-500" style="font-size: 2.1rem; opacity: 0.8"
                 data-cy="wrongAnswer"></i>
            </template>
            <Tag severity="primary" :aria-label="`Question number ${questionNum}`">{{ questionNum }}</Tag>
          </SkillsOverlay>
        </div>
        <div class="flex flex-column flex-1 align-items-start px-2 py-1">
          <div class="flex flex-1">
            <MarkdownText
                :text="question.question"
                :instance-id="`${question.id}`"
                data-cy="questionDisplayText"/>
          </div>
          <div v-if="!isTextInputType && !isRatingType">
            <div v-for="(a, index) in question.answers" :key="a.id" class="flex flex-row flex-wrap mt-1 pl-1">
              <div class="flex align-items-center justify-content-center pb-1" :data-cy="`answerDisplay-${index}`">
                <SelectCorrectAnswer v-model="a.isSelected"
                                     :name="`answers[${index}].isSelected`"
                                     :read-only="true"
                                     :is-radio-icon="isSingleChoiceType"
                                     :markIncorrect="!isSurvey && hasAnswer && a.isConfiguredCorrect !== a.isSelected"
                                     font-size="1.3rem"/>
              </div>
              <div class="flex align-items-center justify-content-center ml-2 pb-1">
                <div class="answerText" :data-cy="`answer-${index}_displayText`">{{ a.answer }}</div>
              </div>
            </div>
          </div>
          <div v-if="isRatingType" class="flex">
            <Rating class="flex-initial py-3 px-4" v-model="surveyScore" :stars="numberOfStars" readonly :cancel="false"/>
          </div>
          <div v-if="isTextInputType" class="flex border-1 border-300 border-round p-3" data-cy="TextInputAnswer">
            <pre>{{ answerText }}</pre>
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