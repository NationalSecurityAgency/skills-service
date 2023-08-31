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
        ref="surveyRunCompletionSummary"
        v-if="isSurveyType && quizResult && !splashScreen.show"
        class="mb-3"
        :quiz-info="quizInfo"
        :quiz-result="quizResult"
        @close="doneWithThisRun">
      <template slot="completeAboveTitle">
        <slot name="completeAboveTitle">
          <i class="fas fa-handshake text-info" aria-hidden="true"></i> Thank you for taking the time to complete the survey!
        </slot>
      </template>
    </survey-run-completion-summary>

    <quiz-run-completion-summary
        id="quizRunCompletionSummary"
        ref="quizRunCompletionSummary"
        v-if="!isSurveyType && quizResult && !splashScreen.show"
        class="mb-3"
        :quiz-info="quizInfo"
        :quiz-result="quizResult"
        @close="doneWithThisRun"
        @run-again="tryAgain">
      <template slot="completeAboveTitle">
        <slot name="completeAboveTitle">
          <span v-if="isSurveyType">Thank you for taking time to take this survey! </span>
          <span v-else>Thank you for completing the Quiz!</span>
        </slot>
      </template>
    </quiz-run-completion-summary>

    <b-card v-if="!splashScreen.show && !(isSurveyType && quizResult) && showQuestions" class="mb-4" data-cy="quizRunQuestions">
      <div class="row border-bottom py-2 mb-3" data-cy="subPageHeader">
        <div class="col">
          <div class="h4 text-success font-weight-bold skills-page-title-text-color" data-cy="quizName">{{ quizInfo.name }}</div>
        </div>
        <div class="col-auto text-right text-muted">
          <b-badge variant="success" data-cy="numQuestions">{{quizInfo.quizLength}}</b-badge> <span class="text-uppercase">questions</span>
          <span v-if="quizInfo.quizTimeLimit > 0 && dateTimer !== null"> | {{currentDate | duration(quizInfo.deadline, false, true)}}</span>
        </div>
      </div>

      <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit }" slim>
      <b-overlay :show="isCompleting" opacity="0.2">
        <div v-for="(q, index) in quizInfo.questions" :key="q.id">
          <quiz-run-question
              :q="q"
              :quiz-id="quizId"
              :quiz-attempt-id="quizAttemptId"
              :num="index+1"
              @selected-answer="updateSelectedAnswers"
              @answer-text-changed="updateSelectedAnswers"/>
        </div>
      </b-overlay>

        <quiz-run-validation-warnings v-if="invalid" :errors-to-show="errorsToShow" />
      <div v-if="!quizResult" class="text-left mt-5">
        <b-button variant="outline-info" @click="saveAndCloseThisRun"
                  class="text-uppercase mr-2 font-weight-bold skills-theme-btn"
                  :disabled="isCompleting"
                  :aria-label="`Save and close this ${quizInfo.quizType}`"
                  data-cy="saveAndCloseQuizAttemptBtn">
          <i class="fas fa-save" aria-hidden="true"> Save and Close</i>
        </b-button>
        <b-overlay :show="isCompleting" rounded opacity="0.6" spinner-small class="d-inline-block">
          <b-button variant="outline-success"
                    @click="handleSubmit(completeTestRun)"
                    :disabled="isCompleting"
                    :aria-label="`Done with ${quizInfo.quizType}`"
                    class="text-uppercase font-weight-bold skills-theme-btn"
                    data-cy="completeQuizBtn">
            <i class="fas fa-check-double" aria-hidden="true"></i> Done
          </b-button>
        </b-overlay>
      </div>
      </ValidationObserver>

      <div v-if="quizResult && quizResult.gradedRes && quizResult.gradedRes.passed" class="text-left mt-5">
        <b-button variant="outline-success" @click="doneWithThisRun" class="text-uppercase font-weight-bold skills-theme-btn"><i class="fas fa-times-circle" aria-hidden="true"></i> Close</b-button>
      </div>
      <div v-if="quizResult && quizResult.gradedRes && !quizResult.gradedRes.passed" class="mt-5">
        <div class="my-2" v-if="(quizInfo.maxAttemptsAllowed - quizInfo.userNumPreviousQuizAttempts - 1) > 0"><span class="text-info">No worries!</span> Would you like to try again?</div>
        <b-button variant="outline-danger"  @click="doneWithThisRun" class="text-uppercase font-weight-bold mr-2 skills-theme-btn" data-cy="closeQuizBtn"><i class="fas fa-times-circle" aria-hidden="true"></i> Close</b-button>
        <b-button v-if="(quizInfo.maxAttemptsAllowed - quizInfo.userNumPreviousQuizAttempts - 1) > 0" variant="outline-success" @click="tryAgain" class="text-uppercase font-weight-bold skills-theme-btn" data-cy="runQuizAgainBtn"><i class="fas fa-redo" aria-hidden="true"></i> Try Again</b-button>
      </div>
    </b-card>

    <div v-if="scrollDistance > 300" id="floating-timer">
      <div v-if="quizInfo.quizTimeLimit > 0 && dateTimer !== null">{{currentDate | duration(quizInfo.deadline, false, true)}}</div>
    </div>
  </div>
