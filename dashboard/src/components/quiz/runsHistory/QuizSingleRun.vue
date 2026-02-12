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

import QuizRunQuestionCard from "@/components/quiz/runsHistory/QuizRunQuestionCard.vue";
import QuizRunStatus from "@/components/quiz/runsHistory/QuizRunStatus.vue";
import {computed} from "vue";
import {useUserTagsUtils} from "@/components/utils/UseUserTagsUtils.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import QuizStatus from "@/components/quiz/runsHistory/QuizStatus.js";
import QuizType from "@/skills-display/components/quiz/QuizType.js";
import QuizSingleRunCard from "@/components/quiz/runsHistory/QuizSingleRunCard.vue";
import QuizCompletedMessage from "@/skills-display/components/quiz/QuizCompletedMessage.vue";

const props = defineProps({
  runInfo: Object,
  showCards: {
    type: Boolean,
    default: true,
  },
  showUserCard: {
    type: Boolean,
    default: true,
  },
  showQuestionCard: {
    type: Boolean,
    default: true,
  },
  showQuizUnderReview: {
    type: Boolean,
    default: true,
  },
  showAiGradingMeta: {
    type: Boolean,
    default: false,
  }
})
const userTagsUtils = useUserTagsUtils();
const timeUtils = useTimeUtils();

const numQuestionsAnswered = computed(() => {
  const nums = props.runInfo.questions.map((q) => {
    const hasAnswer = q.answers.find((a) => a.isSelected === true) !== undefined;
    if (hasAnswer) {
      return 1;
    }
    return 0;
  });
  return nums.reduce((partialSum, a) => partialSum + a, 0);
});
const numQuestionsRight = computed(() => props.runInfo.numQuestionsPassed);
</script>

<template>
  <div>
    <div v-if="showCards" class="flex flex-col md:flex-row flex-wrap gap-4">
      <div v-if="showUserCard" class="flex-1 w-min-12rem">
        <quiz-single-run-card title="User" data-cy="userInfoCard">
          <div class="text-green-700 dark:text-green-400 font-bold">{{ runInfo.userIdForDisplay }}</div>
          <div v-if="userTagsUtils.showUserTagColumn() && runInfo.userTag">
            <span class="text-info italic">{{ userTagsUtils.userTagLabel() }}</span>: {{ runInfo.userTag }}
          </div>
        </quiz-single-run-card>
      </div>
      <div class="flex-1 w-min-12rem" data-cy="quizRunStatus">
        <quiz-single-run-card title="Status">
            <div class="text-primary font-bold">
              <QuizRunStatus :quiz-type="runInfo.quizType" :status="runInfo.status"/>
            </div>
            <div v-if="QuizStatus.isInProgress(runInfo.status)">
              <Tag severity="warn">{{ numQuestionsAnswered }}</Tag>
              /
              <Tag>{{ runInfo.numQuestions }}</Tag>
            </div>
            <div v-if="QuizStatus.isFailed(runInfo.status)">Missed by <span
                class="text-danger italic">{{ runInfo.numQuestionsToPass - numQuestionsRight }}</span>
              questions
            </div>
        </quiz-single-run-card>
      </div>
      <div v-if="QuizType.isQuiz(runInfo.quizType) && showQuestionCard" class="flex-1 w-min-12rem"
           data-cy="numQuestionsToPass">
        <quiz-single-run-card title="Questions">
            <div class="text-primary font-bold">
              <Tag severity="success">{{ numQuestionsRight }}</Tag>
              /
              <Tag>{{ runInfo.numQuestions }}</Tag>
            </div>
            <div>Need <span class="text-info italic">{{ runInfo.numQuestionsToPass }}</span> question{{ runInfo.numQuestionsToPass > 1 ? 's' : ''}} to pass
            </div>
        </quiz-single-run-card>
      </div>
      <div class="flex-1 w-min-12rem">
        <quiz-single-run-card title="Date & Time">
          <div class="text-green-700 dark:text-green-400 font-bold">{{ timeUtils.formatDate(runInfo.started) }}</div>
          <div class="mt-1">
            <span class="italic">Runtime:</span>
            <span class="text-primary ml-2">{{
                timeUtils.formatDurationDiff(runInfo.started, runInfo.completed)
              }}
            </span>
          </div>
        </quiz-single-run-card>
      </div>
    </div>

    <div v-if="runInfo.questions" v-for="q in runInfo.questions" :key="q.id">
      <div :class="{ 'mt-4' : showCards }">
        <QuizRunQuestionCard :question="q" :question-num="q.questionNum" :quiz-type="runInfo.quizType" :show-ai-grading-meta="showAiGradingMeta"/>
      </div>
    </div>
    <quiz-completed-message v-if="showQuizUnderReview && QuizStatus.isNeedsGrading(runInfo.status)" />
    <Message v-if="!runInfo.questions && QuizStatus.isFailed(runInfo.status)" severity="warn" :closable="false" data-cy="allQuestionsNotDisplayedMsg">
      Questions and answers are not displayed so not to give away the correct answers.
    </Message>
    <Message v-if="runInfo.questions && !runInfo.allQuestionsReturned" severity="warn" :closable="false" class="mt-8" data-cy="someQuestionsNotDisplayedMsg">
      The rest of the questions and answers are not displayed so not to give away the correct answers.
    </Message>
  </div>
</template>

<style scoped>

</style>