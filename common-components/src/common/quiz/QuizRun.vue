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
<div class="container-fluid pb-5">
  <skills-spinner :is-loading="isLoading" class="mt-3"/>
  <div v-if="!isLoading">
    <div class="row bg-white border-bottom py-2 mb-3 pt-4" data-cy="subPageHeader">
      <div class="col">
        <div class="h4 text-success">{{ quizInfo.name }}</div>
      </div>
      <div class="col-auto text-right text-muted">
        <b-badge variant="success">{{quizInfo.questions.length}}</b-badge> <span class="text-uppercase">questions</span>
      </div>
    </div>

    <b-card v-if="quizResult" class="mb-3" body-class="text-center">
      <div>Thank you completing <span class="text-primary font-weight-bold">{{ quizInfo.name }}</span> test!</div>
      <div class="h2 mt-4 mb-3 text-uppercase">
        <span v-if="quizResult.gradedRes.passed" class="text-success"><i class="fas fa-check-double"></i> Passed</span>
        <span v-else class="text-danger"><i class="far fa-times-circle"></i> Failed</span>
      </div>
      <b-card-group deck>
        <b-card bg-variant="light" class="text-center">
          <b-card-text>
            <div class="h3">
              {{ quizResult.percentCorrect }}%
            </div>
            <div class="text-secondary mt-2">
              <b>100%</b> is required to pass
            </div>
          </b-card-text>
        </b-card>

        <b-card bg-variant="light" class="text-center">
          <b-card-text>
            <div class="h3">
              <b-badge variant="success">{{ quizResult.numCorrect }}</b-badge> out of <b-badge>{{ quizResult.numTotal }}</b-badge>
            </div>
            <div class="text-secondary mt-2">
              <span v-if="quizResult.missedBy > 0">Missed by <b-badge variant="warning">{{ quizResult.missedBy }}</b-badge> questions</span>
              <span v-else>Well done!</span>
            </div>
          </b-card-text>
        </b-card>
      </b-card-group>

      <div v-if="!quizResult.gradedRes.passed" class="mt-3">
        <span class="text-info">No worries!</span> Would you like to try again? <b-button variant="outline-primary" size="sm" @click="tryAgain"><i class="fas fa-redo"></i> Try Again</b-button>
      </div>
    </b-card>

    <b-card body-class="pl-5" class="mb-4">
      <div v-for="(q, index) in quizInfo.questions" :key="q.id">
        <quiz-run-question :q="q" :num="index" @selected-answer="updateSelectedAnswers"/>
      </div>

      <div v-if="notEveryQuestionHasAnAnswer" class="alert alert-danger text-center">
        <i class="fas fa-exclamation-triangle"></i> Not every question has an answer!
      </div>
      <div v-if="!quizResult" class="text-center">
        <b-button variant="success" @click="completeTestRun"><i class="fas fa-check-double"></i> Done</b-button>
      </div>
    </b-card>

  </div>
</div>
</template>

<script>
  import QuizRunService from '@/common-components/quiz/QuizRunService';
  import SkillsSpinner from '@/common-components/utilities/SkillsSpinner';
  import QuizRunQuestion from '@/common-components/quiz/QuizRunQuestion';

  export default {
    name: 'QuizRun',
    components: { SkillsSpinner, QuizRunQuestion },
    data() {
      return {
        isLoading: true,
        quizId: this.$route.params.quizId,
        quizInfo: null,
        selectedAnswers: [],
        notEveryQuestionHasAnAnswer: false,
        quizResult: null,
        quizAttemptId: null,
        reportAnswerPromises: [],
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        this.isLoading = true;
        QuizRunService.getQuizInfo(this.quizId)
          .then((res) => {
            const copy = ({ ...res });
            copy.questions = res.questions.map((q) => {
              const answerOptions = q.answerOptions.map((a) => ({
                ...a,
                selected: false,
              }));
              return ({ ...q, answerOptions });
            });
            this.quizInfo = copy;

            QuizRunService.startQuizAttempt(this.quizId)
              .then((startQuizAttemptRes) => {
                this.quizAttemptId = startQuizAttemptRes.id;
              }).finally(() => {
                this.isLoading = false;
              });
          });
      },
      updateSelectedAnswers(questionSelectedAnswer) {
        const res = this.selectedAnswers.filter((q) => q.questionId !== questionSelectedAnswer.questionId);
        if (questionSelectedAnswer && questionSelectedAnswer.selectedAnswerIds && questionSelectedAnswer.selectedAnswerIds.length > 0) {
          res.push(questionSelectedAnswer);
          this.notEveryQuestionHasAnAnswer = false;
        }
        this.selectedAnswers = res;
        this.reportAnswerPromises.push(QuizRunService.reportAnswer(this.quizId, this.quizAttemptId, questionSelectedAnswer.changedAnswerId));
      },
      completeTestRun() {
        const everyQuestionHasAnswer = this.selectedAnswers.length === this.quizInfo.questions.length;
        if (!everyQuestionHasAnswer) {
          this.notEveryQuestionHasAnAnswer = true;
        } else {
          this.isLoading = true;
          Promise.all(this.reportAnswerPromises)
            .then(() => {
              QuizRunService.completeQuizAttempt(this.quizId, this.quizAttemptId)
                .then((gradedRes) => {
                  const numCorrect = gradedRes.gradedQuestions.filter((q) => q.isCorrect).length;
                  const numTotal = gradedRes.gradedQuestions.length;
                  const percentCorrect = Math.trunc(((numCorrect * 100) / numTotal));
                  this.quizResult = {
                    gradedRes,
                    numCorrect,
                    numTotal,
                    percentCorrect,
                    missedBy: numTotal - numCorrect,
                  };

                  const updatedQuizInfo = ({ ...this.quizInfo });
                  updatedQuizInfo.questions = updatedQuizInfo.questions.map((q) => {
                    const gradedQuestion = gradedRes.gradedQuestions.find((gradedQ) => gradedQ.questionId === q.id);

                    const answerOptions = q.answerOptions.map((a) => ({
                          ...a,
                          selected: gradedQuestion.selectedAnswerIds.includes(a.id),
                          isGraded: true,
                          isCorrect: gradedQuestion.correctAnswerIds.includes(a.id),
                    }));
                    return ({ ...q, gradedInfo: gradedQuestion, answerOptions });
                  });
                  this.quizInfo = updatedQuizInfo;
                }).finally(() => {
                  this.isLoading = false;
                });
            });
        }
      },
      tryAgain() {
        this.quizResult = null;
        this.loadData();
      },
    },
  };
</script>

<style scoped>

</style>