</div>
</template>

<script>
  import dayjs from 'dayjs';
  import QuizRunService from '@/common-components/quiz/QuizRunService';
  import SkillsSpinner from '@/common-components/utilities/SkillsSpinner';
  import QuizRunQuestion from '@/common-components/quiz/QuizRunQuestion';
  import QuizRunSplashScreen from '@/common-components/quiz/QuizRunSplashScreen';
  import QuizRunCompletionSummary from '@/common-components/quiz/QuizRunCompletionSummary';
  import SurveyRunCompletionSummary from '@/common-components/quiz/SurveyRunCompletionSummary';
  import QuizRunValidationWarnings from '@/common-components/quiz/QuizRunValidationWarnings';
  import QuestionType from '@/common-components/quiz/QuestionType';

  export default {
    name: 'QuizRun',
    components: {
      QuizRunValidationWarnings,
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
        quizResult: null,
        quizAttemptId: null,
        reportAnswerPromises: [],
        splashScreen: {
          show: false,
        },
        currentDate: null,
        dateTimer: null,
        scrollDistance: 0,
      };
    },
    mounted() {
      if (this.quiz) {
        this.setQuizInfo(({ ...this.quiz }));
      } else {
        this.loadData();
      }
    },
    beforeDestroy() {
      this.destroyDateTimer();
    },
    computed: {
      isSurveyType() {
        return this.quizInfo.quizType === 'Survey';
      },
      errorsToShow() {
        const values = Object.values(this.$refs.observer.errors).flat().filter((val) => val && val.length > 0);
        const unique = values.filter((v, i, a) => a.indexOf(v) === i);
        return unique && unique.length > 0 ? unique : null;
      },
      showQuestions() {
        return !this.quizResult
          || (this.quizResult && this.quizResult.gradedRes && this.quizResult.gradedRes.gradedQuestions && this.quizResult.gradedRes.gradedQuestions.length > 0);
      },
    },
    methods: {
      handleScroll() {
        this.scrollDistance = window.scrollY;
      },
      beginDateTimer() {
        if (this.quizInfo.deadline) {
          window.addEventListener('scroll', this.handleScroll);
          this.currentDate = dayjs().utc().valueOf();
          this.dateTimer = setInterval(() => {
            this.currentDate = dayjs().utc().valueOf();
            if (this.currentDate >= dayjs(this.quizInfo.deadline).utc().valueOf()) {
              this.destroyDateTimer();
              QuizRunService.failQuizAttempt(this.quizId, this.quizAttemptId).then((gradedRes) => {
                const numTotal = this.quizInfo.questions.length;
                const numCorrect = 0;
                const percentCorrect = Math.trunc(((numCorrect * 100) / numTotal));
                this.quizResult = {
                  gradedRes,
                  numCorrect,
                  numTotal,
                  percentCorrect,
                  missedBy: numTotal,
                  outOfTime: true,
                };
              });
            }
          }, 1000);
        }
      },
      destroyDateTimer() {
        clearInterval(this.dateTimer);
        this.dateTimer = null;
        window.removeEventListener('scroll', this.handleScroll);
      },
      loadData() {
        this.isLoading = true;
        QuizRunService.getQuizInfo(this.quizId)
          .then((quizInfo) => {
            this.setQuizInfo(quizInfo);
          });
      },
      setQuizInfo(quizInfo) {
        const percentToPass = quizInfo.minNumQuestionsToPass <= 0 ? 100 : Math.trunc(((quizInfo.minNumQuestionsToPass * 100) / quizInfo.quizLength));
        this.quizInfo = { ...quizInfo, percentToPass };
        if (quizInfo.isAttemptAlreadyInProgress) {
          this.startQuizAttempt();
        } else {
          this.splashScreen.show = true;
          this.isLoading = false;
        }
      },
      failQuizAttempt() {
        this.destroyDateTimer();
        this.quizResult = {
          gradedRes: {
            associatedSkillResults: [],
            completed: null,
            gradedQuestion: [],
            numberQuestionsGotWrong: 0,
            passed: false,
            started: null,
          },
          missedBy: this.quizInfo.questions.length,
          numCorrect: 0,
          numTotal: this.quizInfo.questions.length,
          percentCorrect: 0,
          outOfTime: true,
        };
      },
      startQuizAttempt() {
        this.isLoading = true;
        this.splashScreen.show = false;

        QuizRunService.startQuizAttempt(this.quizId)
          .then((startQuizAttemptRes) => {
            if (startQuizAttemptRes.existingAttemptFailed) {
              this.failQuizAttempt();
              return;
            }
            this.quizAttemptId = startQuizAttemptRes.id;
            const {
              selectedAnswerIds, enteredText, questions, deadline,
            } = startQuizAttemptRes;
            this.quizInfo.deadline = deadline;
            this.quizInfo.questions = questions;
            const copy = ({ ...this.quizInfo });
            copy.questions = this.quizInfo.questions.map((q) => {
              const answerOptions = q.answerOptions.map((a) => ({
                ...a,
                selected: !!(selectedAnswerIds && selectedAnswerIds.includes(a.id)),
              }));
              if (enteredText && q.questionType === QuestionType.TextInput) {
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
            this.beginDateTimer();
          }).finally(() => {
            this.isLoading = false;
          });
      },
      updateSelectedAnswers(questionSelectedAnswer) {
        if (questionSelectedAnswer.reportAnswerPromise) {
          this.reportAnswerPromises.push(questionSelectedAnswer.reportAnswerPromise);
        }
      },
      completeTestRun() {
        this.isCompleting = true;
        this.$refs.observer.validate().then((validationResults) => {
          if (validationResults) {
            Promise.all(this.reportAnswerPromises)
              .then(() => {
                this.reportTestRunToBackend()
                  .finally(() => {
                    this.destroyDateTimer();
                    this.isCompleting = false;
                    if (!this.isSurveyType) {
                      this.$nextTick(() => {
                        const element = document.getElementById('quizRunCompletionSummary');
                        element.scrollIntoView({ behavior: 'smooth' });
                      });
                    }
                    let announceMsg = `Completed ${this.quizInfo.quizType}`;
                    if (!this.isSurveyType) {
                      announceMsg = `${announceMsg}. ${!this.quizResult.gradedRes.passed ? 'Failed' : 'Successfully passed'} quiz.`;
                    }
                    this.$nextTick(() => this.$announcer.polite(announceMsg));
                  });
              });
          } else {
            this.isCompleting = false;
          }
        });
      },
      reportTestRunToBackend() {
        return QuizRunService.completeQuizAttempt(this.quizId, this.quizAttemptId)
          .then((gradedRes) => {
            const numTotal = this.quizInfo.questions.length;
            const numCorrect = numTotal - gradedRes.numQuestionsGotWrong;
            const percentCorrect = Math.trunc(((numCorrect * 100) / numTotal));
            this.quizResult = {
              gradedRes,
              numCorrect,
              numTotal,
              percentCorrect,
              missedBy: gradedRes.numQuestionsGotWrong,
            };

            if (gradedRes.gradedQuestions && gradedRes.gradedQuestions.length > 0) {
              const updatedQuizInfo = ({ ...this.quizInfo });
              updatedQuizInfo.questions = updatedQuizInfo.questions.map((q) => {
                const gradedQuestion = gradedRes.gradedQuestions.find((gradedQ) => gradedQ.questionId === q.id);

                const answerOptions = q.answerOptions.map((a) => ({
                  ...a,
                  selected: gradedQuestion.selectedAnswerIds.includes(a.id),
                  isGraded: true,
                  isCorrect: gradedQuestion.correctAnswerIds.includes(a.id),
                }));
                return ({
                  ...q,
                  gradedInfo: gradedQuestion,
                  answerOptions,
                });
              });
              this.quizInfo = updatedQuizInfo;
            }
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
#floating-timer {
  position: fixed;
  top: 30px;
  right: 45px;
  z-index: 10;
  padding: 5px;
  border: 1px solid #000;
}
</style>
