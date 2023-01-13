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
  <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
    <b-modal id="questionEditModal" size="xl" :title="title" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="publishHidden"
             header-text-variant="light" no-fade>
      <skills-spinner :is-loading="loading"/>
      <b-container v-if="!loading" fluid>
        <div class="mb-2">
          <span class="font-weight-bold text-primary">Question:</span>
        </div>

        <ValidationProvider rules="required|maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{errors}" name="Skill Description">
          <markdown-editor v-if="questionDefInternal"
                           :resizable="true"
                           v-focus
                           markdownHeight="150px"
                           v-model="questionDefInternal.question"
                           data-cy="skillDescription"/>
          <small role="alert" class="form-text text-danger" data-cy="skillDescriptionError">{{ errors[0] }}</small>
        </ValidationProvider>

        <div class="mt-3 mb-2">
          <span class="font-weight-bold text-primary">Answers:</span>
        </div>

        <div class="row mb-2 no-gutters">
          <div class="col">
            <vue-select :options="questionType.options"
                        :clearable="false"
                        v-model="questionType.selectedType">
              <template v-slot:option="option">
                <div class="p-1">
                  <i :class="option.icon" style="min-width: 1.2rem" class="border rounded p-1 mr-2"></i>
                  <span class="">{{ option.label }}</span>
                </div>
              </template>
            </vue-select>
          </div>
          <div class="col-auto ml-1">
            <div class="border rounded form-control">
              <span class="font-italic">Graded:</span> <b-form-checkbox v-model="questionDefInternal.graded" name="check-button" class="d-inline-block" switch>
            </b-form-checkbox>
            </div>
          </div>
        </div>

        <div class="pl-3">
          <div class="mb-1">
            <span class="text-secondary">Check the correct answer(s) on the left:</span>
          </div>
          <div v-for="(answer, index) in questionDefInternal.answers" :key="index">
            <div class="row no-gutters mt-2">
              <div class="col-auto">
                <select-correct-answer v-model="answer.isCorrect" class="mr-2" @selected="updateNumQuestionWithContent"/>
              </div>
              <div class="col">
                  <input class="form-control" type="text" v-model="answer.answer"
                         placeholder="Enter an answer"
                         data-cy="testName"
                         id="testNameInput"
                         aria-errormessage="testNameError"
                         aria-describedby="testNameError"/>
              </div>
              <b-button-group class="ml-2">
                <b-button variant="outline-info"
                          :disabled="noMoreAnswers"
                          :aria-label="`Add New Answer at index ${index}`"
                          @click="addNewAnswer(index)">
                  <i class="fas fa-plus"></i>
                </b-button>
                <b-button variant="outline-info"
                          :disabled="twoOrLessQuestions"
                          :aria-label="`Delete Answer at index ${index}`"
                          @click="removeAnswer(index)">
                  <i class="fas fa-minus"></i>
                </b-button>
              </b-button-group>
            </div>
          </div>
          <div v-if="noMoreAnswers" class="alert alert-warning mt-2">
            <i class="fas fa-exclamation-triangle" /> Cannot exceed maximum of <b-badge>{{ maxAnswersAllowed }}</b-badge> answers per question.
            <b-button variant="outline-info" :disabled="true" size="sm" aria-label="Add New Answer"><i class="fas fa-plus"></i></b-button> button was disabled.
          </div>
          <div v-if="customValidation.show" class="alert alert-warning mt-2">
            <div v-if="customValidation.numAnswersWithContent < 2">
              <i class="fas fa-exclamation-triangle" /> Must enter at least <b-badge>2</b-badge> answers.
            </div>
            <div v-if="!customValidation.atLeastOneCorrectAnswerSelected" class="mt-2">
              <i class="fas fa-exclamation-triangle" /> Must select at least <b-badge>1</b-badge> correct answer!
            </div>
            <div v-if="customValidation.emptyQuestionSetToBeCorrect" class="mt-2">
              <i class="fas fa-exclamation-triangle" /> Empty question <b>cannot</b> be a correct selection.
            </div>
          </div>
        </div>

      </b-container>

      <div slot="modal-footer" class="w-100">
        <b-button v-if="!loading" variant="success" size="sm" class="float-right"
                  @click="handleSubmit(saveAnswer)"
                  :disabled="invalid"
                  data-cy="saveAnswerButton">
          <span>Save</span>
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe"
                  data-cy="closeQuizButton">
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
  import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer';

  export default {
    name: 'EditQuestion',
    components: {
      SelectCorrectAnswer,
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
        loading: false,
        show: this.value,
        questionDefInternal: {},
        questionType: {
          options: [{
            label: 'Multiple Choice',
            id: 'MultipleChoice',
            icon: 'fas fa-tasks',
          }, {
            label: 'Input Text',
            id: 'InputText',
            icon: 'far fa-keyboard',
          }],
          selectedType: {
            label: 'Multiple Choice',
            id: 'MultipleChoice',
            icon: 'fas fa-tasks',
          },
        },
        customValidation: {
          show: false,
          startUpdatingShowFlag: false,
          numAnswersWithContent: 0,
          atLeastOneCorrectAnswerSelected: false,
          emptyQuestionSetToBeCorrect: false,
        },
      };
    },
    mounted() {
      if (this.isEdit) {
        this.loading = true;
        QuizService.getQuizDef(this.quiz.quizId)
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
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
      'questionDefInternal.answers': {
        handler: function updateNumQuestionWithContent() {
          this.updateNumQuestionWithContent();
        },
        deep: true,
      },
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Question' : 'New Question';
      },
      twoOrLessQuestions() {
        const { answers } = this.questionDefInternal;
        return !answers || answers?.length <= 2;
      },
      maxAnswersAllowed() {
        return this.$store.getters.config.maxAnswersPerQuizQuestion;
      },
      noMoreAnswers() {
        const { answers } = this.questionDefInternal;
        return answers && answers?.length >= this.maxAnswersAllowed;
      },
    },
    methods: {
      setInternalQuestionDef(questionDef) {
        this.questionDefInternal = {
          isEdit: this.isEdit,
          ...questionDef,
        };
      },
      addNewAnswer(index) {
        const newQuestion = {
          id: null,
          answer: '',
          isCorrect: false,
        };
        this.questionDefInternal.answers.splice(index + 1, 0, newQuestion);
      },
      removeAnswer(index) {
        this.questionDefInternal.answers = this.questionDefInternal.answers.filter((item, arrIndex) => arrIndex !== index);
      },
      updateNumQuestionWithContent() {
        let numAnswersWithContent = 0;
        let atLeastOneCorrectAnswerSelected = false;
        let emptyQuestionSetToBeCorrect = false;
        const { answers } = this.questionDefInternal;
        if (answers) {
          numAnswersWithContent = answers.filter((a) => (a.answer && a.answer.trim().length > 0)).length;
          atLeastOneCorrectAnswerSelected = answers.find((a) => a.isCorrect) !== undefined;
          emptyQuestionSetToBeCorrect = answers.find((a) => a.isCorrect && (!a.answer || a.answer.trim().length === 0)) !== undefined;
        }
        this.customValidation.numAnswersWithContent = numAnswersWithContent;
        this.customValidation.atLeastOneCorrectAnswerSelected = atLeastOneCorrectAnswerSelected;
        this.customValidation.emptyQuestionSetToBeCorrect = emptyQuestionSetToBeCorrect;
        if (this.customValidation.startUpdatingShowFlag) {
          this.customValidation.show = !this.isCustomValidationValid();
        }
      },
      isCustomValidationValid() {
        return this.customValidation.numAnswersWithContent >= 2 && this.customValidation.atLeastOneCorrectAnswerSelected && !this.customValidation.emptyQuestionSetToBeCorrect;
      },
      closeMe(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        if (this.tooltipShowing) {
          e.preventDefault();
        } else {
          this.$emit('hidden', this.quizInternal);
        }
      },
      saveAnswer() {
        this.$refs.observer.validate()
          .then((res) => {
            if (res) {
              if (!this.isCustomValidationValid()) {
                this.customValidation.show = true;
                this.customValidation.startUpdatingShowFlag = true;
              } else {
                const { answers } = this.questionDefInternal;
                const removeEmptyQuestions = answers.filter((a) => (a.answer && a.answer.trim().length > 0));
                const questionDefRes = {
                  question: this.questionDefInternal.question,
                  questionType: this.questionType.selectedType.id,
                  answers: removeEmptyQuestions,
                };
                this.$emit('question-saved', questionDefRes);
                this.closeMe();
              }
            }
          });
      },
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
