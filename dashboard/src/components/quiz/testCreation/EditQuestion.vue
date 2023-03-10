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
  <ValidationObserver ref="observer" v-slot="{errors, invalid}" slim>
    <b-modal id="questionEditModal" size="xl" :title="title" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="publishHidden"
             @shown="showQuestion = true"
             header-text-variant="light" no-fade>
      <skills-spinner :is-loading="loading"/>
      <b-container v-if="!loading" fluid data-cy="editQuestionModal">
        <div class="mb-2">
          <span class="font-weight-bold text-primary">Question:</span>
        </div>

        <ValidationProvider rules="required|maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{errors}" name="Question">
          <markdown-editor v-if="showQuestion && questionDefInternal"
                           :resizable="true"
                           v-focus
                           markdownHeight="150px"
                           v-model="questionDefInternal.question"
                           data-cy="questionText"/>
          <small role="alert" class="form-text text-danger" data-cy="questionTextErr">{{ errors[0] }}</small>
        </ValidationProvider>

        <div class="mt-3 mb-2">
          <span class="font-weight-bold text-primary">Answers:</span>
        </div>
        <div v-if="questionDef.quizType === 'Survey'" class="row mb-2 no-gutters">
          <div class="col">
            <vue-select :options="questionType.options"
                        :clearable="false"
                        v-on:input="questionTypeChanged"
                        data-cy="answerTypeSelector"
                        v-model="questionType.selectedType">
              <template v-slot:option="option">
                <div class="p-1" :data-cy="`selectionItem_${option.id}`">
                  <i :class="option.icon" style="min-width: 1.2rem" class="border rounded p-1 mr-2" aria-hidden="true"></i>
                  <span class="">{{ option.label }}</span>
                </div>
              </template>
            </vue-select>
          </div>
        </div>

        <div v-if="isQuestionTypeTextInput" class="pl-3">
          <b-form-textarea
            id="textarea"
            placeholder="Users will be required to enter text."
            data-cy="textAreaPlaceHolder"
            :disabled="true"
            rows="3"
            max-rows="6"/>
        </div>
        <div v-if="!isQuestionTypeTextInput" class="pl-3">
          <div class="mb-1" v-if="isQuizType">
            <span class="text-secondary">Check one or more correct answer(s) on the left:</span>
          </div>
          <ValidationProvider
            rules="atLeastOneCorrectAnswer|atLeastTwoAnswersFilledIn|correctAnswersMustHaveText|maxNumQuestions"
            :debounce="200"
            :immediate="false"
            name="Answers">
              <configure-answers v-model="questionDefInternal.answers" :quiz-type="questionDef.quizType" />
          </ValidationProvider>
        </div>

        <div v-if="submitButtonClicked && invalid" class="alert alert-danger mt-3" data-cy="editQuestionsErrs">
          <div v-for="(error, propertyName) in errors" :key="propertyName">
            <span v-if="error">{{ error[0] }}</span>
          </div>
        </div>

      </b-container>

      <div slot="modal-footer" class="w-100">
        <b-button v-if="!loading" variant="success" size="sm" class="float-right"
                  @click="saveAnswer"
                  aria-label="Save this question"
                  data-cy="saveQuestionBtn">
          <span>Save</span>
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe"
                  aria-label="Cancel and discard this question"
                  data-cy="closeQuestionBtn">
          Cancel
        </b-button>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import VueSelect from 'vue-select';
  import QuizService from '@/components/quiz/QuizService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import MarkdownEditor from '@/common-components/utilities/MarkdownEditor';
  import QuestionType from '@/common-components/quiz/QuestionType';
  import ConfigureAnswers from '@/components/quiz/testCreation/ConfigureAnswers';
  import { extend } from 'vee-validate';

  export default {
    name: 'EditQuestion',
    components: {
      ConfigureAnswers,
      MarkdownEditor,
      SkillsSpinner,
      VueSelect,
    },
    props: {
      questionDef: Object,
      isEdit: {
        type: Boolean,
        default: false,
      },
      value: Boolean,
    },
    data() {
      return {
        loading: true,
        show: this.value,
        showQuestion: false,
        questionDefInternal: {},
        questionType: {
          options: [{
            label: 'Multiple Choice',
            id: QuestionType.MultipleChoice,
            icon: 'fas fa-tasks',
          }, {
            label: 'Single Choice',
            id: QuestionType.SingleChoice,
            icon: 'far fa-check-square',
          }, {
            label: 'Input Text',
            id: QuestionType.TextInput,
            icon: 'far fa-keyboard',
          }],
          selectedType: {
            label: 'Multiple Choice',
            id: QuestionType.MultipleChoice,
            icon: 'fas fa-tasks',
          },
        },
        submitButtonClicked: false,
      };
    },
    mounted() {
      if (this.isEdit) {
        this.loading = true;
        QuizService.getQuizQuestionDef(this.questionDef.quizId, this.questionDef.id)
          .then((resQuizDef) => {
            this.setInternalQuestionDef(resQuizDef);
          })
          .finally(() => {
            this.performValidation();
            this.loading = false;
          });
      } else {
        this.setInternalQuestionDef(this.questionDef);
      }
      this.registerValidators();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      isSurveyType() {
        return this.questionDef.quizType === 'Survey';
      },
      isQuizType() {
        return this.questionDef.quizType === 'Quiz';
      },
      isQuestionTypeTextInput() {
        return this.questionType.selectedType && this.questionType.selectedType.id === QuestionType.TextInput;
      },
      quizType() {
        return this.questionDef.quizType;
      },
      title() {
        return this.isEdit ? 'Editing Existing Question' : 'New Question';
      },
      maxAnswersAllowed() {
        return this.$store.getters.config.maxAnswersPerQuizQuestion;
      },
    },
    methods: {
      setInternalQuestionDef(questionDef) {
        this.questionDefInternal = {
          isEdit: this.isEdit,
          ...questionDef,
        };
        const qType = questionDef.type ? questionDef.type : questionDef.questionType;
        this.questionType.selectedType = this.questionType.options.find((o) => o.id === qType);
        this.loading = false;
      },
      closeMe(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        if (this.tooltipShowing) {
          e.preventDefault();
        } else {
          this.$emit('hidden', this.questionDefInternal);
        }
      },
      questionTypeChanged(inputItem) {
        if (this.isSurveyType
          && inputItem.id !== QuestionType.TextInput
          && (!this.questionDefInternal.answers || this.questionDefInternal.answers.length < 2)) {
          this.questionDefInternal.answers = [{
            id: null,
            answer: '',
            isCorrect: false,
          }, {
            id: null,
            answer: '',
            isCorrect: false,
          }];
        }
      },
      saveAnswer() {
        this.submitButtonClicked = true;
        this.$refs.observer.validate()
          .then((res) => {
            if (res) {
              const { answers } = this.questionDefInternal;
              const removeEmptyQuestions = answers.filter((a) => (a.answer && a.answer.trim().length > 0));
              const numCorrect = answers.filter((a) => a.isCorrect).length;
              let questionType = this.questionType.selectedType?.id;
              if (this.isQuizType) {
                questionType = numCorrect > 1 ? QuestionType.MultipleChoice : QuestionType.SingleChoice;
              }

              const questionDefRes = {
                id: this.questionDefInternal.id,
                question: this.questionDefInternal.question,
                questionType,
                answers: questionType === QuestionType.TextInput ? [] : removeEmptyQuestions,
              };
              this.$emit('question-saved', questionDefRes);
              this.show = false;
            }
          });
      },
      performValidation() {
        setTimeout(() => {
          this.$nextTick(() => {
            const { observer } = this.$refs;
            if (observer) {
              observer.validate({ silent: false });
            }
          });
        }, 600);
      },
      registerValidators() {
        const self = this;
        extend('atLeastOneCorrectAnswer', {
          message: () => 'Must have at least 1 correct answer selected',
          validate(value) {
            if (self.isSurveyType) {
              return true;
            }
            const numCorrect = value.filter((a) => a.isCorrect).length;
            return numCorrect >= 1;
          },
        });
        extend('atLeastTwoAnswersFilledIn', {
          message: () => 'Must have at least 2 answers',
          validate(value) {
            const numWithContent = value.filter((a) => (a.answer && a.answer.trim().length > 0)).length;
            return numWithContent >= 2;
          },
        });
        extend('correctAnswersMustHaveText', {
          message: () => 'Answers labeled as correct must have text',
          validate(value) {
            if (self.isSurveyType) {
              return true;
            }
            const correctWithoutText = value.filter((a) => (a.isCorrect && (!a.answer || a.answer.trim().length === 0))).length;
            return correctWithoutText === 0;
          },
        });
        extend('maxNumQuestions', {
          message: () => `Exceeded maximum number of [${self.maxAnswersAllowed}] answers`,
          validate(value) {
            return value && value?.length <= self.maxAnswersAllowed;
          },
        });
      },
    },
  };
</script>

<style scoped>
.selected-tag {
  font-size: 14px;
}

>>> {
  --vs-line-height: 1.7;
}
</style>
