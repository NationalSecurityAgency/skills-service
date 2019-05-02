<template>
  <b-modal :id="skillInternal.skillId" size="xl" :title="title" v-model="show"
           header-bg-variant="info" header-text-variant="light" no-fade>
    <b-container fluid>
      <loading-container :is-loading="isLoading">
      <div class="row">
        <div class="col-12 col-lg">
          <div class="form-group">
            <label for="subjName">Skill Name</label>
            <input type="text" class="form-control" id="subjName" @input="updateSkillId"
                   v-model="skillInternal.name"
                   v-validate="'required|min:3|max:100|uniqueName'" data-vv-delay="500" data-vv-name="name"
                   v-focus>
            <small class="form-text text-danger">{{ errors.first('name')}}</small>
          </div>
        </div>
        <div class="col-12 col-lg">

          <id-input type="text" label="Skill ID" v-model="skillInternal.skillId" @input="canAutoGenerateId=false"
                    v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-name="skillId"/>
          <small class="form-text text-danger">{{ errors.first('skillId')}}</small>
        </div>
      </div>
      <hr class="mb-0 pb-1 mt-4 mt-lg-0"/>
      <hr class="my-0 py-0"/>

      <div class="row mt-3">
        <div class="col-12 col-lg">
          <div class="form-group mb-1">
            <label for="subjName">Point Increment</label>
            <input class="form-control" type="text" v-model="skillInternal.pointIncrement"
                   v-validate="'required|numeric|min_value:1|max_value:10000'" data-vv-name="pointIncrement"/>
          </div>
          <small class="form-text text-danger">{{ errors.first('pointIncrement')}}</small>
        </div>
        <div class="col-12 col-lg">
          <div class="form-group mt-2 mt-lg-0">
            <label for="subjName">Times to Completion</label>
            <div class="input-group">
              <div class="input-group-prepend">
                <div class="input-group-text"><i class="fas fa-times"/></div>
              </div>
              <input class="form-control" type="text" v-model="skillInternal.numPerformToCompletion"
                     v-validate="'required|numeric|min_value:1|max_value:10000'" data-vv-name="numPerformToCompletion"/>
            </div>
            <small class="form-text text-danger">{{ errors.first('numPerformToCompletion')}}</small>
          </div>
        </div>
        <div class="col-12 col-lg-2">
          <div class="form-group">
            <label for="subjName">Total Points
              <inline-help msg="Total points are derived and can't be entered directly."/>
            </label>
            <div class="input-group">
              <div class="input-group-prepend">
                <div class="input-group-text"><i class="fas fa-equals"/></div>
              </div>
              <div class="form-control font-italic" style="background: #eeeeee;">{{ totalPoints | number }}</div>
            </div>
          </div>
        </div>
      </div>

      <hr class="my-0 pb-1"/>
      <hr class="mt-0 pt-0"/>

      <div class="row">
        <div class="col-12 col-lg">
          <div class="form-group">
            <label>Increment Interval (hours)
              <inline-help msg="The number of hours that must elapse between incrementing points for a user."/>
            </label>
            <input class="form-control" type="text" v-model="skillInternal.pointIncrementInterval"
                   v-validate="'required|numeric|min_value:1|max_value:10000'" data-vv-name="pointIncrementInterval"
                   value="8"/>
            <small class="form-text text-danger">{{ errors.first('pointIncrementInterval')}}</small>
          </div>
        </div>
        <div class="col-12 col-lg">
          <div class="form-group">
            <label>Version (Optional)
              <inline-help
                msg="An optional version for this skill to allow filtering of available skills for different versions of an application"/>
            </label>
            <input class="form-control" type="number" min="0" v-model="skillInternal.version" :disabled="isEdit"
                   v-validate="'min_value:0|max_value:999|numeric'" data-vv-delay="500" data-vv-name="version"/>
            <small class="form-text text-danger">{{ errors.first('version')}}</small>
          </div>
        </div>
      </div>

      <div class="">
        <label class="label">Description</label>
        <div class="control">
          <markdown-editor v-if="skillInternal" v-model="skillInternal.description"/>
        </div>
      </div>

      <div class="form-group mt-3">
        <label>Help URL</label>
        <input class="form-control" type="text" v-model="skillInternal.helpUrl"
               v-validate="'url:require_protocol'" data-vv-name="helpUrl"/>
        <small class="form-text text-danger">{{ errors.first('helpUrl')}}</small>
      </div>

      <p v-if="errors.any() && overallErrMsg" class="text-center text-danger">***{{ overallErrMsg }}***</p>
    </loading-container>
    </b-container>

    <div slot="modal-footer" class="w-100">
      <b-button variant="success" size="sm" class="float-right" @click="saveSkill" :disabled="isLoading">
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
  import SkillsService from './SkillsService';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import IdInput from '../utils/inputForm/IdInput';
  import InlineHelp from '../utils/InlineHelp';
  import LoadingContainer from '../utils/LoadingContainer';

  export default {
    name: 'EditSkill',
    components: {
      LoadingContainer,
      InlineHelp,
      IdInput,
      MarkdownEditor,
    },
    props: {
      projectId: {
        type: String,
        required: true,
      },
      subjectId: {
        type: String,
        required: true,
      },
      skillId: String,
      isEdit: {
        type: Boolean,
        required: true,
      },
      value: {
        type: Boolean,
        required: true,
      },
    },
    data() {
      return {
        isLoadingSkillDetails: true,
        skillInternal: {
          skillId: '',
          projectId: this.projectId,
          subjectId: this.subjectId,
          name: '',
          pointIncrement: 10,
          pointIncrementInterval: 8,
          numPerformToCompletion: 10,
          description: null,
          helpUrl: null,
        },
        canEditSkillId: false,
        initial: {
          skillId: '',
          skillName: '',
        },
        overallErrMsg: '',
        show: this.value,
      };
    },
    mounted() {
      if (this.isEdit) {
        this.loadSkillDetails();
      } else {
        this.skillInternal = Object.assign({ version: 0 }, this.skillInternal);
        this.findLatestSkillVersion();
      }
      this.setupValidation();
    },
    computed: {
      isLoading() {
        return this.isLoadingSkillDetails;
      },
      totalPoints() {
        if (this.skillInternal.pointIncrement && this.skillInternal.numPerformToCompletion) {
          const result = this.skillInternal.pointIncrement * this.skillInternal.numPerformToCompletion;
          if (result > 0) {
            return result;
          }
        }
        return 0;
      },
      title() {
        return this.isEdit ? 'Editing Existing Skill' : 'New Skill';
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
      setupValidation() {
        const dictionary = {
          en: {
            attributes: {
              name: 'Skill Name',
              skillId: 'Skill ID',
              pointIncrement: 'Point Increment',
              pointIncrementInterval: 'Point Increment Interval',
              numPerformToCompletion: 'Number of Times to Complete',
              totalPoints: 'Total Points',
              helpUrl: 'Help UrL',
              version: 'Version',
            },
          },
        };
        Validator.localize(dictionary);

        const self = this;
        Validator.extend('uniqueName', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.initial.skillName === value) {
              return true;
            }
            return SkillsService.skillWithNameExists(self.projectId, value);
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.initial.skillId === value) {
              return true;
            }
            return SkillsService.skillWithIdExists(self.projectId, value);
          },
        }, {
          immediate: false,
        });
      },
      saveSkill() {
        this.$validator.validateAll()
          .then((res) => {
            if (!res) {
              this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
            } else {
              this.$emit('skill-saved', this.skillInternal);
              this.close();
            }
          });
      },
      loadSkillDetails() {
        SkillsService.getSkillDetails(this.projectId, this.subjectId, this.skillId)
          .then((loadedSkill) => {
            this.skillInternal = loadedSkill;
            this.initial.skillId = this.skillInternal.skillId;
            this.initial.skillName = this.skillInternal.name;
          })
          .finally(() => {
            this.isLoadingSkillDetails = false;
          });
      },
      findLatestSkillVersion() {
        SkillsService.getLatestSkillVersion(this.projectId)
          .then((latestVersion) => {
            this.skillInternal.version = latestVersion;
          })
          .finally(() => {
            this.isLoadingSkillDetails = false;
          });
      },
      updateSkillId() {
        if (!this.isEdit && !this.canEditSkillId) {
          this.skillInternal.skillId = this.skillInternal.name.replace(/[^\w]/gi, '');
        }
      },
    },
  };
</script>

<style>
  .skills-enable-control {
    font-size: 0.8rem;
    font-weight: lighter;
  }

  .markdown-preview ul {
    list-style: unset;
  }

  .markdown-info {
    cursor: pointer;
  }

  .disableControl {
    pointer-events: none;
    color: #a8a8a8;
  }

  .field.operator {
    margin-top: 2.2rem;
    text-align: center;
  }

  .skills-point {
    width: 25%;
  }

  #total-points-field {
    border-right: 1px solid lightgray;
  }

  .input.total-points {
    border: none;
    box-shadow: none;
  }

  #version-field {
    width: 8%;
  }
</style>
