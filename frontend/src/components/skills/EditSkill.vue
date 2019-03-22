<template>
  <modal :title="title" @cancel-clicked="closeMe" @save-clicked="saveSkill">
    <template slot="content">
      <div class="field" style="width: 1110px;">
        <label class="label">Skill Name *</label>
        <div class="control">
          <input class="input" type="text" v-model="skillInternal.name" v-on:input="updateSkillId"
                 v-validate="'required|min:3|max:100|uniqueName'" data-vv-delay="500" name="name" v-focus/>
        </div>
        <p class="help is-danger" v-show="errors.has('name')">{{ errors.first('name')}}</p>
      </div>

      <div class="field skills-remove-bottom-margin">
        <label class="label">Skill ID *</label>
        <div class="control">
          <input class="input" type="text" v-model="skillInternal.skillId" :disabled="!canEditSkillId"
                 v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-delay="500" name="skillId"/>
        </div>
        <p class="help is-danger" v-show="errors.has('skillId')">{{ errors.first('skillId')}}</p>
      </div>
      <p class="control has-text-right skills-enable-control">
        <b-tooltip label="Enable to override auto-generated ID."
                   position="is-left" animated type="is-light">
          <span><i class="fas fa-question-circle"></i></span>
        </b-tooltip>
        <span v-on:click="toggleSkill()">
          <a class="is-info" v-bind:class="{'disableControl': isEdit}" v-if="!canEditSkillId">Enable</a>
          <a class="is-info" v-if="canEditSkillId">Disable</a>
        </span>
      </p>

      <div class="field is-horizontal skills-pad-top-1-rem">
        <div class="field-body">
          <div class="field skills-point">
            <label class="label">Point Increment *</label>
            <p class="control is-expanded has-icons-left">
              <input class="input" type="text" v-model="skillInternal.pointIncrement"
                     v-validate="'required|numeric|min_value:1|max_value:10000'" name="pointIncrement"/>
              <span class="icon is-small is-left">
                    <i class="fas fa-battery-quarter"></i>
                  </span>
            </p>
            <p class="help is-danger" v-show="errors.has('pointIncrement')">{{ errors.first('pointIncrement')}}</p>
          </div>

          <div class="operator field">
            x
            <!--<p class="help is-danger" v-show="false"></p>-->
          </div>

          <div class="field skills-point">
            <label class="label">Times Perform to Completion *</label>
            <p class="control is-expanded has-icons-left">
              <input class="input" type="text" v-model="skillInternal.numPerformToCompletion"
                     v-validate="'required|numeric|min_value:1|max_value:10000'" name="numPerformToCompletion"/>
              <span class="icon is-small is-left">
                    <i class="fas fa-battery-quarter"></i>
                  </span>
            </p>
            <p class="help is-danger" v-show="errors.has('numPerformToCompletion')">{{ errors.first('numPerformToCompletion')}}</p>
          </div>

          <div class="operator field">
            =
            <p class="help is-danger" v-show="false"></p>
          </div>

          <div class="field" id="total-points-field">
            <label class="label">Total Points</label>
            <p class="control is-expanded has-icons-left">
              <span class="input total-points">{{ totalPoints }}</span>
              <span class="icon is-small is-left">
                    <i class="fas fa-battery-full"></i>
                  </span>
            </p>
          </div>


          <div class="field skills-point">
            <label class="label">Increment Interval (hours) *
              <b-tooltip label="The number of hours that must elapse between incrementing points for a user."
                         position="is-left" animated type="is-light">
                <span><i class="fas fa-question-circle"></i></span>
              </b-tooltip>
            </label>
            <p class="control is-expanded has-icons-left">
              <input class="input" type="text" v-model="skillInternal.pointIncrementInterval"
                     v-validate="'required|numeric|min_value:1|max_value:10000'" name="pointIncrementInterval"
                     value="8"/>
              <span class="icon is-small is-left">
                    <i class="fas fa-clock"></i>
                  </span>
            </p>
            <p class="help is-danger" v-show="errors.has('pointIncrementInterval')">{{
              errors.first('pointIncrementInterval')}}</p>
          </div>

        </div>
      </div>

      <div class="field skills-remove-bottom-margin">
        <label class="label">Version
          <b-tooltip
            label="An optional version for this skill to allow filtering of available skills for different versions of an application"
            position="is-right" animated type="is-light">
            <span><i class="fas fa-question-circle"></i></span>
          </b-tooltip>
        </label>
        <div class="control" id="version-field">
          <input class="input" type="number" min="0" v-model="skillInternal.version" :disabled="isEdit"
                 v-validate="'min_value:0|max_value:999|numeric'" data-vv-delay="500" name="version"/>
        </div>
        <p class="help is-danger" v-show="errors.has('version')">{{ errors.first('version')}}</p>
      </div>

      <div class="field skills-pad-top-1-rem">
        <label class="label">Description</label>
        <div class="control">
          <markdown-editor :value="skill.description" @value-updated="updateDescription"></markdown-editor>
        </div>
      </div>

      <div class="field">
        <label class="label">Help URL</label>
        <div class="control has-icons-left">
          <input class="input" type="text" v-model="skillInternal.helpUrl"
                 v-validate="'url:require_protocol'" name="helpUrl"/>
          <span class="icon is-small is-left">
                <i class="fas fa-link"></i>
              </span>
        </div>
        <p class="help is-danger" v-show="errors.has('helpUrl')">{{ errors.first('helpUrl')}}</p>
      </div>

      <p v-if="errors.any() && overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </template>
  </modal>
