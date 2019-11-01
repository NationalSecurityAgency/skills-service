<template>
  <b-modal :id="skillInternal.skillId" size="xl" :title="title" v-model="show" :no-close-on-backdrop="true"
           header-bg-variant="info" header-text-variant="light" no-fade>
    <ValidationObserver ref="observer" v-slot="{invalid}" slim>
      <b-container fluid>
        <loading-container :is-loading="isLoading">
        <div class="row">
          <div class="col-12 col-lg">
            <div class="form-group">
              <label for="subjName">Skill Name</label>
              <ValidationProvider rules="required|minNameLength|maxSkillNameLength|uniqueName" v-slot="{errors}" name="Skill Name">
                <input type="text" class="form-control" id="subjName" @input="updateSkillId"
                       v-model="skillInternal.name" data-vv-name="name" v-focus>
                <small class="form-text text-danger">{{ errors[0] }}</small>
              </ValidationProvider>
            </div>
          </div>
          <div class="col-12 col-lg">
            <id-input type="text" label="Skill ID" additional-validation-rules="uniqueId"
                      v-model="skillInternal.skillId" @can-edit="canEditSkillId=$event"/>
          </div>
          <div class="col-12 col-lg-2 mt-2 mt-lg-0">
            <div class="form-group">
              <label>Version
                <inline-help
                  msg="An optional version for this skill to allow filtering of available skills for different versions of an application"/>
              </label>
              <ValidationProvider rules="optionalNumeric|min_value:0|maxSkillVersion|maxVersion" v-slot="{errors}" name="Version">
                <input class="form-control" type="text"
                       v-model="skillInternal.version" :disabled="isEdit"
                       data-vv-name="version"/>
                <small class="form-text text-danger">{{ errors[0]}}</small>
              </ValidationProvider>
            </div>
          </div>
        </div>
        <hr class="mb-0 pb-1 mt-4 mt-lg-0"/>
        <hr class="my-0 py-0"/>

        <div class="row mt-3">
          <div class="col-12 col-lg">
            <div class="form-group mb-1">
              <label for="subjName">Point Increment</label>
              <ValidationProvider rules="optionalNumeric|required|min_value:1|maxPointIncrement" v-slot="{errors}" name="Point Increment">
                <input class="form-control" type="text"  v-model="skillInternal.pointIncrement"
                       data-vv-name="pointIncrement"/>
                <small class="form-text text-danger">{{ errors[0]}}</small>
              </ValidationProvider>
            </div>
          </div>
          <div class="col-12 col-lg">
            <div class="form-group mt-2 mt-lg-0">
              <label for="subjName">Occurrences to Completion</label>
              <ValidationProvider rules="optionalNumeric|required|min_value:1|maxNumPerformToCompletion|moreThanMaxWindowOccurrences" v-slot="{errors}" name="Occurrences to Completion" tag="div">
                <input class="form-control" type="text" v-model="skillInternal.numPerformToCompletion"
                         data-vv-name="numPerformToCompletion" :readonly="isEdit"/>
                <small class="form-text text-danger">{{ errors[0]}}</small>
              </ValidationProvider>
            </div>
          </div>
          <div class="col-12 col-lg-3">
            <div class="form-group">
              <label for="subjName">Total Points
                <inline-help msg="Derived and can't be entered directly. Total Points = Increment x Occurrences."/>
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
              <label><b-form-checkbox id="checkbox-1" class="d-inline" v-model="skillInternal.timeWindowEnabled" v-on:input="resetTimeWindow"/>Time Window
                <inline-help msg="Uncheck to disable. When disabled skill events are applied immediately."/>

              </label>
              <div class="row">
                <div class="col-12 col-sm">
                  <ValidationProvider rules="optionalNumeric|required|min_value:0|hoursMaxTimeWindow:timeWindowMinutes|cantBe0IfMins0" vid="timeWindowHours" v-slot="{errors}" name="Hours">
                    <div class="input-group">
                      <input class="form-control d-inline" type="text" v-model="skillInternal.pointIncrementIntervalHrs"
                             data-vv-name="pointIncrementIntervalHrs"
                             value="8" :disabled="!skillInternal.timeWindowEnabled"
                             ref="timeWindowHours"/>
                      <div class="input-group-append">
                        <span class="input-group-text" id="hours-append">Hours</span>
                      </div>
                    </div>
                    <small class="form-text text-danger">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="col-12 col-sm">
                  <ValidationProvider rules="optionalNumeric|required|min_value:0|max_value:59|minutesMaxTimeWindow:timeWindowHours|cantBe0IfHours0" vid="timeWindowMinutes" v-slot="{errors}" name="Minutes">
                    <div class="input-group">
                      <input class="form-control d-inline"  type="text" v-model="skillInternal.pointIncrementIntervalMins"
                             data-vv-name="pointIncrementIntervalMins"
                             value="0" :disabled="!skillInternal.timeWindowEnabled" ref="timeWindowMinutes"/>
                      <div class="input-group-append">
                        <span class="input-group-text" id="minutes-append">Minutes</span>
                      </div>
                    </div>
                    <small class="form-text text-danger">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
              </div>

            </div>
          </div>
          <div class="col-12 col-lg">
            <ValidationProvider rules="optionalNumeric|min_value:1|lessThanTotalOccurrences|maxNumPointIncrementMaxOccurrences" v-slot="{errors}" name="Window's Max Occurrences">
              <div class="form-group">
                <label>Window's Max Occurrences
                  <inline-help
                    msg="Once this Max Occurrences has been reached, points will not be incremented until outside of the configured Time Window."/>
                </label>

                  <input class="form-control" type="text" v-model="skillInternal.numPointIncrementMaxOccurrences"
                         data-vv-name="numPointIncrementMaxOccurrences"
                         :disabled="!skillInternal.timeWindowEnabled"/>
                  <small class="form-text text-danger">{{ errors[0]}}</small>
              </div>
            </ValidationProvider>
          </div>
        </div>

          <hr class="my-0 pb-1"/>
          <hr class="mt-0 pt-0"/>

        <div class="">
          <label class="label">Description</label>
          <div class="control">
            <ValidationProvider rules="maxDescriptionLength" v-slot="{errors}" name="Skill Description">
              <markdown-editor v-if="skillInternal" v-model="skillInternal.description"/>
              <small class="form-text text-danger">{{ errors[0] }}</small>
            </ValidationProvider>
          </div>
        </div>

        <div class="form-group mt-3">
          <label>Help URL/Path
            <inline-help
              msg="If project level 'Root Help Url' is specified then this path will be relative to 'Root Help Url'"/>
          </label>
          <input class="form-control" type="text" v-model="skillInternal.helpUrl" data-vv-name="helpUrl"/>
          <small class="form-text text-danger">{{ errors.first('helpUrl')}}</small>
        </div>

        <p v-if="invalid && overallErrMsg" class="text-center text-danger">***{{ overallErrMsg }}***</p>
      </loading-container>
      </b-container>
    </ValidationObserver>

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
  import { Validator, ValidationProvider, ValidationObserver } from 'vee-validate';
  import SkillsService from './SkillsService';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import IdInput from '../utils/inputForm/IdInput';
  import InlineHelp from '../utils/InlineHelp';
  import LoadingContainer from '../utils/LoadingContainer';
  import InputSanitizer from '../utils/InputSanitizer';

  export default {
    name: 'EditSkill',
    components: {
      LoadingContainer,
      InlineHelp,
      IdInput,
      MarkdownEditor,
      ValidationProvider,
      ValidationObserver,
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
          numPerformToCompletion: 5,
          // Time Window - represented in hrs + mins;
          // 0  for both means 'disabled' and that
          // the action can be performed right away
          pointIncrementIntervalHrs: 8,
          pointIncrementIntervalMins: 0,
          timeWindowEnabled: true,
          // Max Occurrences Within Window
          numPointIncrementMaxOccurrences: 1,
          description: null,
          helpUrl: null,
        },
        canEditSkillId: false,
        initial: {
          skillId: '',
          skillName: '',
          latestVersion: 0,
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
              skillId: 'ID',
              pointIncrement: 'Point Increment',
              pointIncrementIntervalMins: 'Minutes',
              pointIncrementIntervalHrs: 'Hours',
              numPerformToCompletion: 'Number of Times to Complete',
              numPointIncrementMaxOccurrences: 'Max Occurrences Within Window',
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
            if (self.isEdit && (value === self.initial.skillName || self.initial.skillName.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
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

        Validator.extend('lessThanTotalOccurrences', {
          getMessage: () => 'Must be less than or equals to \'Occurrences to Completion\' field',
          validate(value) {
            return parseInt(self.skillInternal.numPerformToCompletion, 10) >= parseInt(value, 10);
          },
        }, {
          immediate: false,
        });
        Validator.extend('moreThanMaxWindowOccurrences', {
          getMessage: () => 'Must be more than or equals to \'Max Occurrences Within Window\' field',
          validate(value) {
            return parseInt(value, 10) >= parseInt(self.skillInternal.numPointIncrementMaxOccurrences, 10);
          },
        }, {
          immediate: false,
        });
        Validator.extend('cantBe0IfHours0', {
          getMessage: field => `${field} must be > 0 if Hours = 0`,
          validate(value) {
            if (parseInt(value, 10) > 0 || parseInt(self.skillInternal.pointIncrementIntervalHrs, 10) > 0) {
              return true;
            }
            return false;
          },
        }, {
          immediate: false,
        });
        Validator.extend('cantBe0IfMins0', {
          getMessage: field => `${field} must be > 0 if Minutes = 0`,
          validate(value) {
            if (parseInt(value, 10) > 0 || parseInt(self.skillInternal.pointIncrementIntervalMins, 10) > 0) {
              return true;
            }
            return false;
          },
        }, {
          immediate: false,
        });

        Validator.extend('maxVersion', {
          getMessage: () => `Version ${self.initial.latestVersion} is the latest; max supported version is ${self.initial.latestVersion + 1} (latest + 1)`,
          validate(value) {
            if (parseInt(value, 10) > (self.initial.latestVersion + 1)) {
              return false;
            }
            return true;
          },
        }, {
          immediate: true,
        });

        const validateWindow = (windowHours, windowMinutes) => {
          let hours = 0;
          let minutes = 0;
          if (windowHours) {
            hours = parseInt(windowHours, 10);
          }

          if (windowMinutes) {
            minutes = parseInt(windowMinutes, 10);
          }

          return ((hours * 60) + minutes) <= this.$store.getters.config.maxTimeWindowInMinutes;
        };

        Validator.extend('hoursMaxTimeWindow', {
          getMessage: () => `Time Window must be less then ${self.$store.getters.config.maxTimeWindowInMinutes / 60} hours`,
          validate(value, [otherValue]) {
            return validateWindow(value, otherValue);
          },
        }, {
          immediate: false,
          hasTarget: true,
        });
        Validator.extend('minutesMaxTimeWindow', {
          getMessage: () => `Time Window must be less then ${self.$store.getters.config.maxTimeWindowInMinutes / 60} hours`,
          validate(value, [otherValue]) {
            return validateWindow(otherValue, value);
          },
        }, {
          immediate: false,
          hasTarget: true,
        });
      },
      saveSkill() {
        this.$refs.observer.validate()
          .then((res) => {
            if (!res) {
              this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
            } else {
              this.skillInternal.name = InputSanitizer.sanitize(this.skillInternal.name);
              this.skillInternal.skillId = InputSanitizer.sanitize(this.skillInternal.skillId);
              this.skillInternal.helpUrl = InputSanitizer.sanitize(this.skillInternal.helpUrl);
              this.$emit('skill-saved', this.skillInternal);
              this.close();
            }
          });
      },
      loadSkillDetails() {
        SkillsService.getSkillDetails(this.projectId, this.subjectId, this.skillId)
          .then((loadedSkill) => {
            this.skillInternal = Object.assign({ originalSkillId: loadedSkill.skillId, isEdit: this.isEdit }, loadedSkill);
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
            this.initial.latestVersion = latestVersion;
          })
          .finally(() => {
            this.isLoadingSkillDetails = false;
          });
      },
      updateSkillId() {
        if (!this.isEdit && !this.canEditSkillId) {
          let id = this.skillInternal.name.replace(/[^\w]/gi, '');
          // Subjects, skills and badges can not have same id under a project
          // by default append Skill to avoid id collision with other entities,
          // user can always override in edit mode
          if (id) {
            id = `${id}Skill`;
          }
          this.skillInternal.skillId = id;
        }
      },
      resetTimeWindow(checked) {
        if (!checked) {
          this.skillInternal.pointIncrementIntervalHrs = 8;
          this.skillInternal.pointIncrementIntervalMins = 0;
          this.skillInternal.numPointIncrementMaxOccurrences = 1;
        }
      },
    },
  };
</script>

<style>
</style>
