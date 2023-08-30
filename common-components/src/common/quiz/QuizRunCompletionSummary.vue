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
  <b-card body-class="text-left" data-cy="quizCompletion">
    <div class="h4" tabindex="-1" ref="completionSummaryTitle" data-cy="completionSummaryTitle">
      <slot name="completeAboveTitle" v-if="quizResult.gradedRes.passed">
        <i class="fas fa-handshake text-info skills-page-title-text-color" aria-hidden="true"></i> Thank you for completing the {{ quizInfo.quizType }}!
      </slot>
      <span v-else-if="!quizResult.outOfTime"><i class="fas fa-handshake text-info skills-page-title-text-color"></i> Thank you for completing the {{ quizInfo.quizType }}!</span>
      <span v-else>You've run out of time!</span>
    </div>
    <div class="mb-1 mt-4 h2">
      <span class="font-weight-bold text-success mb-2 skills-page-title-text-color">{{ quizInfo.name }}</span>
      <div class="h2 d-inline-block ml-2">
        <b-badge v-if="!quizResult.gradedRes.passed" class="text-uppercase" variant="warning" data-cy="quizFailed"><i class="far fa-times-circle" aria-hidden="true"></i> Failed</b-badge>
        <b-badge v-if="quizResult.gradedRes.passed" class="text-uppercase" variant="success" data-cy="quizPassed"><i class="fas fa-check-double" aria-hidden="true"></i> Passed</b-badge>
      </div>
    </div>
    <b-card-group deck>
      <b-card bg-variant="light" class="text-center skills-card-theme-border" data-cy="numCorrectInfoCard">
        <b-card-text>
          <div class="h3" data-cy="numCorrect" v-if="!quizResult.outOfTime">
            <b-badge variant="success">{{ quizResult.numCorrect }}</b-badge> out of <b-badge>{{ quizResult.numTotal }}</b-badge>
          </div>
          <div class="h3" data-cy="timedOut" v-else-if="quizResult.outOfTime">
            <i class="fas fa-hourglass-end"></i> Time Expired
          </div>
          <div class="text-secondary mt-2" data-cy="subTitleMsg">
            <span v-if="!quizResult.gradedRes.passed && quizResult.missedBy > 0 && !quizResult.outOfTime">Missed by <b-badge variant="warning">{{ quizResult.missedBy }}</b-badge> question{{ quizResult.missedBy > 1 ? 's' : '' }}</span>
            <span v-else-if="!quizResult.gradedRes.passed && quizResult.outOfTime">You've run out of time!</span>

            <span v-else>Well done!</span>
          </div>
        </b-card-text>
      </b-card>
      <b-card bg-variant="light" class="text-center skills-card-theme-border" data-cy="percentCorrectInfoCard">
        <b-card-text v-if="!quizResult.outOfTime">
          <div class="h3">
            <span data-cy="percentCorrect">{{ quizResult.percentCorrect }}%</span>
          </div>
          <div class="text-secondary mt-2">
            <b data-cy="percentToPass">{{ quizInfo.percentToPass }}%</b> is required to pass
          </div>
        </b-card-text>
        <b-card-text v-else>
          <div class="h3">
            <i class="fas fa-clock"></i> {{quizInfo.quizTimeLimit * 1000 | formatDuration}}
          </div>
          <div class="text-secondary mt-2">
            You must complete the quiz within the time limit.
          </div>
        </b-card-text>
      </b-card>

      <b-card v-if="quizResult.gradedRes.passed" bg-variant="light" class="text-center skills-card-theme-border" data-cy="quizRuntime">
        <b-card-text>
          <div class="h3" data-cy="title">
            {{ quizResult.gradedRes.started | duration(quizResult.gradedRes.completed) }}
          </div>
          <div class="text-secondary mt-2" data-cy="subTitle">
           Time to Complete
          </div>
        </b-card-text>
      </b-card>

      <b-card v-if="!quizResult.gradedRes.passed" bg-variant="light" class="text-center skills-card-theme-border" data-cy="numAttemptsInfoCard">
        <b-card-text>
          <div class="h3" data-cy="title">
            <span v-if="unlimitedAttempts" class=""><i class="fas fa-infinity" aria-hidden="true"></i> Attempts</span>
            <span v-if="!unlimitedAttempts">
              <span v-if="numAttemptsLeft === 0">No</span>
              <b-badge v-else variant="success">{{ numAttemptsLeft }}</b-badge> More Attempt{{ numAttemptsLeft !== 1 ? 's' : '' }}
            </span>
          </div>
          <div class="text-secondary mt-2" data-cy="subTitle">
            <span v-if="unlimitedAttempts">Unlimited Attempts - <b-badge variant="warning">{{ quizInfo.userNumPreviousQuizAttempts  + 1 }}</b-badge> attempt so far</span>
            <span v-if="!unlimitedAttempts">Used <b-badge variant="warning">{{ quizInfo.userNumPreviousQuizAttempts  + 1 }}</b-badge> out of <b-badge variant="success">{{ quizInfo.maxAttemptsAllowed }}</b-badge> attempts</span>
          </div>
        </b-card-text>
      </b-card>
    </b-card-group>

    <div v-if="!quizResult.gradedRes.passed" class="mt-4">
      <div class="my-2" v-if="unlimitedAttempts || numAttemptsLeft > 0"><span class="text-info">No worries!</span> Would you like to try again?</div>
      <b-button variant="outline-danger"  @click="close" class="text-uppercase font-weight-bold mr-2 skills-theme-btn" data-cy="closeQuizBtn"><i class="fas fa-times-circle" aria-hidden="true"></i> Close</b-button>
      <b-button v-if="unlimitedAttempts || numAttemptsLeft > 0" variant="outline-success" @click="runAgain" class="text-uppercase font-weight-bold skills-theme-btn" data-cy="runQuizAgainBtn"><i class="fas fa-redo" aria-hidden="true"></i> Try Again</b-button>
    </div>

    <div v-if="quizResult.gradedRes.passed" class="mt-4">
      <b-button variant="outline-success" @click="close" class="text-uppercase font-weight-bold skills-theme-btn" data-cy="closeQuizBtn"><i class="fas fa-times-circle" aria-hidden="true"></i> Close</b-button>
    </div>
  </b-card>
</template>

<script>
  export default {
    name: 'QuizRunCompletionSummary',
    props: {
      quizResult: Object,
      quizInfo: Object,
    },
    methods: {
      close() {
        this.$emit('close');
      },
      runAgain() {
        this.$emit('run-again');
      },
    },
    computed: {
      unlimitedAttempts() {
        return this.quizInfo.maxAttemptsAllowed <= 0;
      },
      numAttemptsLeft() {
        return this.quizInfo.maxAttemptsAllowed - this.quizInfo.userNumPreviousQuizAttempts - 1;
      },
    },
  };
</script>

<style scoped>

</style>
