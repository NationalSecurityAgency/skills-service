<template>
  <modal :title="title" @cancel-clicked="closeMe" @save-clicked="updateSubject">
    <template slot="content">
      <div class="field is-horizontal" style="width: 720px;">
        <div class="field-body">
          <div class="field is-narrow">
            <icon-picker :startIcon="subjectInternal.iconClass" v-on:on-icon-selected="onSelectedIcons"></icon-picker>
          </div>
          <div class="field">
            <label class="label">Subject Name</label>
            <div class="control">
              <input class="input" type="text" v-model="subjectInternal.name" v-on:input="updateSubjectId"
                     v-validate="'required|min:3|max:50|uniqueName'" data-vv-delay="500" name="subjectName" v-focus/>
            </div>
            <p class="help is-danger" v-show="errors.has('subjectName')">{{ errors.first('subjectName')}}</p>
          </div>
        </div>
      </div>


      <div class="field skills-remove-bottom-margin">
        <label class="label">Subject ID</label>
        <div class="control">
          <input class="input" type="text" v-model="subjectInternal.subjectId" :disabled="!canEditSubjectId"
                 v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-delay="500" name="subjectId"/>
        </div>
        <p class="help is-danger" v-show="errors.has('subjectId')">{{ errors.first('subjectId')}}</p>
      </div>
      <p class="control has-text-right">
        <b-tooltip label="Enable to override auto-generated ID."
                   position="is-left" animanted="true" type="is-light">
          <span><i class="fas fa-question-circle"></i></span>
        </b-tooltip>
        <span v-on:click="toggleSubject()">
            <a class="is-info" v-bind:class="{'disableControl': isEdit}" v-if="!canEditSubjectId">Enable</a>
            <a class="is-info" v-if="canEditSubjectId">Disable</a>
          </span>
      </p>

      <div class="field">
        <label class="label">Description</label>
        <div class="control">
          <markdown-editor :value="subjectInternal.description" @value-updated="updateDescription"></markdown-editor>
        </div>
      </div>

      <p v-if="overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </template>
  </modal>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SubjectsService from './SubjectsService';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import Modal from '../utils/modal/Modal';

  let self = null;

  export default {
    name: 'EditSubject',
    components: {
      Modal,
      IconPicker,
      MarkdownEditor,
    },
    props: ['subject', 'isEdit'],
    data() {
      return {
        canEditSubjectId: false,
        subjectInternal: Object.assign({}, this.subject),
        overallErrMsg: '',
      };
    },
    created() {
      const dictionary = {
        en: {
          attributes: {
            subjectName: 'Subject Name',
            subjectId: 'Subject ID',
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
            return SubjectsService.subjectWithNameExists(self.subjectInternal.projectId, value);
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            return SubjectsService.subjectWithIdExists(self.subjectInternal.projectId, value);
          },
        }, {
          immediate: false,
        });
      }
    },
    mounted() {
      self = this;
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Subject' : 'New Subject';
      },
    },
    methods: {
      closeMe() {
        this.$parent.close();
      },
      updateDescription(value) {
        this.subjectInternal.description = value.value;
      },
      updateSubject() {
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.$parent.close();
            this.$emit('subject-created', this.subjectInternal);
          }
        });
      },
      updateSubjectId() {
        if (!this.isEdit && !this.canEditSubjectId) {
          this.subjectInternal.subjectId = this.subjectInternal.name.replace(/[^\w]/gi, '');
        }
      },
      onSelectedIcons(selectedIconCss) {
        this.subjectInternal.iconClass = selectedIconCss;
      },
      toggleSubject() {
        this.canEditSubjectId = !this.canEditSubjectId && !this.isEdit;
        this.updateSubjectId();
      },
    },
  };
</script>

<style scoped>

</style>
