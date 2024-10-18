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

import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { useUserTagsUtils } from '@/components/utils/UseUserTagsUtils.js';
import QuizService from '@/components/quiz/QuizService.js';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import QuizRunStatus from '@/components/quiz/runsHistory/QuizRunStatus.vue';
import QuizRunQuestionCard from '@/components/quiz/runsHistory/QuizRunQuestionCard.vue';
import QuestionType from "@/skills-display/components/quiz/QuestionType.js";

const timeUtils = useTimeUtils();
const userTagsUtils = useUserTagsUtils();
const loading = ref(false);
const runInfo = ref({});
const route = useRoute();
const isLoading = ref(true);
const quizId = ref(route.params.quizId);
const attemptId = ref(route.params.runId);

const numQuestionsAnswered = computed(() => {
  const nums = runInfo.value.questions.map((q) => {
    const hasAnswer = q.answers.find((a) => a.isSelected === true) !== undefined;
    if (hasAnswer) {
      return 1;
    }
    return 0;
  });
  return nums.reduce((partialSum, a) => partialSum + a, 0);
});
const numQuestionsRight = computed(() => {
  const nums = runInfo.value.questions.map((q) => q.isCorrect ? 1 :0 )
  return nums.reduce((partialSum, a) => partialSum + a, 0);
});

const loadData = () => {
  loading.value = true;
  QuizService.getSingleQuizHistoryRun(quizId.value, attemptId.value)
      .then((res) => {
        runInfo.value = res;
      }).finally(() => {
    loading.value = false;
  });
}

onMounted(() => {
  loadData();
})
</script>

<template>

  <div>
    <SubPageHeader title="User Run">
      <router-link :aria-label="`Return back to all the ${runInfo.quizType} results`"
                   :to="{ name: 'QuizRunsHistoryPage' }" tabindex="-1">
        <SkillsButton icon="fas fa-arrow-alt-circle-left"
                      outlined
                      size="small"
                      data-cy="quizRunBackBtn"
                      id="btn_Questions"
                      label="Back">
        </SkillsButton>
      </router-link>
    </SubPageHeader>

    <Card>
      <template #content>
        <div class="grid">
          <div class="md:col-6 xl:col mb-2">
            <Card class="w-full h-full" data-cy="userInfoCard">
              <template #content>
                <div class="uppercase text-color-secondary">User</div>
                <div class="text-primary font-bold">{{ runInfo.userIdForDisplay }}</div>
                <div v-if="userTagsUtils.showUserTagColumn() && runInfo.userTag">
                  <span class="text-info font-italic">{{ userTagsUtils.userTagLabel() }}</span>: {{ runInfo.userTag }}
                </div>
              </template>
            </Card>
          </div>
          <div class="md:col-6 xl:col mb-2" data-cy="quizRunStatus">
            <Card class="w-full h-full">
              <template #content>
                <div class="uppercase text-color-secondary">Status</div>
                <div class="text-primary font-bold">
                  <QuizRunStatus :quiz-type="runInfo.quizType" :status="runInfo.status"/>
                </div>
                <div v-if="runInfo.status === 'INPROGRESS'">
                  <Tag severity="warning">{{ numQuestionsAnswered }}</Tag>
                  /
                  <Tag>{{ runInfo.questions.length }}</Tag>
                </div>
                <div v-if="runInfo.status === 'FAILED'">Missed by <span
                    class="text-danger font-italic">{{ runInfo.numQuestionsToPass - numQuestionsRight }}</span>
                  questions
                </div>
              </template>
            </Card>
          </div>
          <div v-if="runInfo.quizType === 'Quiz'"
               class="md:col-6 xl:col mb-2"
               data-cy="numQuestionsToPass">
            <Card class="w-full h-full">
              <template #content>
                <div class="uppercase text-color-secondary">Questions</div>
                <div class="text-primary font-bold">
                  <Tag severity="success">{{ numQuestionsRight }}</Tag>
                  /
                  <Tag>{{ runInfo.questions.length }}</Tag>
                </div>
                <div>Need <span class="text-info font-italic">{{ runInfo.numQuestionsToPass }}</span> questions to pass
                </div>
              </template>
            </Card>
          </div>
          <div class="md:col-6 xl:col mb-2">
            <Card class="w-full h-full">
              <template #content>
                <div class="uppercase text-color-secondary">Runtime</div>
                <div class="text-primary font-bold">{{
                    timeUtils.formatDurationDiff(runInfo.started, runInfo.completed)
                  }}
                </div>
              </template>
            </Card>
          </div>
        </div>

        <div v-for="(q, index) in runInfo.questions" :key="q.id">
          <div class="mt-4">
            <QuizRunQuestionCard :question="q" :question-num="index+1" :quiz-type="runInfo.quizType"/>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>