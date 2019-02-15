<template>
  <div id="editSkill" class="modal-card" style="width: 1110px;">
    <header class="modal-card-head">
      <p v-if="isEdit" class="modal-card-title">Editing Existing Skill</p>
      <p v-else class="modal-card-title">New Skill</p>
      <button class="delete" aria-label="close" v-on:click="$parent.close()"></button>
    </header>

    <b-loading :is-full-page="true" :active.sync="isLoading" :can-cancel="false">
    </b-loading>
    <div v-if="isLoading" class="modal-card-body" style="height: 400px;">
    </div>

    <section class="modal-card-body" v-show="!isLoading">

      <div class="field">
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
                   position="is-left" animanted="true" type="is-light">
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
            <label class="label">Times Performed to Completion *</label>
            <p class="control is-expanded has-icons-left">
              <input class="input" type="text" v-model="skillInternal.maxSkillAchievedCount"
                     v-validate="'required|numeric|min_value:1|max_value:10000'" name="maxSkillAchievedCount"/>
              <span class="icon is-small is-left">
                    <i class="fas fa-battery-quarter"></i>
                  </span>
            </p>
            <p class="help is-danger" v-show="errors.has('maxSkillAchievedCount')">{{ errors.first('maxSkillAchievedCount')}}</p>
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
                         position="is-left" animanted="true" type="is-light">
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

      <!--</loading-container>-->
    </section>

    <footer class="modal-card-foot skills-justify-content-right">
      <a class="button is-outlined" v-on:click="$parent.close()">
        <span>Cancel</span>
        <span class="icon is-small">
                <i class="fas fa-stop-circle"/>
              </span>
      </a>

      <a class="button is-primary is-outlined" v-on:click="saveSkill" :disabled="errors.any()">
        <span>Save</span>
        <span class="icon is-small">
                <i class="fas fa-arrow-circle-right"/>
              </span>
      </a>
    </footer>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import LoadingContainer from '../utils/LoadingContainer';
  import SearchAllSkillsCheckbox from './SearchAllSkillsCheckbox';
  import SkillsService from './SkillsService';
  import MarkdownEditor from '../utils/MarkdownEditor';

  export default {
    name: 'EditSkill',
    components: { SearchAllSkillsCheckbox, LoadingContainer, MarkdownEditor },
    props: ['projectId', 'subjectId', 'skill', 'isEdit'],
    data() {
      return {
        isLoadingSkillDetails: true,
        skillInternal: {},
        canEditSkillId: false,
        overallErrMsg: '',
        serverErrors: [],
      };
    },
    mounted() {
      const self = this;
      if (this.isEdit) {
        this.loadSkillDetails();
      } else {
        this.isLoadingSkillDetails = false;
        this.skillInternal = Object.assign({}, this.skill);
      }

      const dictionary = {
        en: {
          attributes: {
            name: 'Skill Name',
            skillId: 'Skill ID',
            pointIncrement: 'Point Increment',
            pointIncrementInterval: 'Point Increment Interval',
            maxSkillAchievedCount: 'Number of Times to Complete',
            totalPoints: 'Total Points',
            helpUrl: 'Help UrL',
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
            return SkillsService.skillWithNameExists(self.projectId, value)
              .catch(e => this.serverErrors.push(e));
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: field => `The value for the ${field} is already taken.`,
          validate(value) {
            return SkillsService.skillWithIdExists(self.projectId, value)
              .catch(e => this.serverErrors.push(e));
          },
        }, {
          immediate: false,
        });
      }
    },
    computed: {
      isLoading: function isLoadingCheck() {
        return this.isLoadingSkillDetails;
      },
      totalPoints: function calculateTotalPoints() {
        if (this.skillInternal.pointIncrement && this.skillInternal.maxSkillAchievedCount) {
          const result = this.skillInternal.pointIncrement * this.skillInternal.maxSkillAchievedCount;
          if (result > 0) {
            return result;
          }
        }
        return 0;
      },
    },
    methods: {
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
          .catch((e) => {
            this.serverErrors.push(e);
            this.isLoadingSkillDetails = false;
            throw e;
        });
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
</style>
