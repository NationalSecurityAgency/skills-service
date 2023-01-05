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
    <b-modal :id="subjectInternal.subjectId" size="xl" :title="title" v-model="show"
             :no-close-on-backdrop="true"
             :centered="true"
             header-bg-variant="primary"
             header-text-variant="light"
             @hide="publishHidden"
             no-fade>

        <skills-spinner :is-loading="loadingComponent"/>

        <b-container fluid v-if="!loadingComponent">
          <ReloadMessage v-if="restoredFromStorage" @discard-changes="discardChanges" />
          <div v-if="displayIconManager === false">
              <div class="media mb-3">
                <icon-picker :startIcon="subjectInternal.iconClass" @select-icon="toggleIconDisplay(true)"
                             class="mr-3"></icon-picker>
                <div class="media-body">
                  <div class="form-group">
                    <label for="subjName">Subject Name</label>
                    <ValidationProvider
                      rules="required|minNameLength|maxSubjectNameLength|uniqueName"
                      v-slot="{ errors }" name="Subject Name">
                      <input type="text" class="form-control" id="subjName" @input="updateSubjectId"
                             v-model="subjectInternal.name" v-on:input="updateSubjectId"
                             v-on:keydown.enter="handleSubmit(updateSubject)"
                             v-focus aria-required="true"
                             :aria-invalid="errors && errors.length > 0"
                             aria-errormessage="subjectNameError"
                             aria-describedby="subjectNameError"
                             data-cy="subjectNameInput">
                      <small role="alert" class="form-text text-danger" id="subjectNameError">{{errors[0]}}</small>
                    </ValidationProvider>
                  </div>
                </div>
              </div>

              <id-input type="text" label="Subject ID" v-model="subjectInternal.subjectId" @can-edit="canAutoGenerateId=!$event"
                        v-on:keydown.enter.native="handleSubmit(updateSubject)" additional-validation-rules="uniqueId"
                        :next-focus-el="previousFocus"
                        @shown="tooltipShowing=true"
                        @hidden="tooltipShowing=false"/>

              <div class="mt-2">
                <label>Description</label>
                <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{ errors }" name="Subject Description">
                  <markdown-editor v-model="subjectInternal.description"
                                   aria-errormessage="subjectDescError"
                                   aria-describedby="subjectDescError"
                                   :aria-invalid="errors && errors.length > 0"/>
                  <small role="alert" id="subjectDescError" class="form-text text-danger" data-cy="subjectDescError">{{ errors[0] }}</small>
                </ValidationProvider>
              </div>

              <help-url-input class="mt-3"
                              :next-focus-el="previousFocus"
                              @shown="tooltipShowing=true"
                              @hidden="tooltipShowing=false"
                              v-model="subjectInternal.helpUrl"
                              v-on:keydown.enter.native="handleSubmit(updateSubject)" />

              <p v-if="invalid && overallErrMsg" class="text-center text-danger" role="alert">***{{ overallErrMsg }}***</p>
          </div>
          <div v-else>
              <icon-manager @selected-icon="onSelectedIcon"></icon-manager>
              <div class="text-right mr-2">
                <b-button variant="secondary" @click="toggleIconDisplay(false)" class="mt-4">Cancel Icon Selection</b-button>
              </div>
          </div>
        </b-container>

      <div slot="modal-footer" class="w-100">
        <div v-if="displayIconManager === false">
          <b-button variant="success"
                    size="sm"
                    class="float-right"
                    @click="handleSubmit(updateSubject)"
                    :disabled="invalid"
                    data-cy="saveSubjectButton">
            Save
          </b-button>
          <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="closeSubjectButton">
            Cancel
          </b-button>
        </div>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import { extend } from 'vee-validate';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import SubjectsService from './SubjectsService';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import IdInput from '../utils/inputForm/IdInput';
  import InputSanitizer from '../utils/InputSanitizer';
  import HelpUrlInput from '../utils/HelpUrlInput';
  import SaveComponentStateLocallyMixin from '../utils/SaveComponentStateLocallyMixin';
  import ReloadMessage from '../utils/ReloadMessage';

  export default {
    name: 'EditSubject',
    mixins: [SaveComponentStateLocallyMixin, MsgBoxMixin],
    components: {
      HelpUrlInput,
      IdInput,
      IconPicker,
      SkillsSpinner,
      MarkdownEditor,
      ReloadMessage,
      'icon-manager': () => import(/* webpackChunkName: 'iconManager' */'../utils/iconPicker/IconManager'),
    },
    props: {
      subject: Object,
      isEdit: Boolean,
      value: Boolean,
    },
    data() {
      return {
        canAutoGenerateId: true,
        subjectInternal: {
          originalSubjectId: this.subject.subjectId,
          isEdit: this.isEdit,
          helpUrl: this.subject.helpUrl,
          subjectId: this.subject.subjectId,
          ...this.subject,
        },
        originalSubject: {
          subjectId: this.subject.subjectId,
          name: this.subject.name,
          helpUrl: this.subject.helpUrl,
          description: this.subject.description,
        },
        overallErrMsg: '',
        show: this.value,
        displayIconManager: false,
        currentFocus: null,
        previousFocus: null,
        tooltipShowing: false,
        loadingComponent: true,
        keysToWatch: ['name', 'description', 'subjectId', 'helpUrl'],
        restoredFromStorage: false,
      };
    },
    created() {
      this.assignCustomValidation();
    },
    mounted() {
      document.addEventListener('focusin', this.trackFocus);
      this.loadComponent();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
      subjectInternal: {
        handler(newValue) {
          this.saveComponentState(this.componentName, newValue);
        },
        deep: true,
      },
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Subject' : 'New Subject';
      },
      componentName() {
        return `${this.subjectInternal.projectId}-${this.$options.name}${this.isEdit ? 'Edit' : ''}`;
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
        this.loadingComponent = true;

        this.loadComponentState(this.componentName).then((result) => {
          if (result) {
            if (!this.isEdit || (this.isEdit && result.originalSubjectId === this.originalSubject.subjectId)) {
              this.subjectInternal = result;
              this.restoredFromStorage = true;
            } else {
              this.subjectInternal = Object.assign(this.subjectInternal, this.originalSubject);
            }
          } else {
            this.subjectInternal = Object.assign(this.subjectInternal, this.originalSubject);
          }
        }).finally(() => {
          this.loadingComponent = false;
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
      publishHidden(e) {
        if (!e.update && this.hasObjectChanged(this.subjectInternal, this.originalSubject) && !this.loadingComponent) {
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
      hideModal(e) {
        this.show = false;
        this.$emit('hidden', e);
      },
      close(e) {
        this.clearComponentState(this.componentName);
        this.hideModal(e);
      },
      updateSubject() {
        this.$refs.observer.validate()
          .then((res) => {
            if (!res) {
              this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
            } else {
              this.publishHidden({ update: true });
              this.subjectInternal.subjectName = InputSanitizer.sanitize(this.subjectInternal.subjectName);
              this.subjectInternal.subjectId = InputSanitizer.sanitize(this.subjectInternal.subjectId);
              this.$emit('subject-saved', this.subjectInternal);
            }
          });
      },
      updateSubjectId() {
        if (!this.isEdit && this.canAutoGenerateId) {
          let id = InputSanitizer.removeSpecialChars(this.subjectInternal.name);
          // Subjects, skills and badges can not have same id under a project
          // by default append Subject to avoid id collision with other entities,
          // user can always override in edit mode
          if (id) {
            id = `${id}Subject`;
          }
          this.subjectInternal.subjectId = id;
        }
      },
      onSelectedIcon(selectedIcon) {
        this.subjectInternal.iconClass = `${selectedIcon.css}`;
        this.displayIconManager = false;
      },
      toggleIconDisplay(shouldDisplay) {
        this.displayIconManager = shouldDisplay;
      },
      assignCustomValidation() {
        // only want to validate for a new subject, existing subjects will override
        // name and subject id
        const self = this;
        extend('uniqueName', {
          message: (field) => `${field} is already taken.`,
          validate(value) {
            if (value === self.subject.name || (value && value.localeCompare(self.subject.name, 'en', { sensitivity: 'base' }) === 0)) {
              return true;
            }
            return SubjectsService.subjectWithNameExists(self.subjectInternal.projectId, value);
          },
        });

        extend('uniqueId', {
          message: (field) => `${field} is already taken.`,
          validate(value) {
            if (value === self.subject.subjectId) {
              return true;
            }
            return SubjectsService.subjectWithIdExists(self.subjectInternal.projectId, value);
          },
        });

        extend('help_url', {
          message: (field) => `${field} must start with "/" or "http(s)"`,
          validate(value) {
            if (!value) {
              return true;
            }
            return value.startsWith('http') || value.startsWith('https') || value.startsWith('/');
          },
        });
      },
    },
  };
</script>

<style scoped>

</style>
