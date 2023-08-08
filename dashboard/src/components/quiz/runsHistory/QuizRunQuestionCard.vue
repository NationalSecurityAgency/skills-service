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
  <div data-cy="questionDisplayCard">
    <div :data-cy="`questionDisplayCard-${questionNum}`">
      <div v-if="!hasAnswer" class="d-block" data-cy="noAnswer">
        <b-badge variant="warning">No Answer</b-badge>
      </div>
      <div class="row no-gutters">
      <div class="col-auto py-2 pr-2">
        <b-overlay :show="!isSurvey && isWrong" variant="transparent" opacity="0">
          <template #overlay>
            <i class="fa fa-ban text-danger" style="font-size: 1.9rem; opacity: 0.8" data-cy="wrongAnswer"></i>
          </template>
          <b-badge variant="primary" :aria-label="`Question number ${questionNum}`">{{ questionNum }}</b-badge>
        </b-overlay>
      </div>
      <div class="col">
        <markdown-text :text="question.question" data-cy="questionDisplayText"/>

        <div v-if="!isTextInputType && !isRating" class="mt-1 pl-1">
          <div v-for="(a, index) in question.answers" :key="a.id" class="row no-gutters">
            <div class="col-auto pb-1" :data-cy="`answerDisplay-${index}`">
              <select-correct-answer :value="a.isSelected"
                                     :read-only="true"
                                     :is-radio-icon="isSingleChoiceType"
                                     :markIncorrect="!isSurvey && hasAnswer && a.isConfiguredCorrect !== a.isSelected"
                                     font-size="1.3rem"/>
            </div>
            <div class="col ml-2 pb-1"><div class="answerText align-middle" :data-cy="`answer-${index}_displayText`">{{ a.answer }}</div>
            </div>
          </div>
        </div>
        <div v-if="isTextInputType" class="border rounded p-3" data-cy="TextInputAnswer">
          <pre>{{ answerText }}</pre>
        </div>
        <div v-if="isRating">
          <b-form-rating no-border readonly inline :value="surveyScore" />
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script>
  import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer';
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import QuestionType from '@/common-components/quiz/QuestionType';

  export default {
    name: 'QuizRunQuestionCard',
    components: { MarkdownText, SelectCorrectAnswer },
    props: {
      quizType: String,
      question: Object,
      questionNum: Number,
    },
    data() {
      return {
        showDeleteDialog: false,
        answerText: this.question.answers[0].answer,
      };
    },
    computed: {
      isSingleChoiceType() {
        return this.question.questionType === QuestionType.SingleChoice;
      },
      isTextInputType() {
        return this.question.questionType === QuestionType.TextInput;
      },
      isRating() {
        return this.question.questionType === QuestionType.Rating;
      },
      hasAnswer() {
        return this.question.answers.find((a) => a.isSelected === true) !== undefined;
      },
      isWrong() {
        return this.question.answers.find((a) => this.hasAnswer && a.isConfiguredCorrect !== a.isSelected) !== undefined;
      },
      isSurvey() {
        return this.quizType === 'Survey';
      },
      surveyScore() {
        const answer = this.question.answers.find((a) => a.isSelected === true);
        return answer.answer;
      },
    },
    methods: {
    },
  };
</script>

<style scoped>
pre {
  overflow-x: auto;
  white-space: pre-wrap;
  white-space: -moz-pre-wrap;
  white-space: -pre-wrap;
  white-space: -o-pre-wrap;
  word-wrap: break-word;
}
</style>
