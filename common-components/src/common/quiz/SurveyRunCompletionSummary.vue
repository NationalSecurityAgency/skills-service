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
  <b-card data-cy="surveyCompletion">
    <div class="h5">
      <slot name="completeAboveTitle">
        <i class="fas fa-handshake text-info" aria-hidden="true"></i> Thank you for taking the time to complete the survey!
      </slot>
    </div>

    <div class="mb-1 mt-4 h2">
      <span class="font-weight-bold text-success mb-2 skills-page-title-text-color">{{ quizInfo.name }}</span>
    </div>

    <div class="row">
      <div class="col-auto">
        <b-card class="text-center skills-card-theme-border" body-class="pt-2 pb-1">
          <i class="fas fa-question-circle text-info skills-theme-quiz-correct-answer" style="font-size: 1.3rem;" aria-hidden="true"></i>
          <span class="text-secondary font-italic ml-1">Questions:</span>
          <span class="text-uppercase ml-1 font-weight-bold">{{ quizInfo.questions.length }}</span>
        </b-card>
      </div>
      <div class="col-auto">
        <b-card class="text-center skills-card-theme-border" body-class="pt-2 pb-1" data-cy="surveyRuntimeCard">
          <i class="fas fa-business-time text-info skills-theme-quiz-correct-answer" style="font-size: 1.3rem;" aria-hidden="true"></i>
          <span class="text-secondary font-italic ml-1">Completed In:</span>
          <span class="text-uppercase ml-1 font-weight-bold">{{ quizResult.gradedRes.started | duration(quizResult.gradedRes.completed) }}</span>
        </b-card>
      </div>
      <div class="col"></div>
    </div>

    <div class="mt-5">
      <b-button variant="outline-success"
                @click="close"
                class="text-uppercase font-weight-bold skills-theme-btn"
                data-cy="closeSurveyBtn"><i class="fas fa-times-circle" aria-hidden="true"></i> Close</b-button>
    </div>
  </b-card>
</template>

<script>
  export default {
    name: 'SurveyRunCompletionSummary',
    props: {
      quizResult: Object,
      quizInfo: Object,
    },
    methods: {
      close() {
        this.$emit('close');
      },
    },
  };
</script>

<style scoped>

</style>
