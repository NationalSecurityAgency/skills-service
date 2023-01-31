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
  <div class="row no-gutters mb-4" :data-cy="`question_${num}`">
    <div class="col-auto pt-2 pr-2">
      <b-badge class="d-inline-block" :variant="`${q.gradedInfo ? (q.gradedInfo.isCorrect ? 'success' : 'danger') : 'default'}`">{{ num }}</b-badge>
      <span v-if="q.gradedInfo" class="ml-1 pt-1">
        <span v-if="q.gradedInfo.isCorrect" class="text-success" style="font-size: 1.1rem;" data-cy="questionAnsweredCorrectly"><i class="fas fa-check-double" aria-hidden="true"></i></span>
        <span v-if="!q.gradedInfo.isCorrect" class="text-danger" style="font-size: 1.1rem;" data-cy="questionAnsweredWrong"><i class="fas fa-times-circle" aria-hidden="true"></i></span>
      </span>
    </div>

    <div class="col">
      <markdown-text :text="q.question" data-cy="questionsText" />

      <ValidationObserver ref="singleQuestionObserver">
      <div v-if="isTextInput">
        <ValidationProvider rules="required|customDescriptionValidator" v-slot="{errors}" :name="`Answer to question #${num}`" :debounce="400" :immediate="false">
          <b-form-textarea
              :id="`answer-${num}`"
              data-cy="textInputAnswer"
              v-model="answerText"
              :debounce="500"
              placeholder="Please enter your response here..."
              rows="2"
              max-rows="20"/>
          <small role="alert" class="form-text text-danger" data-cy="textInputAnswerErr" id="skillPointIncrementError">{{ errors[0] }}</small>
        </ValidationProvider>
      </div>
      <div v-else>
        <div v-if="isMultipleChoice" class="text-secondary font-italic small" data-cy="multipleChoiceMsg">(Select <b>all</b> that apply)</div>
          <ValidationProvider rules="atLeastOneSelected" v-slot="{errors}" :name="`Question ${num}`" :immediate="false">
            <quiz-run-answers class="mt-1 pl-1"
                              @selected-answer="selectionChanged"
                              v-model="answerOptions"
                              :q="q"
                              :can-select-more-than-one="isMultipleChoice"/>
            <small role="alert" class="form-text text-danger" data-cy="choiceAnswerErr" id="skillPointIncrementError">{{ errors[0] }}</small>
          </ValidationProvider>
      </div>
      </ValidationObserver>
    </div>
  </div>
</template>

<script>
  import { extend } from 'vee-validate';
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import QuizRunAnswers from '@/common-components/quiz/QuizRunAnswers';
  import QuestionType from '@/common-components/quiz/QuestionType';
  import QuizRunService from './QuizRunService';

  export default {
    name: 'QuizRunQuestion',
    components: { QuizRunAnswers, MarkdownText },
    props: {
      q: Object,
      quizId: String,
      quizAttemptId: Number,
      num: Number,
    },
    data() {
      return {
        answerOptions: [],
        answerText: '',
      };
    },
    mounted() {
      this.answerOptions = this.q.answerOptions.map((a) => ({ ...a, selected: a.selected ? a.selected : false }));
      if (this.isTextInput) {
        const existingAnswerText = this.q.answerOptions[0].answerText;
        this.answerText = existingAnswerText || '';
      }
      this.setupValidation();
    },
    watch: {
      answerText() {
        this.textAnswerChanged();
      },
    },
    computed: {
      isMultipleChoice() {
        return this.q.questionType === QuestionType.MultipleChoice;
      },
      isSingleChoice() {
        return this.q.questionType === QuestionType.SingleChoice;
      },
      isTextInput() {
        return this.q.questionType === QuestionType.TextInput;
      },
      isMissingAnswer() {
        if (this.isTextInput) {
          return !this.answerText || this.answerText.trimEnd() === '';
        }
        return this.answerOptions.findIndex((a) => a.selected === true) < 0;
      },
    },
    methods: {
      setupValidation() {
        extend('atLeastOneSelected', {
          message: () => 'At least 1 choice must be selected',
          validate(value) {
            const foundSelected = value && (value.findIndex((a) => a.selected) >= 0);
            return foundSelected;
          },
        }, {
          immediate: false,
        });
      },
      textAnswerChanged() {
        const selectedAnswerIds = this.answerOptions.map((a) => a.id);
        const isAnswerBlank = !this.answerText || this.answerText.trimEnd() === '';
        const currentAnswer = {
          questionId: this.q.id,
          questionType: this.q.questionType,
          selectedAnswerIds,
          changedAnswerId: this.answerOptions[0].id,
          changedAnswerIdSelected: !isAnswerBlank,
          answerText: this.answerText,
        };
        this.reportAnswer(currentAnswer).then((reportAnswerPromise) => {
          // only 1 answer in case of TextInput
          this.$emit('answer-text-changed', {
            ...currentAnswer,
            reportAnswerPromise,
          });
        });
      },
      selectionChanged(currentAnswer) {
        this.reportAnswer(currentAnswer).then((reportAnswerPromise) => {
          this.$emit('selected-answer', {
            ...currentAnswer,
            reportAnswerPromise,
          });
        });
      },
      reportAnswer(answer) {
        if (this.$refs.singleQuestionObserver) {
          return this.$refs.singleQuestionObserver.validate({ silent: false })
            .then((validationResults) => {
              if (validationResults) {
                return QuizRunService.reportAnswer(this.quizId, this.quizAttemptId, answer.changedAnswerId, answer.changedAnswerIdSelected, answer.answerText);
              }
              return null;
            });
        }
        return new Promise((resolve) => {
          resolve(null);
        });
      },
    },
  };
</script>

<style scoped>

</style>
