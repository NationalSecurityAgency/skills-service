<template>
  <div class="modal-card" style="width: 900px">
    <header class="modal-card-head">
      <p v-if="isEdit" class="modal-card-title">Editing Existing Subject</p>
      <p v-else class="modal-card-title">New Subject</p>
      <button class="delete" aria-label="close" v-on:click="$parent.close()"></button>
    </header>

    <section class="modal-card-body">

      <div class="field is-horizontal">
        <div class="field-body">
          <div class="field is-narrow">
            <icon-picker :startIcon="subject.iconClass" v-on:on-icon-selected="onSelectedIcons"></icon-picker>
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
          <markdown-editor :value="subject.description" @value-updated="updateDescription"></markdown-editor>
        </div>
      </div>

      <p v-if="overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </section>

    <footer class="modal-card-foot skills-justify-content-right">
      <a class="button is-outlined" v-on:click="$parent.close()">
        <span class="icon is-small">
          <i class="fas fa-stop-circle"/>
        </span>
        <span>Cancel</span>
      </a>

      <a class="button is-primary is-outlined" v-on:click="updateSubject" :disabled="errors.any()">
        <span class="icon is-small">
          <i class="fas fa-arrow-circle-right"/>
        </span>
        <span>Save</span>
      </a>
    </footer>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SubjectsService from './SubjectsService';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import MarkdownEditor from '../utils/MarkdownEditor';

  let self = null;

  export default {
    name: 'EditSubject',
    components: {
      IconPicker,
      MarkdownEditor,
    },
    props: ['subject', 'isEdit'],
    data() {
      return {
        canEditSubjectId: false,
        subjectInternal: Object.assign({}, this.subject),
        overallErrMsg: '',
        serverErrors: [],
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
            return SubjectsService.subjectWithNameExists(self.subject.projectId, value)
              .catch(e => this.serverErrors.push(e));
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            return SubjectsService.subjectWithIdExists(self.subject.projectId, value)
              .catch(e => this.serverErrors.push(e));
          },
        }, {
          immediate: false,
        });
      }
    },
    mounted() {
      self = this;
    },
    methods: {
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
  .disableControl {
    pointer-events: none;
    color: #a8a8a8;
  }
</style>
