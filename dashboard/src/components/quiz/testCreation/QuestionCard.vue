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
import { useQuizConfig } from "@/stores/UseQuizConfig.js";
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';
import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer.vue';
import {useRoute} from "vue-router";
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import { useDebounceFn } from '@vueuse/core'
import { useAppConfig } from "@/common-components/stores/UseAppConfig.js";

const route = useRoute()
const quizConfig = useQuizConfig()
const appConfig = useAppConfig()

const props = defineProps({
  quizType: String,
  question: Object,
  questionNum: Number,
  showDragAndDropControls: Boolean,
  showEditControls: {
    type: Boolean,
    default: true
  },
  supportsEditQuestionInline: {
    type: Boolean,
    default: false
  },
  showEditQuestionInline: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['editQuestion', 'deleteQuestion', 'sortChangeRequested', 'copyQuestion', 'questionUpdated'])

const showDeleteDialog = ref(false)

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
const isDragAndDropControlsVisible = computed(() => {
  return !quizConfig.isReadOnlyQuiz && props.showDragAndDropControls;
})
const numberOfStars = computed(() => {
  return props.question.answers ? props.question.answers.length : 3;
})
const mediaAttributes = computed(() => {
  const media = props.question.attributes?.videoConf
  if(media) {
    media.isAudio = media.videoType?.includes('audio/')
  }
  return media
})
const textInputAiGraderConfigured = computed(() =>  appConfig.enableOpenAIIntegration && props.question.attributes?.textInputAiGradingConf?.enabled === true)
const editQuestion = () => {
  emit('editQuestion', props.question)
}
const copyQuestion = () => {
  emit('copyQuestion', props.question)
}
const deleteQuestion = () => {
  emit('deleteQuestion', props.question)
}
const moveQuestion = (changeIndexBy) => {
  emit('sortChangeRequested', { question: props.question, newIndex: props.questionNum + changeIndexBy - 1 })
}

const questionUpdatedDebounced = useDebounceFn((updatedQuestionText) => questionUpdated(updatedQuestionText), appConfig.formFieldDebounceInMs)
const questionUpdated = (updatedQuestionText) => {
  emit('questionUpdated', { question: props.question, updatedQuestionText })
}

</script>

<template>
  <div class="border border-surface-300 dark:border-surface-500" data-cy="questionDisplayCard">
    <div class="flex flex-col md:flex-row flex-wrap gap-0 mb-4" :data-cy="`questionDisplayCard-${questionNum}`">
      <div class="flex flex-initial items-start">
        <div v-if="isDragAndDropControlsVisible"
             :id="`questionSortControl-${question.id}`"
             class="sort-control mr-4 border-r border-b border-surface text-muted-color rounded-border"
             @click.prevent.self
             tabindex="0"
             aria-label="Questions Sort Control. Press up or down to change the order of this question."
             role="button"
             @keyup.down="moveQuestion(1)"
             @keyup.up="moveQuestion(-1)"
             data-cy="sortControlHandle">
          <i class="fas fa-arrows-alt"/>
        </div>
      </div>
      <div :class="{ 'ml-3' : !isDragAndDropControlsVisible }" class="flex-col flex-1 items-start px-2 py-1">
        <div class="flex flex-1">
          <div v-if="supportsEditQuestionInline">
            <markdown-editor v-show="showEditQuestionInline"
                             class="w-full"
                             :value="question.question"
                             :id="`${question.id}`"
                             :data-cy="`questionDisplayTextEditor-${question.id}`"
                             :name="question.name"
                             :disable-ai-prompt="true"
                             :allow-attachments="false"
                             :allow-insert-images="false"
                             @value-changed="questionUpdatedDebounced"
                             label="Question"
                             markdownHeight="150px"/>
          </div>

          <markdown-text v-if="!showEditQuestionInline"
                         :text="question.question"
                         :instance-id="`${question.id}`"
                         data-cy="questionDisplayText"/>
        </div>
        <div class="flex flex-col gap-2 mb-3">
          <div v-if="mediaAttributes" class="flex gap-2 items-center">
            <div class="border rounded py-1 bg-slate-600! text-orange-300! w-[2.5rem] text-center" data-cy="questionHasVideoOrAudio">
              <i :class="`${mediaAttributes.isAudio ? 'fa-solid fa-volume-high' : 'fa-solid fa-film'}`" aria-hidden="true" />
            </div>
            <span class="uppercase " aria-label="Video is configured for this question">{{ mediaAttributes.isAudio ? 'Audio' : 'Video' }}</span>
          </div>
          <div v-if="textInputAiGraderConfigured" class="flex gap-2 items-center" data-cy="questionAiGraded">
            <div class="border rounded-2xl py-1 bg-indigo-600! text-purple-100! w-[2.5rem] text-center">
              <i class="fa-solid fa-robot" aria-hidden="true"></i>
            </div>
            <span class="" aria-label="This question is graded by AI">Graded via AI</span>
          </div>
        </div>
        <div v-if="!isTextInputType && !isRatingType && !isMatchingType">
          <div v-for="(a, index) in question.answers" :key="a.id" class="flex flex-row flex-wrap mt-1 pl-1">
            <div class="flex items-center justify-center pb-1" :data-cy="`answerDisplay-${index}`">
              <SelectCorrectAnswer v-model="a.isCorrect"
                                   :name="`answers[${index}].isCorrect`"
                                   :answer-number="index+1"
                                   :read-only="true"
                                   :is-radio-icon="isSingleChoiceType"
                                   font-size="1.3rem"/>
            </div>
            <div class="flex items-center justify-center ml-2 pb-1">
              <div class="answerText" :data-cy="`answer-${index}_displayText`">{{ a.answer }}</div>
            </div>
          </div>
        </div>
        <div v-if="isRatingType" class="flex">
          <Rating class="flex-initial bg-surface-100 dark:bg-surface-700 rounded-border py-4 px-6" :stars="numberOfStars" disabled :cancel="false"/>
        </div>
        <div v-if="isTextInputType" class="flex">
          <label :for="`q${questionNum}textInputPlaceholder`" hidden>Text Input Answer Placeholder:</label>
          <Textarea
              style="resize: none"
              class="flex-1"
              :id="`q${questionNum}textInputPlaceholder`"
              placeholder="Users will be required to enter text."
              disabled
              aria-hidden="true"
              data-cy="textAreaPlaceHolder"
              rows="2"/>
        </div>
        <div v-if="isMatchingType" class="flex flex-col gap-3 mt-2">
          <div v-for="(answer, index) in question.answers">
            <div v-if="answer.multiPartAnswer" class="flex flex-row gap-3" :data-cy="`question-${questionNum}-answer-${index}`">
              <div :data-cy="`question-${questionNum}-answer-${index}-term`">
                {{ answer.multiPartAnswer.term }}
              </div>
              <div>
                <i class="fas fa-arrow-right text-gray-500 dark:text-gray-400" aria-hidden="true"></i>
              </div>
              <div :data-cy="`question-${questionNum}-answer-${index}-value`">
                {{ answer.multiPartAnswer.value }}
              </div>
            </div>
            <hr v-if="index < question.answers.length - 1"
                class="mt-3 border-t border-dashed border-gray-300 dark:border-gray-600"/>
          </div>
        </div>
        <div class="flex" v-if="question.answerHint">
          <Message size="small" severity="warn" icon="fas fa-lightbulb" :closable="false" class="mt-2" data-cy="answerHintMsg">
            <pre data-cy="answerHintMsgContent">{{ question.answerHint }}</pre>
          </Message>
        </div>
      </div>
      <div v-if="!quizConfig.isReadOnlyQuiz && showEditControls" class="flex flex-col gap-2 pr-4">
        <div>
          <ButtonGroup class="ml-1 mt-2">
            <SkillsButton @click="editQuestion"
                          icon="fas fa-edit"
                          label="Edit"
                          outlined
                          size="small"
                          :data-cy="`editQuestionButton_${questionNum}`"
                          :aria-label="`Edit Question Number ${questionNum}`"
                          :ref="`editQuestion_${question.id}`"
                          :id="`editQuestion_${question.id}`"
                          :track-for-focus="true"
                          title="Edit Question">
            </SkillsButton>
            <SkillsButton @click="copyQuestion"
                          icon="fas fa-copy"
                          label="Copy"
                          outlined
                          size="small"
                          :data-cy="`copyQuestionButton_${questionNum}`"
                          :aria-label="`Copy Question Number ${questionNum}`"
                          :ref="`copyQuestion_${question.id}`"
                          :id="`copyQuestion_${question.id}`"
                          :track-for-focus="true"
                          title="Copy Question">
            </SkillsButton>
            <SkillsButton @click="showDeleteDialog = true"
                          icon="text-warning fas fa-trash"
                          label="Delete"
                          outlined
                          size="small"
                          :data-cy="`deleteQuestionButton_${questionNum}`"
                          :aria-label="`delete question number ${questionNum}`"
                          :ref="`deleteQuestion_${question.id}`"
                          :id="`deleteQuestion_${question.id}`"
                          :track-for-focus="true"
                          title="Delete Question">
            </SkillsButton>
          </ButtonGroup>
        </div>
        <div class="flex justify-end">
          <router-link :aria-label="`Configure video for question ${question.id}`" :data-cy="`add-video-question-${questionNum}`"
                       :to="`/administrator/quizzes/${route.params.quizId}/questions/${question.id}/config-video`" tabindex="-1">
            <div class="flex gap-1 items-center">
              <Avatar icon="fa-regular fa-play-circle" shape="circle" class="bg-slate-600! text-orange-300!"/>
              <div class="underline">{{ question.attributes?.videoConf ? "Edit Audio/Video" : "Add Audio/Video"}}</div>
            </div>
          </router-link>
        </div>
        <div v-if="isTextInputType && appConfig.enableOpenAIIntegration" class="flex justify-end">
          <router-link :aria-label="`Configure ai grading for question ${question.id}`" :data-cy="`ai-grader-question-${questionNum}`"
                       :to="`/administrator/quizzes/${route.params.quizId}/questions/${question.id}/ai-grader`" tabindex="-1">
            <div class="flex gap-1 items-center">
              <Avatar icon="fa-solid fa-wand-magic-sparkles" shape="circle" class="bg-indigo-600! text-purple-100!"/>
              <div class="underline">AI Grader</div>
            </div>
          </router-link>
        </div>
      </div>

      <removal-validation
          v-if="showDeleteDialog"
          :item-name="`Question #${questionNum}`"
          item-type=""
          v-model="showDeleteDialog"
          focus-on-close-id="btn_Questions"
          @do-remove="deleteQuestion">
        <div>
          Any associated answers and metrics for this question will also be removed. Please proceed with caution.
        </div>
      </removal-validation>
    </div>
  </div>


</template>


<!--TODO: figure scss and theming-->
<!--@import "@/assets/custom";-->
<style lang="css" scoped>

.sort-control i {
  padding: 0.4rem;
  font-size: 1.2rem;
  top: 0rem;
  left: 0rem;
  border-bottom-right-radius: .25rem !important
}

.sort-control:hover, .sort-control i:hover {
  cursor: grab !important;
  color: #146c75 !important;
  font-size: 1.5rem;
}

.answerText {
  font-size: 0.9rem;
}

</style>