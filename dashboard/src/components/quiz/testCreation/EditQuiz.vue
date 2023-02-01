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
    <b-modal :id="quizInternal.testId" size="xl" :title="title" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="publishHidden"
             header-text-variant="light" no-fade>
      <skills-spinner :is-loading="loading"/>
      <b-container v-if="!loading" fluid>
        <div class="form-group">
          <label for="quizNameInput">* Name</label>
          <ValidationProvider
            rules="required|minNameLength|maxQuizNameLength|uniqueName|customNameValidator"
            :debounce="500"
            v-slot="{errors}"
            name="Quiz Name">
            <input id="quizNameInput"
                   class="form-control" type="text" v-model="quizInternal.name"
                   v-on:input="updateQuizId"
                   v-on:keydown.enter="handleSubmit(saveQuiz)"
                   v-focus
                   data-cy="quizName"
                   :aria-invalid="errors && errors.length > 0"
                   aria-errormessage="quizNameError"
                   aria-describedby="quizNameError"/>
            <small role="alert" class="form-text text-danger" data-cy="quizNameError"
                   id="quizNameError">{{ errors[0] }}</small>
          </ValidationProvider>
        </div>

        <id-input type="text" label="Quiz/Survey ID" v-model="quizInternal.quizId"
                  additional-validation-rules="uniqueId"
                  v-on:keydown.enter.native="handleSubmit(updateProject)"
                  :next-focus-el="previousFocus"
                  @shown="tooltipShowing=true"
                  @hidden="tooltipShowing=false"/>

        <div class="form-group mt-3" data-cy="quizTypeSection">
          <label id="quizTypeLabel" for="quizTypeInput">* Type:</label>
          <b-form-select v-model="quizInternal.type"
                         id="quizTypeInput"
                         :options="quizTypeOptions"
                         :disabled="isEdit"
                         aria-labelledby="quizTypeLabel"
                         data-cy="quizTypeSelector" required/>
          <div v-if="isEdit" class="text-secondary font-italic small">** Can only be modified for a new quiz/survey **</div>
        </div>

        <label>Description</label>
        <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250"
                            v-slot="{errors}"
                            name="Quiz/Survey Description">
          <markdown-editor v-model="quizInternal.description" data-cy="quizDescription"></markdown-editor>
          <small role="alert" class="form-text text-danger mb-3"
                 data-cy="quizDescriptionError">{{ errors[0] }}</small>
        </ValidationProvider>

        <p v-if="invalid && overallErrMsg" class="text-center text-danger mt-2" aria-live="polite">
          <small>***{{ overallErrMsg }}***</small></p>
      </b-container>

      <div slot="modal-footer" class="w-100">
        <b-button v-if="!loading" variant="success" size="sm" class="float-right"
                  @click="handleSubmit(saveQuiz)"
                  :disabled="invalid"
                  data-cy="saveQuizButton">
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
  import { extend } from 'vee-validate';
  import InputSanitizer from '@/components/utils/InputSanitizer';
  import IdInput from '@/components/utils/inputForm/IdInput';
  import MarkdownEditor from '@/common-components/utilities/MarkdownEditor';
  import QuizService from '@/components/quiz/QuizService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  export default {
    name: 'EditQuiz',
    components: {
      SkillsSpinner,
      MarkdownEditor,
      IdInput,
    },
    props: {
      quiz: Object,
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
        quizInternal: {},
        quizTypeOptions: [
          { value: 'Quiz', text: 'Quiz' },
          { value: 'Survey', text: 'Survey' },
        ],
        currentFocus: null,
        previousFocus: null,
        tooltipShowing: false,
        overallErrMsg: '',
      };
    },
    created() {
      this.registerValidation();
    },
    mounted() {
      document.addEventListener('focusin', this.trackFocus);
      if (this.isEdit) {
        this.loading = true;
        QuizService.getQuizDef(this.quiz.quizId)
          .then((resQuizDef) => {
            this.setInternalQuizDef(resQuizDef);
          })
          .finally(() => {
            setTimeout(() => {
              this.$nextTick(() => {
                const { observer } = this.$refs;
                if (observer) {
                  observer.validate({ silent: false });
                }
              });
            }, 600);
            this.loading = false;
          });
      } else {
        this.setInternalQuizDef(this.quiz);
      }
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Quiz/Survey' : 'New Quiz/Survey';
      },
    },
    methods: {
      setInternalQuizDef(quizDef) {
        this.quizInternal = {
          originalQuizId: quizDef.quizId,
          isEdit: this.isEdit,
          type: quizDef.type ? quizDef.type : 'Quiz',
          ...quizDef,
        };
      },
      trackFocus() {
        this.previousFocus = this.currentFocus;
        this.currentFocus = document.activeElement;
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
      updateQuizId() {
        if (!this.isEdit) {
          this.quizInternal.quizId = InputSanitizer.removeSpecialChars(this.quizInternal.name);
        }
      },
      saveQuiz() {
        this.$refs.observer.validate()
          .then((res) => {
            if (res) {
              this.quizInternal.name = InputSanitizer.sanitize(this.quizInternal.name);
              this.quizInternal.quizId = InputSanitizer.sanitize(this.quizInternal.quizId);
              this.$emit('quiz-saved', this.quizInternal);
              this.closeMe();
            }
          });
      },
      registerValidation() {
        const self = this;
        extend('uniqueName', {
          message: (field) => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && (self.quizInternal.name === value || self.quizInternal.name.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
              return true;
            }
            return QuizService.checkIfQuizNameExist(value)
              .then((remoteRes) => !remoteRes);
          },
        });

        extend('uniqueId', {
          message: (field) => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.quizInternal.quizId === value) {
              return true;
            }
            return QuizService.checkIfQuizIdExist(value)
              .then((remoteRes) => !remoteRes);
          },
        });
      },
    },
  };
</script>

<style scoped>

</style>
