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
  <skills-spinner :is-loading="isLoading" class="mt-3"/>
  <div v-if="!isLoading">
    <quiz-run-splash-screen v-if="splashScreen.show" :quiz-info="quizInfo" @cancel="cancelQuizAttempt" @start="startQuizAttempt">
      <template slot="aboveTitle">
        <slot name="splashPageTitle">
          <span v-if="isSurveyType">Thank you for taking time to take this survey! </span>
          <span v-else>You are about to begin the quiz!</span>
        </slot>
      </template>
    </quiz-run-splash-screen>

    <survey-run-completion-summary
        v-if="isSurveyType && quizResult && !splashScreen.show"
        class="mb-3"
        :quiz-info="quizInfo"
        :quiz-result="quizResult"
        @close="doneWithThisRun">
      <template slot="completeAboveTitle">
        <slot name="completeAboveTitle">
          <i class="fas fa-handshake text-info"></i> Thank you for taking the time to complete the survey!
        </slot>
      </template>
    </survey-run-completion-summary>

    <quiz-run-completion-summary
        v-if="!isSurveyType && quizResult && !splashScreen.show"
        class="mb-3"
        :quiz-info="quizInfo"
        :quiz-result="quizResult"
        @close="doneWithThisRun"
        @run-again="tryAgain">
      <template slot="completeAboveTitle">
        <slot name="completeAboveTitle">
          <span v-if="isSurveyType">Thank you for taking time to take this survey! </span>
          <span v-else>You are about to begin the quiz!</span>
        </slot>
      </template>
    </quiz-run-completion-summary>

    <b-card v-if="!splashScreen.show && !(isSurveyType && quizResult)" class="mb-4" data-cy="quizRunQuestions">
      <div class="row bg-white border-bottom py-2 mb-3" data-cy="subPageHeader">
        <div class="col">
          <div class="h4 text-success font-weight-bold" data-cy="quizName">{{ quizInfo.name }}</div>
        </div>
        <div class="col-auto text-right text-muted">
          <b-badge variant="success" data-cy="numQuestions">{{quizInfo.questions.length}}</b-badge> <span class="text-uppercase">questions</span>
        </div>
      </div>

      <b-overlay :show="isCompleting" opacity="0.2">
        <div v-for="(q, index) in quizInfo.questions" :key="q.id">
          <quiz-run-question
              :q="q"
              :text-input-err-msg="q.textInputErrMsg"
              :num="index+1"
              :enable-missing-indicator="notEveryQuestionHasAnAnswer || (q.textInputErrMsg && q.textInputErrMsg.length > 0)"
              @selected-answer="updateSelectedAnswers"
              @answer-text-changed="updateSelectedAnswers"/>
        </div>
      </b-overlay>

      <div v-if="notEveryQuestionHasAnAnswer || someQuestionsHaveErrors" class="alert alert-danger text-center" data-cy="issuesWithAnswer">
        <i class="fas fa-exclamation-triangle mr-2"></i>
        <span v-if="notEveryQuestionHasAnAnswer || !someQuestionsHaveErrors">Missing answers!</span>
        <span v-if="someQuestionsHaveErrors">There are still validation errors. Please revisit the {{ quizInfo.quizType }}!</span>
      </div>
      <div v-if="!quizResult" class="text-left mt-5">
        <b-button variant="outline-info" @click="saveAndCloseThisRun" class="text-uppercase mr-2 font-weight-bold"
                  :disabled="isCompleting"
                  data-cy="saveAndCloseQuizAttemptBtn">
          <i class="fas fa-save" aria-hidden="true"> Save and Close</i>
        </b-button>
        <b-overlay :show="isCompleting" rounded opacity="0.6" spinner-small class="d-inline-block">
          <b-button variant="outline-success"
                    @click="completeTestRun"
                    :disabled="isCompleting"
                    class="text-uppercase font-weight-bold"
                    data-cy="completeQuizBtn">
            <i class="fas fa-check-double" aria-hidden="true"></i> Done
          </b-button>
        </b-overlay>
      </div>

      <div v-if="quizResult && quizResult.gradedRes && quizResult.gradedRes.passed" class="text-left mt-5">
        <b-button variant="outline-success" @click="doneWithThisRun" class="text-uppercase font-weight-bold"><i class="fas fa-times-circle"></i> Close</b-button>
      </div>
      <div v-if="quizResult && quizResult.gradedRes && !quizResult.gradedRes.passed" class="mt-5">
        <div class="my-2"><span class="text-info">No worries!</span> Would you like to try again?</div>
        <b-button variant="outline-danger"  @click="doneWithThisRun" class="text-uppercase font-weight-bold mr-2"><i class="fas fa-times-circle"></i> Close</b-button>
        <b-button variant="outline-success" @click="tryAgain" class="text-uppercase font-weight-bold"><i class="fas fa-redo"></i> Try Again</b-button>
      </div>
    </b-card>

  </div>
