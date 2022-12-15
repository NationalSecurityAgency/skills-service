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
    <b-modal :id="internalProject.projectId"
              :title="title"
              @hide="publishHidden"
              v-model="show"
              :no-close-on-backdrop="true"
              :centered="true"
              header-bg-variant="info"
              header-text-variant="light" no-fade
              size="xl">

      <skills-spinner :is-loading="loadingComponent"/>

      <b-container fluid v-if="!loadingComponent">
        <div class="row">
          <div class="col-12">
            <div class="form-group">
              <label for="projectIdInput">* {{ nameLabelTxt }}</label>
              <ValidationProvider rules="required|minNameLength|maxProjectNameLength|uniqueName|customNameValidator"
                                  v-slot="{errors}"
                                  name="Project Name">
                <input class="form-control" type="text" v-model="internalProject.name"
                       v-on:input="updateProjectId"
                       v-on:keydown.enter="handleSubmit(updateProject)"
                       v-focus
                       data-cy="projectName"
                        id="projectIdInput"
                      :aria-invalid="errors && errors.length > 0"
                      aria-errormessage="projectNameError"
                      aria-describedby="projectNameError"/>
                <small role="alert" class="form-text text-danger" data-cy="projectNameError" id="projectNameError">{{ errors[0] }}</small>
              </ValidationProvider>
            </div>
          </div>

          <div class="col-12">
            <id-input type="text" :label="idLabelTxt" v-model="internalProject.projectId"
                      additional-validation-rules="uniqueId" @can-edit="canEditProjectId=$event"
                      v-on:keydown.enter.native="handleSubmit(updateProject)"
                      :next-focus-el="previousFocus"
                      @shown="tooltipShowing=true"
                      @hidden="tooltipShowing=false"/>
          </div>
        </div>
        <div class="row">
          <div class="mt-2 col-12">
            <label>Description</label>
              <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{errors}"
                                  name="Project Description">
                <markdown-editor v-if="!isEdit || descriptionLoaded" v-model="internalProject.description" @input="updateDescription"></markdown-editor>
                <small role="alert" class="form-text text-danger mb-3" data-cy="projectDescriptionError">{{ errors[0] }}</small>
              </ValidationProvider>
          </div>
        </div>

        <p v-if="invalid && overallErrMsg" class="text-center text-danger mt-2" aria-live="polite"><small>***{{ overallErrMsg }}***</small></p>
      </b-container>

      <div slot="modal-footer" class="w-100">
        <b-button variant="success" size="sm" class="float-right" @click="handleSubmit(updateProject)"
                  :disabled="invalid"
                  data-cy="saveProjectButton">
          <span>{{ saveBtnTxt }}</span>
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="closeProjectButton">
          Cancel
        </b-button>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import { extend } from 'vee-validate';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import MarkdownEditor from '@/components/utils/MarkdownEditor';
  import ProjectService from './ProjectService';
  import IdInput from '../utils/inputForm/IdInput';
  import InputSanitizer from '../utils/InputSanitizer';
  import SaveComponentStateLocallyMixin from '../utils/SaveComponentStateLocallyMixin';

  export default {
    name: 'EditProject',
    components: { IdInput, MarkdownEditor, SkillsSpinner },
    mixins: [SaveComponentStateLocallyMixin, MsgBoxMixin],
    props: ['project', 'isEdit', 'value', 'isCopy'],
    data() {
      return {
        show: this.value,
        internalProject: {
          originalProjectId: this.project.projectId,
          isEdit: this.isEdit,
          ...this.project,
        },
        originalProject: {
          name: '',
          description: '',
          projectId: '',
        },
        canEditProjectId: false,
        overallErrMsg: '',
        currentFocus: null,
        previousFocus: null,
        tooltipShowing: false,
        loadingComponent: true,
        descriptionLoaded: false,
      };
    },
    created() {
      this.registerValidation();
    },
    mounted() {
      this.loadingComponent = true;
      this.descriptionLoaded = false;

      if (this.isEdit) {
        this.startLoadingFromDescription();
      } else {
        this.startLoadingFromState();
      }

      document.addEventListener('focusin', this.trackFocus);
    },
    computed: {
      title() {
        if (this.isCopy) {
          return 'Copy Project';
        }
        return this.isEdit ? 'Editing Existing Project' : 'New Project';
      },
      saveBtnTxt() {
        return this.isCopy ? 'Copy Project' : 'Save';
      },
      nameLabelTxt() {
        return this.isCopy ? 'New Project Name' : 'Project Name';
      },
      idLabelTxt() {
        return this.isCopy ? 'New Project ID' : 'Project ID';
      },
      componentName() {
        return `${this.$options.name}${this.isEdit ? 'Edit' : ''}`;
      },
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
      internalProject: {
        handler(newValue) {
          this.saveComponentState(this.componentName, newValue);
        },
        deep: true,
      },
    },
    methods: {
      startLoadingFromDescription() {
        this.originalProject = {
          name: this.project.name,
          projectId: this.project.projectId,
        };

        ProjectService.loadDescription(this.project.projectId).then((data) => {
          this.originalProject.description = data.description;
          this.startLoadingFromState();
        });
      },
      startLoadingFromState() {
        this.loadComponentState(this.componentName).then((result) => {
          if (result) {
            if (!this.isEdit || (this.isEdit && result.projectId === this.internalProject.projectId)) {
              this.internalProject = result;
            } else {
              this.internalProject = Object.assign(this.internalProject, this.originalProject);
            }
          } else {
            this.internalProject = Object.assign(this.internalProject, this.originalProject);
          }
        }).finally(() => {
          this.loadingComponent = false;
          this.descriptionLoaded = true;
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
      hasObjectChanged() {
        if (this.internalProject.name === this.originalProject.name
          && this.internalProject.description === this.originalProject.description
          && this.internalProject.projectId === this.originalProject.projectId) {
          return false;
        }
        return true;
      },
      trackFocus() {
        this.previousFocus = this.currentFocus;
        this.currentFocus = document.activeElement;
      },
      handleIdToggle(canEdit) {
        this.canEditProjectId = canEdit;
      },
      close(e) {
        this.publishHidden(e);
      },
      publishHidden(e) {
        if (!e.updated && this.hasObjectChanged()) {
          e.preventDefault();
          this.msgConfirm('You have unsaved changes.  Discard?')
            .then((res) => {
              if (res) {
                this.clearComponentState(this.componentName);
                this.hideModal(e);
              }
            });
        } else if (this.tooltipShowing && typeof e.preventDefault === 'function') {
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
      updateProject() {
        this.$refs.observer.validate()
          .then((res) => {
            if (res) {
              this.close({ updated: true });
              this.internalProject.name = InputSanitizer.sanitize(this.internalProject.name);
              this.internalProject.projectId = InputSanitizer.sanitize(this.internalProject.projectId);
              this.$emit('project-saved', this.internalProject);
            }
          });
      },
      updateProjectId() {
        if (!this.isEdit && !this.canEditProjectId) {
          this.internalProject.projectId = InputSanitizer.removeSpecialChars(this.internalProject.name);
        }
      },
      updateDescription(event) {
        this.internalProject.description = event;
      },
      registerValidation() {
        const self = this;
        extend('uniqueName', {
          message: (field) => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && (self.originalProject.name === value || self.originalProject.name.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
              return true;
            }
            return ProjectService.checkIfProjectNameExist(value)
              .then((remoteRes) => !remoteRes);
          },
        });

        extend('uniqueId', {
          message: (field) => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.originalProject.projectId === value) {
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

<style lang="scss" scoped>

</style>
