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
  <b-modal :id="internalProject.projectId" :title="title" v-model="show" :no-close-on-backdrop="true"
           header-bg-variant="info" header-text-variant="light" no-fade>
    <ValidationObserver ref="observer" v-slot="{invalid}" slim>
      <b-container fluid>
        <div class="row">
          <div class="col-12">
            <div class="form-group">
              <label>Project Name</label>
              <ValidationProvider rules="required|minNameLength|maxProjectNameLength|uniqueName|customNameValidator" v-slot="{errors}" name="Project Name">
                <input class="form-control" type="text" v-model="internalProject.name" v-on:input="updateProjectId"
                       data-vv-name="projectName" v-focus/>
                <small class="form-text text-danger">{{ errors[0] }}</small>
              </ValidationProvider>
            </div>
          </div>

          <div class="col-12">
            <id-input type="text" label="Project ID" v-model="internalProject.projectId"
                      additional-validation-rules="uniqueId" @can-edit="canEditProjectId=$event"/>
          </div>
        </div>

        <p v-if="invalid && overallErrMsg" class="text-center text-danger mt-2"><small>***{{ overallErrMsg }}***</small></p>
      </b-container>
    </ValidationObserver>

    <div slot="modal-footer" class="w-100">
      <b-button variant="success" size="sm" class="float-right" @click="updateProject">
        Save
      </b-button>
      <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close">
        Cancel
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  import { Validator, ValidationProvider, ValidationObserver } from 'vee-validate';
  import ProjectService from './ProjectService';
  import IdInput from '../utils/inputForm/IdInput';
  import InputSanitizer from '../utils/InputSanitizer';

  export default {
    name: 'EditProject',
    components: { IdInput, ValidationProvider, ValidationObserver },
    props: ['project', 'isEdit', 'value'],
    data() {
      return {
        show: this.value,
        internalProject: { originalProjectId: this.project.projectId, isEdit: this.isEdit, ...this.project },
        canEditProjectId: false,
        overallErrMsg: '',
        original: {
          name: '',
          projectId: '',
        },
      };
    },
    created() {
      this.registerValidation();
    },
    mounted() {
      this.original = {
        name: this.project.name,
        projectId: this.project.projectId,
      };
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Project' : 'New Project';
      },
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      handleIdToggle(canEdit) {
        this.canEditProjectId = canEdit;
      },
      close() {
        this.show = false;
      },
      updateProject() {
        this.$refs.observer.validate().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.close();
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
      registerValidation() {
        const dictionary = {
          en: {
            attributes: {
              projectName: 'Project Name',
              projectId: 'Project ID',
            },
          },
        };
        Validator.localize(dictionary);

        const self = this;
        Validator.extend('uniqueName', {
          getMessage: (field) => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && (self.original.name === value || self.original.name.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
              return true;
            }
            return ProjectService.checkIfProjectNameExist(value)
              .then((remoteRes) => !remoteRes);
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: (field) => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.original.projectId === value) {
              return true;
            }
            return ProjectService.checkIfProjectIdExist(value)
              .then((remoteRes) => !remoteRes);
          },
        }, {
          immediate: false,
        });
      },
    },
  };
</script>

<style lang="scss" scoped>

</style>
