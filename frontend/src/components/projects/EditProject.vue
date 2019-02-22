<template>
  <modal :title="title" @cancel-clicked="closeMe" @save-clicked="updateProject">
    <template slot="content">
      <div class="field" style="width: 500px">
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
          <a class="is-info" v-if="!canEditProjectId">Enable</a>
          <a class="is-info" v-else>Disable</a>
        </span>
      </p>

      <p v-if="overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </template>
  </modal>
</template>

<script>
  import { Validator } from 'vee-validate';
  import ProjectService from './ProjectService';
  import HelpItem from '../utils/HelpItem';
  import Modal from '../utils/modal/Modal';

  export default {
    name: 'EditProject',
    components: { Modal, HelpItem },
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
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Project' : 'New Project';
      },
    },
    methods: {
      closeMe() {
        this.$parent.close();
      },
      updateProject() {
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.closeMe();
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
        this.canEditProjectId = !this.canEditProjectId;
        this.updateProjectId();
      },
    },
  };
</script>

<style lang="scss" scoped>

</style>
