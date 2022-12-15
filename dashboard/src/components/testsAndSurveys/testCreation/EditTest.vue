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
      <b-container fluid>
        <div class="form-group">
          <label for="projectIdInput">* Name</label>
          <ValidationProvider
            rules="required|minNameLength|maxQuizNameLength|uniqueName|customNameValidator"
            v-slot="{errors}"
            name="Test Name">
            <input class="form-control" type="text" v-model="quizInternal.name"
                   v-on:input="updateQuizId"
                   v-on:keydown.enter="handleSubmit(updateProject)"
                   v-focus
                   data-cy="testName"
                   id="testNameInput"
                   :aria-invalid="errors && errors.length > 0"
                   aria-errormessage="testNameError"
                   aria-describedby="testNameError"/>
            <small role="alert" class="form-text text-danger" data-cy="testNameError"
                   id="testNameError">{{ errors[0] }}</small>
          </ValidationProvider>
        </div>

        <id-input type="text" label="Test ID" v-model="quizInternal.quizId"
                  additional-validation-rules="uniqueId"
                  v-on:keydown.enter.native="handleSubmit(updateProject)"
                  :next-focus-el="previousFocus"
                  @shown="tooltipShowing=true"
                  @hidden="tooltipShowing=false"/>

        <label>Description</label>
        <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{errors}"
                            name="Badge Description">
          <markdown-editor v-model="quizInternal.description"></markdown-editor>
          <small role="alert" class="form-text text-danger mb-3" data-cy="badgeDescriptionError">{{ errors[0] }}</small>
        </ValidationProvider>

        <p v-if="invalid && overallErrMsg" class="text-center text-danger mt-2" aria-live="polite">
          <small>***{{ overallErrMsg }}***</small></p>
      </b-container>

      <div slot="modal-footer" class="w-100">
        <b-button variant="success" size="sm" class="float-right" @click="handleSubmit(saveQuiz)"
                  :disabled="invalid"
                  data-cy="saveQuizButton">
          <span>Save Quiz</span>
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe" data-cy="closeQuizButton">
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
  import MarkdownEditor from '@/components/utils/MarkdownEditor';
  import ProjectService from '@/components/projects/ProjectService';

  export default {
    name: 'EditTest',
    components: { MarkdownEditor, IdInput },
    props: {
      test: Object,
      isEdit: {
        type: Boolean,
        default: false,
      },
      value: Boolean,
    },
    data() {
      const quizInternal = {
        originalQuizId: this.test.quizId,
        isEdit: this.isEdit,
        ...this.test,
      };
      return {
        show: this.value,
        quizInternal,
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
        setTimeout(() => {
          this.$nextTick(() => {
            const { observer } = this.$refs;
            if (observer) {
              observer.validate({ silent: false });
            }
          });
        }, 600);
      }
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Test' : 'New Test';
      },
    },
    methods: {
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
          this.$emit('hidden', e);
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
              this.closeMe();
              this.quizInternal.name = InputSanitizer.sanitize(this.quizInternal.name);
              this.quizInternal.quizId = InputSanitizer.sanitize(this.quizInternal.quizId);
              this.$emit('quiz-saved', this.quizInternal);
            }
          });
      },
      registerValidation() {
        const self = this;
        extend('uniqueName', {
          message: (field) => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && (self.original.name === value || self.original.name.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
              return true;
            }
            return ProjectService.checkIfProjectNameExist(value)
              .then((remoteRes) => !remoteRes);
          },
        });

        extend('uniqueId', {
          message: (field) => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.original.projectId === value) {
              return true;
            }
            return ProjectService.checkIfProjectIdExist(value)
              .then((remoteRes) => !remoteRes);
          },
        });
      },
    },
  };
</script>

<style scoped>

</style>
