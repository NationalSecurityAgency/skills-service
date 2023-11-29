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
    <b-modal :id="quiz.quizId" size="xl" :title="title" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="publishHidden"
             @shown="showDescription = true"
             header-text-variant="light" no-fade>
      <skills-spinner :is-loading="loading"/>
      <b-container fluid v-if="!loading">
        <ReloadMessage v-if="restoredFromStorage" @discard-changes="discardChanges" />
        <div class="row">
          <div class="col-12">
            <div class="form-group">
          <label for="quizNameInput">* Name</label>
          <ValidationProvider
            rules="required|minNameLength|maxQuizNameLength|nullValueNotAllowed|uniqueName|customNameValidator"
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
          </div>
        </div>

        <div class="row">
          <div class="col-12">
            <id-input type="text" label="Quiz/Survey ID" v-model="quizInternal.quizId"
                      additional-validation-rules="uniqueId"
                      v-on:keydown.enter.native="handleSubmit(saveQuiz)"
                      :next-focus-el="previousFocus"
                      @shown="tooltipShowing=true"
                      @hidden="tooltipShowing=false"/>
          </div>
        </div>

        <div class="row mt-3">
          <div class="col-12">
            <div class="form-group" data-cy="quizTypeSection">
              <label id="quizTypeLabel" for="quizTypeInput">* Type:</label>
              <b-form-select v-model="quizInternal.type"
                             id="quizTypeInput"
                             :options="quizTypeOptions"
                             :disabled="isEdit"
                             aria-labelledby="quizTypeLabel"
                             data-cy="quizTypeSelector" required/>
              <div v-if="isEdit" class="text-secondary font-italic small">** Can only be modified for a new quiz/survey **</div>
            </div>
          </div>
        </div>

        <div class="row mt-3" v-if="showDescription">
          <div class="col-12">
            <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="400"
                                v-slot="{errors}"
                                name="Quiz/Survey Description">
              <markdown-editor id="quizDescription"
                               :quiz-id="isEdit ? quizInternal.quizId : null"
                               v-model="quizInternal.description" data-cy="quizDescription"></markdown-editor>
              <small role="alert" class="form-text text-danger mb-3"
                     data-cy="quizDescriptionError">{{ errors[0] }}</small>
            </ValidationProvider>
          </div>
        </div>

        <p v-if="invalid && overallErrMsg" class="text-center text-danger mt-2" aria-live="polite">
          <small>***{{ overallErrMsg }}***</small></p>
      </b-container>

      <div slot="modal-footer" class="w-100">
        <b-button v-if="!loading" variant="success" size="sm" class="float-right"
                  @click="handleSubmit(saveQuiz)"
                  :disabled="invalid"
                  :aria-label="`Save ${quizInternal.type}`"
                  data-cy="saveQuizButton">
          <span>Save</span>
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close"
                  data-cy="closeQuizButton">
          Cancel
        </b-button>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import { extend } from 'vee-validate';
  import MarkdownEditor from '@/common-components/utilities/MarkdownEditor';
  import InputSanitizer from '@/components/utils/InputSanitizer';
  import IdInput from '@/components/utils/inputForm/IdInput';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import QuizService from '@/components/quiz/QuizService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import SaveComponentStateLocallyMixin from '../../utils/SaveComponentStateLocallyMixin';
  import ReloadMessage from '../../utils/ReloadMessage';

  export default {
    name: 'EditQuiz',
    components: {
      SkillsSpinner,
      MarkdownEditor,
      IdInput,
      ReloadMessage,
    },
    mixins: [SaveComponentStateLocallyMixin, MsgBoxMixin],
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
        quizInternal: {
          quizId: this.quiz.quizId,
          description: this.quiz.description,
          name: this.quiz.name,
          isEdit: this.isEdit,
          type: this.quiz.type ? this.quiz.type : 'Quiz',
        },
        quizTypeOptions: [
          { value: 'Quiz', text: 'Quiz' },
          { value: 'Survey', text: 'Survey' },
        ],
        originalQuiz: {
          quizId: this.quiz.quizId,
          name: this.quiz.name,
          type: this.quiz.type ? this.quiz.type : 'Quiz',
          description: this.quiz.description,
        },
        currentFocus: null,
        previousFocus: null,
        tooltipShowing: false,
        overallErrMsg: '',
        showDescription: false,
        keysToWatch: ['name', 'description', 'quizId', 'type'],
        restoredFromStorage: false,
      };
    },
    created() {
      this.registerValidation();
    },
    mounted() {
      document.addEventListener('focusin', this.trackFocus);
      this.loadComponent();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
      quizInternal: {
        handler(newValue) {
          this.saveComponentState(this.componentName, newValue);
        },
        deep: true,
      },
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Quiz/Survey' : 'New Quiz/Survey';
      },
      componentName() {
        return `${this.quiz.name}-${this.$options.name}${this.isEdit ? 'Edit' : ''}`;
      },
    },
    methods: {
      discardChanges(reload = false) {
        this.clearComponentState(this.componentName);
        if (reload) {
          this.restoredFromStorage = false;
          this.loadComponent();
        }
      },
      loadComponent() {
        this.loading = true;
        const getComponentState = this.loadComponentState(this.componentName);
        const getQuizInfo = this.isEdit ? QuizService.getQuizDef(this.quiz.quizId) : null;

        Promise.all([getQuizInfo, getComponentState]).then((values) => {
          const quizInfo = values[0];
          const localInfo = values[1];
          if (localInfo) {
            if (!this.isEdit || (this.isEdit && localInfo.quizId === this.originalQuiz.quizId)) {
              this.quizInternal = localInfo;
              this.restoredFromStorage = true;
            } else if (this.isEdit && quizInfo) {
              this.quizInternal = Object.assign(this.quizInternal, quizInfo);
              this.originalQuiz = Object.assign(this.originalQuiz, quizInfo);
            } else {
              this.quizInternal = Object.assign(this.quizInternal, this.originalQuiz);
            }
          } else if (this.isEdit && quizInfo) {
            this.quizInternal = Object.assign(this.quizInternal, quizInfo);
            this.originalQuiz = Object.assign(this.originalQuiz, quizInfo);
          } else {
            this.quizInternal = Object.assign(this.quizInternal, this.originalQuiz);
          }
        }).finally(() => {
          this.loading = false;
          if (this.isEdit) {
            setTimeout(() => {
              this.$nextTick(() => {
                const { observer } = this.$refs;
                if (observer) {
                  observer.validate({ silent: false });
                }
              });
            }, 600);
          }
        });
      },
      trackFocus() {
        this.previousFocus = this.currentFocus;
        this.currentFocus = document.activeElement;
      },
      hideModal() {
        this.show = false;
        this.$emit('hidden', this.quizInternal);
      },
      close(e) {
        this.clearComponentState(this.componentName);
        this.hideModal(e);
      },
      publishHidden(e) {
        if (!e.update && this.hasObjectChanged(this.quizInternal, this.originalQuiz) && !this.loading) {
          e.preventDefault();
          this.$nextTick(() => this.$announcer.polite('You have unsaved changes.  Discard?'));
          this.msgConfirm('You have unsaved changes.  Discard?', 'Discard Changes?', 'Discard Changes', 'Continue Editing')
            .then((res) => {
              if (res) {
                this.clearComponentState(this.componentName);
                this.hideModal(e);
                this.$nextTick(() => this.$announcer.polite('Changes discarded'));
              } else {
                this.$nextTick(() => this.$announcer.polite('Continued editing'));
              }
            });
        } else if (this.tooltipShowing) {
          e.preventDefault();
        } else {
          this.clearComponentState(this.componentName);
          this.hideModal(e);
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
              this.publishHidden({ update: true });
              this.quizInternal.name = InputSanitizer.sanitize(this.quizInternal.name);
              this.quizInternal.quizId = InputSanitizer.sanitize(this.quizInternal.quizId);
              if (this.isEdit) {
                this.quizInternal.isEdit = this.isEdit;
                this.quizInternal.originalQuizId = this.originalQuiz.quizId;
              }
              this.$emit('quiz-saved', this.quizInternal);
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
