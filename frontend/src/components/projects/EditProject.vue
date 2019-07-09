<template>
  <b-modal :id="internalProject.projectId" :title="title" v-model="show" :no-close-on-backdrop="true"
           header-bg-variant="info" header-text-variant="light" no-fade>
    <b-container fluid>
      <div class="row">
        <div class="col-12">
          <div class="form-group">
            <label>Project Name</label>
            <input class="form-control" type="text" v-model="internalProject.name" v-on:input="updateProjectId"
                   v-validate="'required|min:3|max:50|uniqueName'" data-vv-delay="750" data-vv-name="projectName" v-focus/>
            <small class="form-text text-danger">{{ errors.first('projectName')}}</small>
          </div>
        </div>

        <div class="col-12">
          <id-input type="text" label="Project ID" v-model="internalProject.projectId"
                    v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-name="projectId" data-vv-delay="750"/>
          <small class="form-text text-danger">{{ errors.first('projectId')}}</small>
        </div>
      </div>

      <p v-if="overallErrMsg" class="text-center text-danger mt-2"><small>***{{ overallErrMsg }}***</small></p>
    </b-container>


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
  import { Validator } from 'vee-validate';
  import ProjectService from './ProjectService';
  import IdInput from '../utils/inputForm/IdInput';

  export default {
    name: 'EditProject',
    components: { IdInput },
    props: ['project', 'isEdit', 'value'],
    data() {
      return {
        show: this.value,
        internalProject: Object.assign({}, this.project),
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
      close() {
        this.show = false;
      },
      updateProject() {
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.close();
            this.$emit('project-saved', this.internalProject);
          }
        });
      },
      updateProjectId() {
        if (!this.isEdit && !this.canEditProjectId) {
          this.internalProject.projectId = this.internalProject.name.replace(/[^\w]/gi, '');
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
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.original.name === value) {
              return true;
            }
            return ProjectService.checkIfProjectNameExist(value)
              .then(remoteRes => !remoteRes);
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.original.projectId === value) {
              return true;
            }
            return ProjectService.checkIfProjectIdExist(value)
              .then(remoteRes => !remoteRes);
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
