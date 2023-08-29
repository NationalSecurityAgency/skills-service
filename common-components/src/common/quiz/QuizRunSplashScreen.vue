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
  <b-card data-cy="quizSplashScreen">
    <div class="h5">
      <div v-if="quizInfo.userQuizPassed" class="alert alert-success">
        <i class="fas fa-gift" aria-hidden="true"></i> Good News! You already <span v-if="!isSurveyType">passed this quiz</span><span v-else>completed this survey</span> <span class="font-weight-bold">{{ quizInfo.userLastQuizAttemptDate | timeFromNow }}</span>!
        <b-button variant="outline-primary" @click="cancel" class="text-uppercase" size="sm" data-cy="closeQuizAttemptInAlert"><i class="fas fas fa-times-circle" aria-hidden="true"> Close {{ quizInfo.quizType }}</i></b-button>
      </div>
      <slot v-if="!quizInfo.userQuizPassed" name="aboveTitle" />
      <div class="mb-1 mt-4 h2">
        <span class="font-weight-bold text-success skills-page-title-text-color">{{ quizInfo.name }}</span>
      </div>
    </div>

    <b-card v-if="!isSurveyType && canStartQuiz" class="mb-1 skills-card-theme-border" body-class="h5" data-cy="quizPassInfo">
      <i class="fas fa-check-circle text-success" aria-hidden="true"></i>
      Must get <b-badge variant="success">{{ minNumQuestionsToPass }}</b-badge> / <b-badge>{{ numQuestions }}</b-badge> questions <span class="text-secondary font-italic">({{ quizInfo.percentToPass }}%)</span> to <span class="text-success text-uppercase">pass</span>. Good Luck!
    </b-card>

    <div class="row">
      <div class="col-sm pt-2">
        <b-card class="skills-card-theme-border" body-class="pt-2 pb-1" data-cy="quizInfoCard">
          <i class="fas fa-question-circle text-info" style="font-size: 1.3rem;" aria-hidden="true"></i>
          <span class="text-secondary font-italic ml-1">Questions:</span>
          <span class="text-uppercase ml-1 font-weight-bold" data-cy="numQuestions">{{ numQuestions }}</span>
        </b-card>
      </div>
      <div v-if="!isSurveyType" class="col-md pt-2">
        <b-card class="skills-card-theme-border" body-class="pt-2 pb-1" data-cy="quizTimeLimitCard">
          <i class="fas fa-business-time text-info" style="font-size: 1.3rem;"></i>
          <span class="text-secondary font-italic ml-1">Time Limit:</span>
          <span v-if="quizInfo.quizTimeLimit > 0" class="text-uppercase ml-1 font-weight-bold">{{quizTimeLimit | formatDuration}}</span>
          <span v-else class="text-uppercase ml-1 font-weight-bold">NONE</span>
        </b-card>
      </div>
      <div v-if="!isSurveyType" class="col-md pt-2">
        <b-card class="skills-card-theme-border" body-class="pt-2 pb-1" data-cy="quizInfoCard">
          <i class="fas fa-redo-alt text-info" style="font-size: 1.3rem;" aria-hidden="true"></i>
          <span class="text-secondary font-italic ml-1">Attempts:</span>
          <span class="text-uppercase ml-1 font-weight-bold" data-cy="numAttempts"><b-badge>{{quizInfo.userNumPreviousQuizAttempts}}</b-badge> / <b-badge>{{ maxAttemptsDisplay }}</b-badge></span>
        </b-card>
      </div>
    </div>

    <div v-if="!quizInfo.userQuizPassed && allAttemptsExhausted" class="alert alert-danger mt-3 h4" data-cy="noMoreAttemptsAlert">
      <i class="fas fa-exclamation-triangle" aria-hidden="true"></i> No more attempts available. This quiz allows <b-badge>{{quizInfo.maxAttemptsAllowed}}</b-badge> maximum attempt<span v-if="quizInfo.maxAttemptsAllowed > 1">s</span>.
    </div>
    <div v-if="numQuestions === 0" class="alert alert-danger mt-3 h4" data-cy="quizHasNoQuestions">
      <i class="fas fa-exclamation-triangle" aria-hidden="true"></i> This {{ quizInfo.quizType }} has no questions declared and unfortunately cannot be completed.
    </div>

    <p v-if="quizInfo.description && !allAttemptsExhausted" class="mt-3" data-cy="quizDescription">
      <markdown-text :text="quizInfo.description" />
    </p>

    <div class="mt-5">
      <b-button v-if="canStartQuiz" variant="outline-danger"
                :aria-label="`Cancel ${quizInfo.quizType} run`"
                @click="cancel" class="text-uppercase mr-2 skills-theme-btn" data-cy="cancelQuizAttempt"><i class="fas fas fa-times-circle" aria-hidden="true"> Cancel</i></b-button>
      <b-button v-if="canStartQuiz" variant="outline-success"
                :aria-label="`Start ${quizInfo.quizType} run`"
                @click="start" class="text-uppercase skills-theme-btn" data-cy="startQuizAttempt"><i class="fas fa-play-circle" aria-hidden="true"> Start</i></b-button>
      <b-button v-if="!canStartQuiz" variant="outline-primary"
                :aria-label="`Close ${quizInfo.quizType} run`"
                @click="cancel" class="text-uppercase mr-2 skills-theme-btn" data-cy="closeQuizAttempt"><i class="fas fas fa-times-circle" aria-hidden="true"> Close</i></b-button>
    </div>
  </b-card>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';

  export default {
    name: 'QuizRunSplashScreen',
    components: {
      MarkdownText,
    },
    props: {
      quizInfo: Object,
    },
    computed: {
      quizTimeLimit() {
        return this.quizInfo.quizTimeLimit * 1000;
      },
      numQuestions() {
        return this.quizInfo.quizLength;
      },
      isSurveyType() {
        return this.quizInfo.quizType === 'Survey';
      },
      maxAttemptsDisplay() {
        return this.quizInfo.maxAttemptsAllowed > 0 ? this.quizInfo.maxAttemptsAllowed : 'Unlimited';
      },
      allAttemptsExhausted() {
        return this.quizInfo.maxAttemptsAllowed > 0 && this.quizInfo.maxAttemptsAllowed <= this.quizInfo.userNumPreviousQuizAttempts;
      },
      minNumQuestionsToPass() {
        return this.quizInfo.minNumQuestionsToPass > 0 ? this.quizInfo.minNumQuestionsToPass : this.numQuestions;
      },
      canStartQuiz() {
        return !this.quizInfo.userQuizPassed && !this.allAttemptsExhausted && this.numQuestions > 0;
      },
    },
    methods: {
      cancel() {
        this.$emit('cancel');
      },
      start() {
        this.$emit('start');
      },
    },
  };
</script>

<style scoped>

</style>
