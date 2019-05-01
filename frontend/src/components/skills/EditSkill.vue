<template>
  <b-modal :id="skillInternal.skillId" size="xl" :title="title" v-model="show"
           header-bg-variant="info" header-text-variant="light" no-fade>
    <!--  <modal :title="title" @cancel-clicked="closeMe" @save-clicked="saveSkill">-->
    <b-container fluid>
      <div class="form-group">
        <label for="subjName">Skill Name</label>
        <input type="text" class="form-control" id="subjName" @input="updateSubjectId"
               v-model="skillInternal.name" v-on:input="updateSkillId"
               v-validate="'required|min:3|max:100|uniqueName'" data-vv-delay="500" data-vv-name="name"
               v-focus>
        <small class="form-text text-danger">{{ errors.first('name')}}</small>
      </div>
      <id-input type="text" label="Skill ID" v-model="skillInternal.skillId" @input="canAutoGenerateId=false"
                v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-name="skillId"/>
      <small class="form-text text-danger">{{ errors.first('skillId')}}</small>


      <!--      <div class="field skills-remove-bottom-margin">-->
      <!--        <label class="label">Skill ID *</label>-->
      <!--        <div class="control">-->
      <!--          <input class="input" type="text" v-model="skillInternal.skillId" :disabled="!canEditSkillId"-->
      <!--                 v-validate="'required|min:3|max:50|alpha_num|uniqueId'" data-vv-delay="500" name="skillId"/>-->
      <!--        </div>-->
      <!--        <p class="help is-danger" v-show="errors.has('skillId')">{{ errors.first('skillId')}}</p>-->
      <!--      </div>-->
      <!--      <p class="control has-text-right skills-enable-control">-->
      <!--        <b-tooltip label="Enable to override auto-generated ID."-->
      <!--                   position="is-left" animated type="is-light">-->
      <!--          <span><i class="fas fa-question-circle"></i></span>-->
      <!--        </b-tooltip>-->
      <!--        <span v-on:click="toggleSkill()">-->
      <!--          <a class="is-info" v-bind:class="{'disableControl': isEdit}" v-if="!canEditSkillId">Enable</a>-->
      <!--          <a class="is-info" v-if="canEditSkillId">Disable</a>-->
      <!--        </span>-->
      <!--      </p>-->

      <!--      <div class="text-center mt-3">-->
      <!--        Increment <i class="fa fa-times mx-2"/> Times to Completion <i class="fa fa-equals mx-2"/> Total Points-->
      <!--      </div>-->

      <!--      <div class="row mt-3">-->
      <!--        <div class="col">-->
      <!--            <input class="form-control" type="text" v-model="skillInternal.pointIncrement"-->
      <!--                   v-validate="'required|numeric|min_value:1|max_value:10000'" data-vv-name="pointIncrement"/>-->
      <!--        </div>-->
      <!--        <div class="col-1 text-center"> <i class="fa fa-times"/> </div>-->
      <!--        <div class="col">-->
      <!--            <input class="form-control" type="text" v-model="skillInternal.numPerformToCompletion"-->
      <!--                   v-validate="'required|numeric|min_value:1|max_value:10000'" data-vv-name="numPerformToCompletion"/>-->
      <!--            <small class="form-text text-danger">{{ errors.first('numPerformToCompletion')}}</small>-->
      <!--        </div>-->
      <!--        <div class="col-1"> <i class="fa fa-equals"/> </div>-->
      <!--        <div class="col-2">-->
      <!--          <div class="form-control text-secondary text-center">{{totalPoints}}</div>-->

      <!--&lt;!&ndash;            <input class="form-control" type="text" v-model="totalPoints"/>&ndash;&gt;-->
      <!--        </div>-->
      <!--      </div>-->

      <hr class="mb-0 pb-1 mt-5 mt-lg-3"/>
      <hr class="my-0 py-0"/>

      <div class="row mt-3">
        <div class="col-12 col-lg">
          <div class="form-group">
            <label for="subjName">Point Increment</label>
            <input class="form-control" type="text" v-model="skillInternal.pointIncrement"
                   v-validate="'required|numeric|min_value:1|max_value:10000'" data-vv-name="pointIncrement"/>
            <small class="form-text text-danger">{{ errors.first('pointIncrement')}}</small>
          </div>
        </div>
        <div class="col-12 col-lg-1 text-center"><i class="fa fa-times"/></div>
        <div class="col-12 col-lg">
          <div class="form-group">
            <label for="subjName">Times to Completion</label>
            <input class="form-control" type="text" v-model="skillInternal.numPerformToCompletion"
                   v-validate="'required|numeric|min_value:1|max_value:10000'" data-vv-name="numPerformToCompletion"/>
            <small class="form-text text-danger">{{ errors.first('numPerformToCompletion')}}</small>
          </div>
        </div>
        <div class="col-12 col-lg-1 text-center"><i class="fa fa-equals"/></div>
        <div class="col-12 col-lg-2">
          <div class="form-group">
            <label for="subjName">Total Points</label>
            <div class="form-control">{{totalPoints}}</div>
          </div>
        </div>
      </div>

      <hr class="my-0 pb-1"/>
      <hr class="mt-0 pt-0"/>

      <div class="form-group">
        <label>Increment Interval (hours)</label>
        <input class="form-control" type="text" v-model="skillInternal.pointIncrementInterval"
               v-validate="'required|numeric|min_value:1|max_value:10000'" data-vv-name="pointIncrementInterval"
               value="8"/>
        <small class="form-text text-danger">{{ errors.first('pointIncrementInterval')}}</small>
      </div>

      <div class="form-group">
        <label>Version (Optional)</label>
        <input class="form-control" type="number" min="0" v-model="skillInternal.version" :disabled="isEdit"
               v-validate="'min_value:0|max_value:999|numeric'" data-vv-delay="500" data-vv-name="version"/>
        <small class="form-text text-danger">{{ errors.first('version')}}</small>
      </div>


      <!--      <div class="field is-horizontal skills-pad-top-1-rem">-->
      <!--        <div class="field-body">-->
      <!--          <div class="field skills-point">-->
      <!--            <label class="label">Point Increment *</label>-->
      <!--            <p class="control is-expanded has-icons-left">-->
      <!--              <input class="input" type="text" v-model="skillInternal.pointIncrement"-->
      <!--                     v-validate="'required|numeric|min_value:1|max_value:10000'" name="pointIncrement"/>-->
      <!--              <span class="icon is-small is-left">-->
      <!--                    <i class="fas fa-battery-quarter"></i>-->
      <!--                  </span>-->
      <!--            </p>-->
      <!--            <p class="help is-danger" v-show="errors.has('pointIncrement')">{{ errors.first('pointIncrement')}}</p>-->
      <!--          </div>-->

      <!--          <div class="operator field">-->
      <!--            x-->
      <!--            &lt;!&ndash;<p class="help is-danger" v-show="false"></p>&ndash;&gt;-->
      <!--          </div>-->

      <!--          <div class="field skills-point">-->
      <!--            <label class="label">Times Perform to Completion *</label>-->
      <!--            <p class="control is-expanded has-icons-left">-->
      <!--              <input class="input" type="text" v-model="skillInternal.numPerformToCompletion"-->
      <!--                     v-validate="'required|numeric|min_value:1|max_value:10000'" name="numPerformToCompletion"/>-->
      <!--              <span class="icon is-small is-left">-->
      <!--                    <i class="fas fa-battery-quarter"></i>-->
      <!--                  </span>-->
      <!--            </p>-->
      <!--            <p class="help is-danger" v-show="errors.has('numPerformToCompletion')">{{-->
      <!--              errors.first('numPerformToCompletion')}}</p>-->
      <!--          </div>-->

      <!--          <div class="operator field">-->
      <!--            =-->
      <!--            <p class="help is-danger" v-show="false"></p>-->
      <!--          </div>-->

      <!--          <div class="field" id="total-points-field">-->
      <!--            <label class="label">Total Points</label>-->
      <!--            <p class="control is-expanded has-icons-left">-->
      <!--              <span class="input total-points">{{ totalPoints }}</span>-->
      <!--              <span class="icon is-small is-left">-->
      <!--                    <i class="fas fa-battery-full"></i>-->
      <!--                  </span>-->
      <!--            </p>-->
      <!--          </div>-->


      <!--          <div class="field skills-point">-->
      <!--            <label class="label">Increment Interval (hours) *-->
      <!--              <b-tooltip label="The number of hours that must elapse between incrementing points for a user."-->
      <!--                         position="is-left" animated type="is-light">-->
      <!--                <span><i class="fas fa-question-circle"></i></span>-->
      <!--              </b-tooltip>-->
      <!--            </label>-->
      <!--            <p class="control is-expanded has-icons-left">-->
      <!--              <input class="input" type="text" v-model="skillInternal.pointIncrementInterval"-->
      <!--                     v-validate="'required|numeric|min_value:1|max_value:10000'" name="pointIncrementInterval"-->
      <!--                     value="8"/>-->
      <!--              <span class="icon is-small is-left">-->
      <!--                    <i class="fas fa-clock"></i>-->
      <!--                  </span>-->
      <!--            </p>-->
      <!--            <p class="help is-danger" v-show="errors.has('pointIncrementInterval')">{{-->
      <!--              errors.first('pointIncrementInterval')}}</p>-->
      <!--          </div>-->

      <!--        </div>-->
      <!--      </div>-->

      <!--      <div class="field skills-remove-bottom-margin">-->
      <!--        <label class="label">Version-->
      <!--          <b-tooltip-->
      <!--            label="An optional version for this skill to allow filtering of available skills for different versions of an application"-->
      <!--            position="is-right" animated type="is-light">-->
      <!--            <span><i class="fas fa-question-circle"></i></span>-->
      <!--          </b-tooltip>-->
      <!--        </label>-->
      <!--        <div class="control" id="version-field">-->
      <!--          <input class="input" type="number" min="0" v-model="skillInternal.version" :disabled="isEdit"-->
      <!--                 v-validate="'min_value:0|max_value:999|numeric'" data-vv-delay="500" name="version"/>-->
      <!--        </div>-->
      <!--        <p class="help is-danger" v-show="errors.has('version')">{{ errors.first('version')}}</p>-->
      <!--      </div>-->

      <div class="field skills-pad-top-1-rem">
        <label class="label">Description</label>
        <div class="control">
          <markdown-editor v-model="skillInternal.description"/>
        </div>
      </div>


      <div class="form-group mt-3">
        <label>Help URL</label>
        <input class="form-control" type="text" v-model="skillInternal.helpUrl"
               v-validate="'url:require_protocol'" data-vv-name="helpUrl"/>
        <small class="form-text text-danger">{{ errors.first('helpUrl')}}</small>
      </div>

      <!--      <div class="field">-->
      <!--        <label class="label">Help URL</label>-->
      <!--        <div class="control has-icons-left">-->
      <!--          <input class="input" type="text" v-model="skillInternal.helpUrl"-->
      <!--                 v-validate="'url:require_protocol'" name="helpUrl"/>-->
      <!--          <span class="icon is-small is-left">-->
      <!--                <i class="fas fa-link"></i>-->
      <!--              </span>-->
      <!--        </div>-->
      <!--        <p class="help is-danger" v-show="errors.has('helpUrl')">{{ errors.first('helpUrl')}}</p>-->
      <!--      </div>-->

      <p v-if="errors.any() && overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </b-container>
  </b-modal>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SkillsService from './SkillsService';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import IdInput from '../utils/inputForm/IdInput';

  export default {
    name: 'EditSkill',
    components: {
      IdInput,
      MarkdownEditor,
    },
    props: ['projectId', 'subjectId', 'skillId', 'isEdit', 'value'],
    data() {
      return {
        isLoadingSkillDetails: true,
        skillInternal: {},
        canEditSkillId: false,
        overallErrMsg: '',
        show: this.value,
      };
    },
    mounted() {
      if (this.isEdit) {
        this.loadSkillDetails();
      } else {
        this.skillInternal = Object.assign({ version: 0 }, this.skill);
        this.findLatestSkillVersion();
      }
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
        const self = this;
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
          skillName = this.skillInternal.name;
          ({ skillId } = this.skill);
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
      saveSkill() {
        this.$validator.validateAll()
          .then((res) => {
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
        SkillsService.getSkillDetails(this.projectId, this.subjectId, this.skillId)
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
        SkillsService.getLatestSkillVersion(this.skillInternal.projectId)
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
