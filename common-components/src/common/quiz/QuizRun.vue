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
<div class="container-fluid pb-4">
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

    <b-card v-if="splashScreen.show" body-class="pl-4">
      <div class="h5">
        <slot name="splashPageTitle">
          You are about to begin the test!
        </slot>
        <div class="mb-1 mt-4 quiz-name" style="font-size: 1.7rem">
          <span class="font-weight-bold text-success">{{ quizInfo.name }}</span>
        </div>
      </div>

      <div class="row">
<!--        <div class="col"></div>-->
        <div class="col-auto">
          <b-card class="text-center" body-class="pt-2 pb-1">
            <i class="fas fa-business-time text-info" style="font-size: 1.3rem;"></i>
            <span class="text-secondary font-italic ml-1">Time Limit:</span>
            <span class="text-uppercase ml-1 font-weight-bold">None</span>
          </b-card>
        </div>
        <div class="col-auto">
          <b-card class="text-center" body-class="pt-2 pb-1">
            <i class="fas fa-redo-alt text-info" style="font-size: 1.3rem;"></i>
            <span class="text-secondary font-italic ml-1">Attempts:</span>
            <span class="text-uppercase ml-1 font-weight-bold">Unlimited</span>
          </b-card>
        </div>
        <div class="col"></div>
      </div>

      <p v-if="quizInfo.description" class="mt-3">
        <markdown-text :text="quizInfo.description" />
      </p>

      <div class="mt-4 text-center">
        <b-button variant="outline-danger" @click="cancelQuizAttempt" class="text-uppercase mr-2"><i class="fas fas fa-times-circle"> Cancel</i></b-button>
        <b-button variant="outline-success" @click="startQuizAttempt" class="text-uppercase"><i class="fas fa-play-circle"> Start</i></b-button>
      </div>
    </b-card>

    <b-card v-if="quizResult && !splashScreen.show" class="mb-3" body-class="text-center">
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
        OR <b-button variant="outline-success"  size="sm" @click="doneWithThisRun" class="text-uppercase font-weight-bold"><i class="fas fa-times-circle"></i> Close</b-button>
      </div>

      <div v-if="quizResult.gradedRes.passed" class="text-center mt-4">
        <b-button variant="outline-success" @click="doneWithThisRun" class="text-uppercase font-weight-bold"><i class="fas fa-times-circle"></i> Close</b-button>
      </div>
    </b-card>

    <b-card v-if="!splashScreen.show" body-class="pl-5" class="mb-4">
      <div v-for="(q, index) in quizInfo.questions" :key="q.id">
        <quiz-run-question :q="q" :num="index" @selected-answer="updateSelectedAnswers"/>
      </div>

      <div v-if="notEveryQuestionHasAnAnswer" class="alert alert-danger text-center">
        <i class="fas fa-exclamation-triangle"></i> Not every question has an answer!
      </div>
      <div v-if="!quizResult" class="text-center">
        <b-button variant="outline-danger" @click="cancelQuizAttempt" class="text-uppercase mr-2"><i class="fas fas fa-times-circle"> Cancel</i></b-button>
        <b-button variant="outline-success" @click="completeTestRun" class="text-uppercase font-weight-bold"><i class="fas fa-check-double"></i> Done</b-button>
      </div>
      <div v-if="quizResult" class="text-center">
        <b-button variant="outline-success" @click="doneWithThisRun" class="text-uppercase font-weight-bold"><i class="fas fa-times-circle"></i> Close</b-button>
      </div>
    </b-card>

  </div>
</div>
</template>

<script>
  import QuizRunService from '@/common-components/quiz/QuizRunService';
  import SkillsSpinner from '@/common-components/utilities/SkillsSpinner';
  import QuizRunQuestion from '@/common-components/quiz/QuizRunQuestion';
  import MarkdownText from '@/common-components/utilities/MarkdownText';

  export default {
    name: 'QuizRun',
    components: { MarkdownText, SkillsSpinner, QuizRunQuestion },
    props: {
      quizId: String,
    },
    data() {
      return {
        isLoading: true,
        quizInfo: null,
        questionsWithAnswersSelected: [],
        notEveryQuestionHasAnAnswer: false,
        quizResult: null,
        quizAttemptId: null,
        reportAnswerPromises: [],
        splashScreen: {
          show: false,
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        this.isLoading = true;
        QuizRunService.getQuizInfo(this.quizId)
          .then((quizInfo) => {
            this.quizInfo = quizInfo;
            if (quizInfo.isAttemptAlreadyInProgress) {
              this.startQuizAttempt();
            } else {
              this.splashScreen.show = true;
              this.isLoading = false;
            }
          });
      },
      startQuizAttempt() {
        this.isLoading = true;
        this.splashScreen.show = false;
        QuizRunService.startQuizAttempt(this.quizId)
          .then((startQuizAttemptRes) => {
            this.quizAttemptId = startQuizAttemptRes.id;
            const { selectedAnswerIds } = startQuizAttemptRes;
            const copy = ({ ...this.quizInfo });
            copy.questions = this.quizInfo.questions.map((q) => {
              const answerOptions = q.answerOptions.map((a) => ({
                  ...a,
                  selected: !!(selectedAnswerIds && selectedAnswerIds.includes(a.id)),
              }));
              return ({ ...q, answerOptions });
            });
            this.quizInfo = copy;
            this.questionsWithAnswersSelected = copy.questions.filter((q) => q.answerOptions.map((a) => a.selected).indexOf(true) >= 0);
          }).finally(() => {
            this.isLoading = false;
          });
      },
      updateSelectedAnswers(questionSelectedAnswer) {
        const res = this.questionsWithAnswersSelected.filter((q) => q.questionId !== questionSelectedAnswer.questionId);
        if (questionSelectedAnswer && questionSelectedAnswer.selectedAnswerIds && questionSelectedAnswer.selectedAnswerIds.length > 0) {
          res.push(questionSelectedAnswer);
          this.notEveryQuestionHasAnAnswer = false;
        }
        this.questionsWithAnswersSelected = res;
        this.reportAnswerPromises.push(QuizRunService.reportAnswer(this.quizId, this.quizAttemptId, questionSelectedAnswer.changedAnswerId, questionSelectedAnswer.changedAnswerIdSelected));
      },
      completeTestRun() {
        const everyQuestionHasAnswer = this.questionsWithAnswersSelected.length === this.quizInfo.questions.length;
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
      cancelQuizAttempt() {
        this.$emit('cancelled');
      },
      doneWithThisRun() {
        this.$emit('testWasTaken', this.quizResult);
      },
    },
  };
</script>

<style scoped>
.quiz-name {
  animation: zoomIn; /* referring directly to the animation's @keyframe declaration */
  animation-duration: 1s; /* don't forget to set a duration! */
  animate-delay: 1.7s;
}
</style>
