<template>
  <div class="modal-card">
    <header class="modal-card-head">
      <p v-if="isEdit" class="modal-card-title">Editing Existing Project</p>
      <p v-else class="modal-card-title">New Project</p>
      <button class="delete" aria-label="close" v-on:click="$parent.close()"></button>
    </header>

    <section class="modal-card-body">

      <div class="field">
        <label class="label">Project Name</label>
        <div class="control">
          <input class="input" type="text" v-model="internalProject.name" v-on:input="updateProjectId"
            v-validate="'required|min:3|max:50|uniqueName'" data-vv-delay="500" name="projectName" v-focus/>
        </div>
        <p class="help is-danger" v-show="errors.has('projectName')">{{ errors.first('projectName')}}</p>
      </div>

      <div class="field skills-remove-bottom-margin">
        <label class="label">Project ID</label>
        <div class="control">
          <input class="input" type="text" v-model="internalProject.projectId" :disabled="!canEditProjectId"
                 v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-delay="500" name="projectId"/>
        </div>
        <p class="help is-danger" v-show="errors.has('projectId')">{{ errors.first('projectId')}}</p>
      </div>
      <p class="control has-text-right">
        <help-item msg="Enable to override auto-generated ID" position="is-left"></help-item>
        <span v-on:click="toggleProject()">
          <a class="is-info" v-bind:class="{'disableControl': isEdit}" v-if="!canEditProjectId">Enable</a>
          <a class="is-info" v-if="canEditProjectId">Disable</a>
        </span>
      </p>

      <p v-if="overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </section>

    <div class="modal-card-foot skills-justify-content-right">
        <a class="button is-danger  is-outlined" v-on:click="$parent.close()">
          <span>Cancel</span>
          <span class="icon is-small">
              <i class="fas fa-stop-circle"/>
            </span>
        </a>

        <a class="button is-success is-outlined" v-on:click="updateProject"
          :disabled="errors.any() || internalProject.projectName === ''">
          <span>Save</span>
          <span class="icon is-small">
            <i class="fas fa-arrow-circle-right"/>
          </span>
        </a>
    </div>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import ProjectService from './ProjectService';
  import HelpItem from '../utils/HelpItem';

  export default {
    name: 'EditProject',
    components: { HelpItem },
    props: ['project', 'isEdit'],
    data() {
      return {
        internalProject: Object.assign({}, this.project),
        canEditProjectId: false,
        overallErrMsg: '',
        serverErrors: [],
      };
    },
    created() {
      const dictionary = {
        en: {
          attributes: {
            projectName: 'Project Name',
            projectId: 'Project ID',
          },
        },
      };
      Validator.localize(dictionary);

      if (this.isEdit) {
        Validator.extend('uniqueName', { validate: () => true });
        Validator.extend('uniqueId', { validate: () => true });
      } else {
        Validator.extend('uniqueName', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            return ProjectService.checkIfProjectNameExist(value)
              .then(remoteRes => !remoteRes);
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            return ProjectService.checkIfProjectIdExist(value)
              .then(remoteRes => !remoteRes);
          },
        }, {
          immediate: false,
        });
      }
    },
    methods: {
      updateProject() {
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.$parent.close();
            this.$emit('project-created', this.internalProject);
          }
        });
      },
      updateProjectId() {
        if (!this.isEdit && !this.canEditProjectId) {
          this.internalProject.projectId = this.internalProject.name.replace(/[^\w]/gi, '');
        }
      },
      toggleProject() {
        this.canEditProjectId = !this.canEditProjectId && !this.isEdit;
        this.updateProjectId();
      },
    },
  };
</script>

<style lang="scss" scoped>
  @import "../../styles/palette";

  .modal-card .modal-card-head {
    background-color: $green-palette-color1;
  }

  .modal-card .modal-card-head .modal-card-title {
    color: whitesmoke;
  }

  .modal-card .label {
    color: $monochrome-palette-color1;
  }

  .modal-card .modal-card-foot {
    background-color: white;
  }

  .disableField {
    pointer-events: none;
    background-color: #f0f0f0;
  }
  .disableControl {
    pointer-events: none;
    color: #a8a8a8;
  }
</style>
