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
      <slot name="aboveTitle" />
      <div class="mb-1 mt-4 h2">
        <span class="font-weight-bold text-success">{{ quizInfo.name }}</span>
      </div>
    </div>

    <div class="row">
      <div class="col-auto">
        <b-card class="text-center" body-class="pt-2 pb-1" data-cy="quizInfoCard">
          <i class="fas fa-question-circle text-info" style="font-size: 1.3rem;"></i>
          <span class="text-secondary font-italic ml-1">Questions:</span>
          <span class="text-uppercase ml-1 font-weight-bold" data-cy="numQuestions">{{ quizInfo.questions.length }}</span>
        </b-card>
      </div>
      <div v-if="!isSurveyType" class="col-auto">
        <b-card class="text-center" body-class="pt-2 pb-1" data-cy="quizInfoCard">
          <i class="fas fa-business-time text-info" style="font-size: 1.3rem;"></i>
          <span class="text-secondary font-italic ml-1">Time Limit:</span>
          <span class="text-uppercase ml-1 font-weight-bold">None</span>
        </b-card>
      </div>
      <div v-if="!isSurveyType" class="col-auto">
        <b-card class="text-center" body-class="pt-2 pb-1" data-cy="quizInfoCard">
          <i class="fas fa-redo-alt text-info" style="font-size: 1.3rem;"></i>
          <span class="text-secondary font-italic ml-1">Attempts:</span>
          <span class="text-uppercase ml-1 font-weight-bold" data-cy="numAttempts"><b-badge>{{quizInfo.userNumPreviousQuizAttempts}}</b-badge> / <b-badge>{{ maxAttemptsDisplay }}</b-badge></span>
        </b-card>
      </div>
      <div class="col"></div>
    </div>

    <div v-if="allAttemptsExhausted" class="alert alert-danger mt-3 h4" data-cy="noMoreAttemptsAlert">
      <i class="fas fa-exclamation-triangle" aria-hidden="true"></i> No more attempts available. This quiz allows <b-badge>{{quizInfo.maxAttemptsAllowed}}</b-badge> maximum attempt<span v-if="quizInfo.maxAttemptsAllowed > 1">s</span>.
    </div>

    <p v-if="quizInfo.description && !allAttemptsExhausted" class="mt-3" data-cy="quizDescription">
      <markdown-text :text="quizInfo.description" />
    </p>

    <div class="mt-5">
      <b-button variant="outline-danger" @click="cancel" class="text-uppercase mr-2" data-cy="cancelQuizAttempt"><i class="fas fas fa-times-circle"> Cancel</i></b-button>
      <b-button v-if="!allAttemptsExhausted" variant="outline-success" @click="start" class="text-uppercase" data-cy="startQuizAttempt"><i class="fas fa-play-circle"> Start</i></b-button>
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
      isSurveyType() {
        return this.quizInfo.quizType === 'Survey';
      },
      maxAttemptsDisplay() {
        return this.quizInfo.maxAttemptsAllowed > 0 ? this.quizInfo.maxAttemptsAllowed : 'Unlimited';
      },
      allAttemptsExhausted() {
        return this.quizInfo.maxAttemptsAllowed > 0 && this.quizInfo.maxAttemptsAllowed <= this.quizInfo.userNumPreviousQuizAttempts;
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