</div>
</template>

<script>
  import QuizRunService from '@/common-components/quiz/QuizRunService';
  import SkillsSpinner from '@/common-components/utilities/SkillsSpinner';
  import QuizRunQuestion from '@/common-components/quiz/QuizRunQuestion';
  import QuizRunSplashScreen from '@/common-components/quiz/QuizRunSplashScreen';
  import QuizRunCompletionSummary from '@/common-components/quiz/QuizRunCompletionSummary';
  import SurveyRunCompletionSummary from '@/common-components/quiz/SurveyRunCompletionSummary';

  export default {
    name: 'QuizRun',
    components: {
      SurveyRunCompletionSummary,
      QuizRunCompletionSummary,
      QuizRunSplashScreen,
      SkillsSpinner,
      QuizRunQuestion,
    },
    props: {
      quizId: String,
      quiz: {
        type: Object,
        default: null,
      },
    },
    data() {
      return {
        isLoading: true,
        isCompleting: false,
        quizInfo: null,
        questionsWithAnswersSelected: [],
        notEveryQuestionHasAnAnswer: false,
        someQuestionsHaveErrors: false,
        quizResult: null,
        quizAttemptId: null,
        reportAnswerPromises: [],
        splashScreen: {
          show: false,
        },
      };
    },
    mounted() {
      if (this.quiz) {
        this.setQuizInfo(({ ...this.quiz }));
      } else {
        this.loadData();
      }
    },
    computed: {
      isSurveyType() {
        return this.quizInfo.quizType === 'Survey';
      },
    },
    methods: {
      loadData() {
        this.isLoading = true;
        QuizRunService.getQuizInfo(this.quizId)
          .then((quizInfo) => {
            this.setQuizInfo(quizInfo);
          });
      },
      setQuizInfo(quizInfo) {
        const percentToPass = quizInfo.minNumQuestionsToPass <= 0 ? 100 : Math.trunc(((quizInfo.minNumQuestionsToPass * 100) / quizInfo.questions.length));
        this.quizInfo = { ...quizInfo, percentToPass };
        if (quizInfo.isAttemptAlreadyInProgress) {
          this.startQuizAttempt();
        } else {
          this.splashScreen.show = true;
          this.isLoading = false;
        }
      },
      startQuizAttempt() {
        this.isLoading = true;
        this.splashScreen.show = false;
        QuizRunService.startQuizAttempt(this.quizId)
          .then((startQuizAttemptRes) => {
            this.quizAttemptId = startQuizAttemptRes.id;
            const { selectedAnswerIds, enteredText } = startQuizAttemptRes;
            const copy = ({ ...this.quizInfo });
            copy.questions = this.quizInfo.questions.map((q) => {
              const answerOptions = q.answerOptions.map((a) => ({
                  ...a,
                  selected: !!(selectedAnswerIds && selectedAnswerIds.includes(a.id)),
              }));
              if (enteredText && q.questionType === 'TextInput') {
                const answerId = q.answerOptions[0].id;
                const enteredTextObj = enteredText.find((t) => t.answerId === answerId);
                if (enteredTextObj) {
                  // eslint-disable-next-line no-param-reassign
                  answerOptions[0].answerText = enteredTextObj.answerText;
                }
              }
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
        const isTextInput = questionSelectedAnswer.questionType === 'TextInput';
        const questionHasASelection = questionSelectedAnswer && questionSelectedAnswer.selectedAnswerIds && questionSelectedAnswer.selectedAnswerIds.length > 0
          && (!isTextInput || questionSelectedAnswer.changedAnswerIdSelected === true);

        this.notEveryQuestionHasAnAnswer = false;
        this.someQuestionsHaveErrors = false;

        if (questionHasASelection) {
          res.push(questionSelectedAnswer);
        }

        this.questionsWithAnswersSelected = res;
        if (isTextInput) {
          if (questionSelectedAnswer.changedAnswerIdSelected && questionSelectedAnswer.answerText) {
            this.validateQuestionAnswerText(questionSelectedAnswer.questionId, questionSelectedAnswer.answerText)
              .then((validationRes) => {
                if (validationRes.valid) {
                  this.reportAnswer(questionSelectedAnswer);
                }
              });
          } else {
            this.reportAnswer(questionSelectedAnswer);
            // clear error if needed
            const questionWithErrIndex = this.quizInfo.questions.findIndex((q) => q.id === questionSelectedAnswer.questionId);
            if (this.quizInfo.questions[questionWithErrIndex].textInputErrMsg) {
              this.quizInfo.questions[questionWithErrIndex].textInputErrMsg = null;
              this.quizInfo.questions = this.quizInfo.questions.map((q) => q);
            }
          }
        } else {
          this.reportAnswer(questionSelectedAnswer);
        }
      },
      validateQuestionAnswerText(questionId, answerText) {
        return QuizRunService.validateDescription(answerText)
          .then((validationRes) => {
            const questionWithErrIndex = this.quizInfo.questions.findIndex((q) => q.id === questionId);
            this.quizInfo.questions[questionWithErrIndex].textInputErrMsg = !validationRes.valid ? validationRes.msg : null;
            this.quizInfo.questions = this.quizInfo.questions.map((q) => q);
            return validationRes;
          });
      },
      reportAnswer(questionSelectedAnswer) {
        this.reportAnswerPromises.push(QuizRunService.reportAnswer(this.quizId, this.quizAttemptId, questionSelectedAnswer.changedAnswerId, questionSelectedAnswer.changedAnswerIdSelected, questionSelectedAnswer.answerText));
      },
      completeTestRun() {
        this.isCompleting = true;
        const questionsWithText = this.questionsWithAnswersSelected.filter((q) => q.answerText && q.answerText.trim().length > 0);
        const validationPromises = [];
        questionsWithText.forEach((q) => {
          validationPromises.push(this.validateQuestionAnswerText(q.questionId, q.answerText));
        });
        Promise.all(validationPromises).then((validationResults) => {
          const foundIndex = validationResults.findIndex((validationResult) => validationResult.valid === false);
          if (validationResults && validationResults.length > 0 && foundIndex >= 0) {
            this.someQuestionsHaveErrors = true;
            this.isCompleting = false;
          } else {
            Promise.all(this.reportAnswerPromises)
              .then(() => {
                const everyQuestionHasAnswer = this.questionsWithAnswersSelected.length === this.quizInfo.questions.length;
                const someQuestionsHaveIssues = this.quizInfo.questions.find((q) => q.textInputErrMsg);
                if (!everyQuestionHasAnswer || someQuestionsHaveIssues) {
                  this.notEveryQuestionHasAnAnswer = !everyQuestionHasAnswer;
                  this.someQuestionsHaveErrors = someQuestionsHaveIssues;
                  this.isCompleting = false;
                } else {
                  this.reportTestRunToBackend()
                    .finally(() => {
                      this.isCompleting = false;
                    });
                }
              });
          }
        });
      },
      reportTestRunToBackend() {
        return QuizRunService.completeQuizAttempt(this.quizId, this.quizAttemptId)
          .then((gradedRes) => {
            const numCorrect = gradedRes.gradedQuestions.filter((q) => q.isCorrect).length;
            const numTotal = gradedRes.gradedQuestions.length;
            const numQuestionsToPass = this.quizInfo.minNumQuestionsToPass > 0 ? this.quizInfo.minNumQuestionsToPass : numTotal;
            const percentCorrect = Math.trunc(((numCorrect * 100) / numTotal));
            this.quizResult = {
              gradedRes,
              numCorrect,
              numTotal,
              percentCorrect,
              missedBy: numQuestionsToPass - numCorrect,
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
          });
      },
      tryAgain() {
        this.quizResult = null;
        this.loadData();
      },
      cancelQuizAttempt() {
        this.$emit('cancelled');
      },
      saveAndCloseThisRun() {
        this.isCompleting = true;
        Promise.all(this.reportAnswerPromises)
          .then(() => {
            this.$emit('cancelled');
            this.isCompleting = false;
          });
      },
      doneWithThisRun() {
        this.$emit('testWasTaken', this.quizResult);
      },
    },
  };
</script>

<style scoped>

</style>
