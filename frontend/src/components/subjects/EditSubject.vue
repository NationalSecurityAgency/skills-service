<template>
  <b-container fluid>

    <div class="media">
      <icon-picker :startIcon="subjectInternal.iconClass" v-on:on-icon-selected="onSelectedIcons" class="mr-3"></icon-picker>
      <div class="media-body">
        <div class="form-group">
          <label for="subjName">Subject Name</label>
          <input type="email" class="form-control" id="subjName" aria-describedby="nameHelp" placeholder="Subject Name"
                 v-model="subjectInternal.name" v-on:input="updateSubjectId"
                 v-validate="'required|min:3|max:50|uniqueName'" data-vv-delay="500" name="nameHelp" v-focus>
          <small id="nameHelp" class="form-text text-danger" v-show="errors.has('subjectName')">{{ errors.first('subjectName')}}</small>
        </div>
      </div>
    </div>

    <div class="form-group mt-2">
      <label for="subjId">Subject ID</label>
      <input type="email" class="form-control" id="subjId" aria-describedby="subjIdHelp" placeholder="Enter email"
             v-model="subjectInternal.subjectId" :disabled="!canEditSubjectId"
             v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-delay="500">
      <small id="subjIdHelp" class="form-text text-danger" v-show="errors.has('subjectId')">{{ errors.first('subjectId')}}</small>
    </div>

    <div class="text-right" style="margin-top: -1rem;">
      <i class="fas fa-question-circle mr-1 text-secondary" v-b-tooltip.hover.bottom title="Enable to override auto-generated ID." />
      <b-link v-if="!canEditSubjectId" @click="toggleSubject">Enable</b-link>
      <b-link v-else @click="toggleSubject">Disable</b-link>
    </div>

<!--    <p class="control has-text-right">-->
<!--      <b-tooltip label="Enable to override auto-generated ID."-->
<!--                 position="is-left" animanted="true" type="is-light">-->
<!--        <span><i class="fas fa-question-circle"></i></span>-->
<!--      </b-tooltip>-->
<!--      <span v-on:click="toggleSubject()">-->
<!--            <a class="is-info" v-bind:class="{'disableControl': isEdit}" v-if="!canEditSubjectId">Enable</a>-->
<!--            <a class="is-info" v-if="canEditSubjectId">Disable</a>-->
<!--          </span>-->
<!--    </p>-->

    <div class="field">
      <label class="label">Description</label>
      <div class="control">
        <markdown-editor :value="subjectInternal.description" @value-updated="updateDescription"></markdown-editor>
      </div>
    </div>

    <p v-if="overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
  </b-container>
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

      let subjectName = '';
      let subjectId = '';
      if (this.isEdit) {
        ({ subjectId } = this.subject);
        subjectName = this.subject.name;
      }

      Validator.extend('uniqueName', {
        getMessage: field => `The value for the ${field} is already taken.`,
        validate(value) {
          if (subjectName === value) {
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
          if (subjectId === value) {
            return true;
          }
          return SubjectsService.subjectWithIdExists(self.subjectInternal.projectId, value);
        },
      }, {
        immediate: false,
      });
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
        this.$validator.validateAll()
          .then((res) => {
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