</template>

<script>
  import { Validator } from 'vee-validate';
  import LoadingContainer from '../utils/LoadingContainer';
  import SearchAllSkillsCheckbox from './SearchAllSkillsCheckbox';
  import SkillsService from './SkillsService';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import Modal from '../utils/modal/Modal';

  export default {
    name: 'EditSkill',
    components: { Modal, SearchAllSkillsCheckbox, LoadingContainer, MarkdownEditor },
    props: ['projectId', 'subjectId', 'skill', 'isEdit'],
    data() {
      return {
        isLoadingSkillDetails: true,
        skillInternal: {},
        canEditSkillId: false,
        overallErrMsg: '',
      };
    },
    mounted() {
      const self = this;
      if (this.isEdit) {
        this.loadSkillDetails();
      } else {
        this.skillInternal = Object.assign({ version: 0 }, this.skill);
        this.findLatestSkillVersion();
      }

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

      let skillName = '';
      let skillId = '';
      if (this.isEdit) {
        skillName = this.skill.name;
        skillId = this.skill.skillId;
      }

      Validator.extend('uniqueName', {
        getMessage: field => `The value for the ${field} is already taken.`,
        validate(value) {
          if (skillName === value) {
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
          if (skillId === value) {
            return true;
          }
          return SkillsService.skillWithIdExists(self.projectId, value);
        },
      }, {
        immediate: false,
      });
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
    methods: {
      closeMe() {
        this.$parent.close();
      },
      updateDescription(value) {
        this.skillInternal.description = value.value;
      },
      saveSkill() {
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.$emit('skill-created', this.skillInternal);
            this.$parent.close();
          }
        });
      },
      updateSkillId() {
        if (!this.isEdit && !this.canEditSkillId) {
          this.skillInternal.skillId = this.skillInternal.name.replace(/[^\w]/gi, '');
        }
      },

      loadSkillDetails() {
        SkillsService.getSkillDetails(this.projectId, this.subjectId, this.skill.skillId)
          .then((loadedSkill) => {
            this.skillInternal = loadedSkill;
            this.isLoadingSkillDetails = false;
          })
          .finally(() => {
            this.isLoadingSkillDetails = false;
        });
      },
      findLatestSkillVersion() {
        // let myLatestVersion = 0;
        SkillsService.getLatestSkillVersion(this.projectId)
          .then((latestVersion) => {
            this.skillInternal.version = latestVersion;
            this.isLoadingSkillDetails = false;
          })
          .finally(() => {
            this.isLoadingSkillDetails = false;
        });
        // this.skillInternal.version = myLatestVersion;
      },
      toggleSkill() {
        this.canEditSkillId = !this.canEditSkillId && !this.isEdit;
        this.updateSkillId();
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
