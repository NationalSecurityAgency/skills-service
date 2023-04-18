/*
Copyright 2020 SkillTree

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
<template>
<div>
  <sub-page-header title="User Run">
    <b-button :to="{ name: 'QuizRunsHistoryPage' }"
              :aria-label="`Return back to all the ${runInfo.quizType} results`"
              variant="outline-primary" size="sm"
              data-cy="quizRunBackBtn">
      <i class="fas fa-arrow-alt-circle-left" aria-hidden="true"/> Back
    </b-button>
  </sub-page-header>

  <b-card body-class="mb-4">
    <skills-spinner v-if="loading" :is-loading="loading"/>
    <div v-if="!loading">
      <div class="row h-100">
        <div class="col-md-6 col-xl mb-2">
          <b-card class="h-100" data-cy="userInfoCard">
            <div class="text-uppercase text-secondary">User</div>
            <div class="text-primary font-weight-bold">{{ runInfo.userIdForDisplay }}</div>
            <div v-if="showUserTagColumn && runInfo.userTag"><span class="text-info font-italic">{{ userTagLabel }}</span>: {{ runInfo.userTag }}</div>
          </b-card>
        </div>
        <div class="col-md-6 col-xl mb-2" data-cy="quizRunStatus">
          <b-card class="h-100">
            <div class="text-uppercase text-secondary">Status</div>
            <div class="text-primary font-weight-bold"><quiz-run-status :quiz-type="runInfo.quizType" :status="runInfo.status" /></div>
            <div v-if="runInfo.status === 'INPROGRESS'"><b-badge variant="warning">{{ numQuestionsAnswered }}</b-badge> / <b-badge>{{ runInfo.questions.length }}</b-badge></div>
            <div v-if="runInfo.status === 'FAILED'">Missed by <span class="text-danger font-italic">{{ runInfo.numQuestionsToPass - numQuestionsRight }}</span> questions</div>
          </b-card>
        </div>
        <div v-if="runInfo.quizType === 'Quiz'"
             class="col-md-6 col-xl mb-2"
             data-cy="numQuestionsToPass">
          <b-card class="h-100">
            <div class="text-uppercase text-secondary">Questions</div>
            <div class="text-primary font-weight-bold"><b-badge variant="success">{{ numQuestionsRight }}</b-badge> / <b-badge>{{ runInfo.questions.length }}</b-badge></div>
            <div>Need <span class="text-info font-italic">{{ runInfo.numQuestionsToPass }}</span> questions to pass</div>
          </b-card>
        </div>
        <div class="col-md-6 col-xl mb-2">
          <b-card class="h-100">
            <div class="text-uppercase text-secondary">Runtime</div>
            <div class="text-primary font-weight-bold">{{ runInfo.started | duration(runInfo.completed) }}</div>
          </b-card>
        </div>
      </div>
      <div v-for="(q, index) in runInfo.questions" :key="q.id">
        <div class="mt-4">
          <quiz-run-question-card :question="q" :question-num="index+1" :quiz-type="runInfo.quizType"/>
        </div>
      </div>
    </div>
  </b-card>

</div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import QuizService from '@/components/quiz/QuizService';
  import QuizRunQuestionCard from '@/components/quiz/runsHistory/QuizRunQuestionCard';
  import QuizRunStatus from '@/components/quiz/runsHistory/QuizRunStatus';
  import UserTagsConfigMixin from '@/components/users/UserTagsConfigMixin';

  export default {
    name: 'QuizRunsHistoryPage',
    mixins: [UserTagsConfigMixin],
    components: {
      QuizRunQuestionCard,
      SkillsSpinner,
      SubPageHeader,
      QuizRunStatus,
    },
    data() {
      return {
        loading: true,
        quizId: this.$route.params.quizId,
        attemptId: this.$route.params.runId,
        runInfo: {},
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      numQuestionsAnswered() {
        const nums = this.runInfo.questions.map((q) => {
          const hasAnswer = q.answers.find((a) => a.isSelected === true) !== undefined;
          if (hasAnswer) {
            return 1;
          }
          return 0;
        });
        return nums.reduce((partialSum, a) => partialSum + a, 0);
      },
      numQuestionsRight() {
        const nums = this.runInfo.questions.map((q) => {
          const incorrectAnswer = q.answers.find((a) => a.isSelected !== a.isConfiguredCorrect) !== undefined;
          if (incorrectAnswer) {
            return 0;
          }
          return 1;
        });
        return nums.reduce((partialSum, a) => partialSum + a, 0);
      },
    },
    methods: {
      loadData() {
        this.loading = true;
        QuizService.getSingleQuizHistoryRun(this.quizId, this.attemptId)
          .then((res) => {
            this.runInfo = res;
          }).finally(() => {
            this.loading = false;
          });
      },
    },
  };
</script>

<style>
.controls-column {
  max-width: 2.5rem !important;
}

</style>
