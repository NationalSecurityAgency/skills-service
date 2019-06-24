<template>
  <b-modal :id="subjectInternal.subjectId" size="xl" :title="title" v-model="show" :no-close-on-backdrop="true"
           header-bg-variant="info" header-text-variant="light" no-fade>
    <b-container fluid>
      <div v-if="displayIconManager === false">
        <div class="media mb-3">
          <icon-picker :startIcon="subjectInternal.iconClass" @select-icon="toggleIconDisplay(true)"
                       class="mr-3"></icon-picker>
          <div class="media-body">
            <div class="form-group">
              <label for="subjName">Subject Name</label>
              <input type="text" class="form-control" id="subjName" @input="updateSubjectId"
                     v-model="subjectInternal.name" v-on:input="updateSubjectId"
                     v-validate="'required|min:3|max:50|uniqueName'" data-vv-delay="500" data-vv-name="subjectName"
                     v-focus>
              <small class="form-text text-danger">{{ errors.first('subjectName')}}</small>
            </div>
          </div>
        </div>

        <id-input type="text" label="Subject ID" v-model="subjectInternal.subjectId" @input="canAutoGenerateId=false"
                  v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-name="subjectId"/>
        <small class="form-text text-danger">{{ errors.first('subjectId')}}</small>

        <div class="mt-2">
          <label>Description</label>
          <markdown-editor v-model="subjectInternal.description"/>
        </div>

        <p v-if="overallErrMsg" class="text-center text-danger">***{{ overallErrMsg }}***</p>
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
        <b-button variant="success" size="sm" class="float-right" @click="updateSubject">
          Save
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close">
          Cancel
        </b-button>
      </div>
    </div>
  </b-modal>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SubjectsService from './SubjectsService';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import IdInput from '../utils/inputForm/IdInput';
  import IconManager from '../utils/iconPicker/IconManager';


  export default {
    name: 'EditSubject',
    components: {
      IdInput,
      IconPicker,
      MarkdownEditor,
      IconManager,
    },
    props: {
      subject: Object,
      isEdit: Boolean,
      value: Boolean,
    },
    data() {
      return {
        canAutoGenerateId: true,
        subjectInternal: Object.assign({}, this.subject),
        overallErrMsg: '',
        show: this.value,
        displayIconManager: false,
      };
    },
    created() {
      this.assignCustomValidation();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Subject' : 'New Subject';
      },
    },
    methods: {
      close() {
        this.show = false;
      },
      updateSubject() {
        this.$validator.validateAll()
          .then((res) => {
            if (!res) {
              this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
            } else {
              this.close();
              this.$emit('subject-saved', this.subjectInternal);
            }
          });
      },
      updateSubjectId() {
        if (!this.isEdit && this.canAutoGenerateId) {
          let id = this.subjectInternal.name.replace(/[^\w]/gi, '');
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
        const dictionary = {
          en: {
            attributes: {
              subjectName: 'Subject Name',
              subjectId: 'ID',
            },
          },
        };
        Validator.localize(dictionary);


        // only want to validate for a new subject, existing subjects will override
        // name and subject id
        const self = this;
        Validator.extend('uniqueName', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit) {
              return true;
            }
            return SubjectsService.subjectWithNameExists(self.subjectInternal.projectId, value);
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit) {
              return true;
            }
            return SubjectsService.subjectWithIdExists(self.subjectInternal.projectId, value);
          },
        }, {
          immediate: false,
        });
      },
    },
  };
</script>

<style scoped>

</style>
