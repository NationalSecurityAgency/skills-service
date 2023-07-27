/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
    <b-modal :id="badgeInternal.badgeId" size="xl" :title="title" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="publishHidden"
             header-text-variant="light" no-fade>

      <skills-spinner :is-loading="loadingComponent"/>

      <b-container fluid v-if="!loadingComponent">
        <ReloadMessage v-if="restoredFromStorage" @discard-changes="discardChanges" />
        <div v-if="displayIconManager === false" class="text-left">
          <div class="media">
            <icon-picker :startIcon="badgeInternal.iconClass" @select-icon="toggleIconDisplay(true, false)"
                         class="mr-3"></icon-picker>
            <div class="media-body">
              <div class="form-group">
                <label for="badgeName">* Badge Name</label>
                <ValidationProvider rules="required|minNameLength|maxBadgeNameLength|uniqueName|customNameValidator"
                                    v-slot="{errors}" name="Badge Name" :debounce="250">
                  <input v-focus class="form-control" id="badgeName" type="text" v-model="badgeInternal.name"
                         @input="updateBadgeId" aria-required="true" data-cy="badgeName"
                         v-on:keydown.enter="handleSubmit(updateBadge)"
                         :aria-invalid="errors && errors.length > 0"
                         aria-errormessage="badgeNameError"
                         aria-describedby="badgeNameError"/>
                  <small role="alert" class="form-text text-danger" v-show="errors[0]" data-cy="badgeNameError" id="badgeNameError">{{ errors[0] }}
                  </small>
                </ValidationProvider>
              </div>
            </div>
          </div>

          <id-input type="text" label="Badge ID" v-model="badgeInternal.badgeId" @input="canAutoGenerateId=false"
                    additional-validation-rules="uniqueId" v-on:keydown.enter.native="handleSubmit(updateBadge)"
                    :next-focus-el="previousFocus"
                    @shown="tooltipShowing=true"
                    @hidden="tooltipShowing=false"/>

          <div class="mt-3">
            <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{errors}"
                                name="Badge Description">
              <markdown-editor v-model="badgeInternal.description"
                               :project-id="badgeInternal.projectId"
                               :skill-id="isEdit ? badgeInternal.skillId : null"
                               @input="updateDescription"></markdown-editor>
              <small role="alert" class="form-text text-danger mb-3" data-cy="badgeDescriptionError">{{ errors[0] }}</small>
            </ValidationProvider>
          </div>

          <b-card class="mt-1" v-if="!global" data-cy="bonusAwardCard">>
            <div :class="{'form-group': badgeInternal.timeLimitEnabled}">
              <label>
                <b-form-checkbox data-cy="timeLimitCheckbox" id="checkbox-1" class="d-inline" v-model="badgeInternal.timeLimitEnabled" v-on:input="resetTimeLimit"/>Enable Bonus Award
                <inline-help
                  target-id="timeLimitHelp"
                  :next-focus-el="previousFocus"
                  @shown="tooltipShowing=true"
                  @hidden="tooltipShowing=false"
                  :msg="badgeInternal.timeLimitEnabled ? 'Uncheck to disable. When disabled, there is no reward for completing a badge within the time limit.' : 'Check to enable. When enabled, users will be rewarded for completing the badge within the time limit.'"/>
              </label>
              <div class="row" style="padding-bottom: 10px;" v-if="badgeInternal.timeLimitEnabled">
                <div class="text-left col">
                  <div class="media">
                    <icon-picker :startIcon="badgeInternal.awardAttrs.iconClass" class="mr-3" @select-icon="toggleIconDisplay(true, true)" :disabled="!badgeInternal.timeLimitEnabled"></icon-picker>
                    <div class="media-body">
                      <div class="form-group">
                        <label for="awardName">Award Name</label>
                        <ValidationProvider rules="required|minNameLength|maxBadgeNameLength|customNameValidator"
                                            v-slot="{errors}" name="Award Name" :debounce="250">
                          <input v-focus class="form-control" id="awardName" type="text" v-model="badgeInternal.awardAttrs.name"
                                 aria-required="true" data-cy="awardName"
                                 v-on:keydown.enter="handleSubmit(updateBadge)"
                                 :aria-invalid="errors && errors.length > 0"
                                 :disabled="!badgeInternal.timeLimitEnabled"
                                 aria-errormessage="awardNameError"
                                 aria-describedby="awardNameError"/>
                          <small role="alert" class="form-text text-danger" v-show="errors[0]" data-cy="awardNameError" id="awardNameError">{{ errors[0] }}
                          </small>
                        </ValidationProvider>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="row" v-if="badgeInternal.timeLimitEnabled">
                <div class="col-12 col-sm">
                  <ValidationProvider rules="optionalNumeric|required|min_value:0|daysMaxTimeLimit:@timeLimitHours,@timeLimitMinutes|cantBe0IfHours0Minutes0" vid="timeLimitDays" v-slot="{errors}" name="Days">
                    <div class="input-group">
                      <input class="form-control d-inline" type="text" v-model="badgeInternal.expirationDays"
                             value="8" :disabled="!badgeInternal.timeLimitEnabled"
                             :aria-required="badgeInternal.timeLimitEnabled"
                             ref="timeLimitDays" data-cy="timeLimitDays"
                             v-on:keydown.enter="handleSubmit(updateBadge)"
                             id="timeLimitDays" :aria-label="`time window days ${maxTimeLimitMessage}`"
                             aria-describedby="badgeDaysError" :aria-invalid="errors && errors.length > 0"
                             aria-errormessage="badgeDaysError"/>
                      <div class="input-group-append">
                        <span class="input-group-text" id="days-append">Days</span>
                      </div>
                    </div>
                    <small role="alert" class="form-text text-danger" data-cy="badgeDaysError" id="badgeDaysError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="col-12 col-sm">
                  <ValidationProvider rules="optionalNumeric|required|min_value:0|max_value:23|hoursMaxTimeLimit:@timeLimitDays,@timeLimitMinutes|cantBe0IfMins0Days0" vid="timeLimitHours" v-slot="{errors}" name="Hours">
                    <div class="input-group">
                      <input class="form-control d-inline" type="text" v-model="badgeInternal.expirationHrs"
                             value="8" :disabled="!badgeInternal.timeLimitEnabled"
                             :aria-required="badgeInternal.timeLimitEnabled"
                             ref="timeLimitHours" data-cy="timeLimitHours"
                             v-on:keydown.enter="handleSubmit(updateBadge)"
                             id="timeLimitHours" :aria-label="`time window hours ${maxTimeLimitMessage}`"
                             aria-describedby="badgeHoursError" :aria-invalid="errors && errors.length > 0"
                             aria-errormessage="badgeHoursError"/>
                      <div class="input-group-append">
                        <span class="input-group-text" id="hours-append">Hours</span>
                      </div>
                    </div>
                    <small role="alert" class="form-text text-danger" data-cy="badgeHoursError" id="badgeHoursError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="col-12 col-sm">
                  <ValidationProvider rules="optionalNumeric|required|min_value:0|max_value:59|minutesMaxTimeLimit:@timeLimitDays,@timeLimitHours|cantBe0IfHours0Days0" vid="timeLimitMinutes" v-slot="{errors}" name="Minutes">
                    <div class="input-group">
                      <input class="form-control d-inline"  type="text" v-model="badgeInternal.expirationMins"
                             value="0" :disabled="!badgeInternal.timeLimitEnabled" ref="timeLimitMinutes" data-cy="timeLimitMinutes"
                             v-on:keydown.enter="handleSubmit(updateBadge)"
                             :aria-required="badgeInternal.timeLimitEnabled"
                             aria-label="time window minutes"
                             aria-describedby="badgeMinutesError"
                             aria-errormessage="badgeMinutesError"
                             :aria-invalid="errors && errors.length > 0"/>
                      <div class="input-group-append">
                        <span class="input-group-text" id="minutes-append">Minutes</span>
                      </div>
                    </div>
                    <small role="alert" class="form-text text-danger" data-cy="badgeMinutesError" id="badgeMinutesError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
              </div>
            </div>
          </b-card>

          <help-url-input class="mt-3"
                          :next-focus-el="previousFocus"
                          @shown="tooltipShowing=true"
                          @hidden="tooltipShowing=false"
                          v-model="badgeInternal.helpUrl" v-on:keydown.enter.native="handleSubmit(updateBadge)" />

          <div v-if="!global" data-cy="gemEditContainer">
            <b-form-checkbox v-model="limitTimeframe" class="mt-4"
                             @change="onEnableGemFeature" data-cy="enableGemCheckbox">
              Enable Gem Feature
              <inline-help
                target-id="gemFeatureHelp"
                :next-focus-el="previousFocus"
                @shown="tooltipShowing=true"
                @hidden="tooltipShowing=false"
                msg="The Gem feature allows for the badge to only be achievable during the specified time frame."/>
            </b-form-checkbox>

            <b-collapse id="gemCollapse" v-model="limitTimeframe">
              <b-row v-if="limitTimeframe" no-gutters class="justify-content-md-center mt-3" key="gemTimeFields">
                <b-col cols="12" md="4" style="min-width: 20rem;">
                  <label class="label mt-2">* Start Date</label>
                  <ValidationProvider rules="required|dateOrder" v-slot="{errors}" name="Start Date"
                                      ref="startDateValidationProvider">
                    <datepicker :inline="true" v-model="badgeInternal.startDate" name="startDate"
                                key="gemFrom" data-cy="startDatePicker"
                                aria-required="true"></datepicker>
                    <small role="alert" class="form-text text-danger" v-show="errors[0]" data-cy="startDateError">{{ errors[0] }}
                    </small>
                  </ValidationProvider>
                </b-col>
                <b-col cols="12" md="4" style="min-width: 20rem;">
                  <label class="label mt-2">* End Date</label>
                  <ValidationProvider rules="required|dateOrder|noHistoricalEnd" v-slot="{errors}" name="End Date"
                                      ref="endDateValidationProvider">
                    <datepicker :inline="true" v-model="badgeInternal.endDate" name="endDate"
                                key="gemTo" data-cy="endDatePicker" aria-required="true"></datepicker>
                    <small role="alert" class="form-text text-danger" v-show="errors[0]" data-cy="endDateError">{{errors[0]}}</small>
                  </ValidationProvider>
                </b-col>
              </b-row>
            </b-collapse>
          </div>
        </div>
        <div v-else>
          <icon-manager @selected-icon="onSelectedIcon"></icon-manager>
          <div class="text-right mr-2">
            <b-button variant="secondary" @click="toggleIconDisplay(false, false)" class="mt-4">Cancel Icon Selection
            </b-button>
          </div>
        </div>
      </b-container>

      <div slot="modal-footer" class="w-100">
        <div v-if="displayIconManager === false">
          <b-button variant="success" size="sm" class="float-right" @click="handleSubmit(updateBadge)"
                    :disabled="invalid"
                    data-cy="saveBadgeButton">
            Save
          </b-button>
          <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe" data-cy="closeBadgeButton">
            Cancel
          </b-button>
        </div>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import { extend } from 'vee-validate';
  import Datepicker from 'vuejs-datepicker';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import dayjs from '@/common-components/DayJsCustomizer';
  import MarkdownEditor from '@/common-components/utilities/MarkdownEditor';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import IdInput from '../utils/inputForm/IdInput';
  import InlineHelp from '../utils/InlineHelp';
  import BadgesService from './BadgesService';
  import GlobalBadgeService from './global/GlobalBadgeService';
  import InputSanitizer from '../utils/InputSanitizer';
  import HelpUrlInput from '../utils/HelpUrlInput';
  import SaveComponentStateLocallyMixin from '../utils/SaveComponentStateLocallyMixin';
  import ReloadMessage from '../utils/ReloadMessage';

  export default {
    name: 'EditBadge',
    mixins: [SaveComponentStateLocallyMixin, MsgBoxMixin],
    components: {
      HelpUrlInput,
      InlineHelp,
      IconPicker,
      MarkdownEditor,
      Datepicker,
      SkillsSpinner,
      IdInput,
      ReloadMessage,
      'icon-manager': () => import(/* webpackChunkName: 'iconManager' */'../utils/iconPicker/IconManager'),
    },
    props: {
      badge: Object,
      isEdit: {
        type: Boolean,
        default: false,
      },
      value: Boolean,
      global: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      let awardAttrs;
      if (this.badge.awardAttrs && this.badge.awardAttrs.name !== null) {
        awardAttrs = this.badge.awardAttrs;
      } else {
        awardAttrs = {
          name: 'Speedy Finish',
          iconClass: 'fas fa-car-side',
          numMinutes: 0,
        };
      }
      const expirationDays = awardAttrs.numMinutes ? Math.floor(((awardAttrs.numMinutes / 60) / 24)) : 0;
      const remainingHrs = awardAttrs.numMinutes - (expirationDays * 24);
      const expirationHrs = remainingHrs ? Math.floor((remainingHrs / 60)) : 8;
      const expirationMins = remainingHrs ? remainingHrs % 60 : 0;
      const timeLimitEnabled = awardAttrs.numMinutes > 0;
      const badgeInternal = {
        originalBadgeId: this.badge.badgeId,
        isEdit: this.isEdit,
        description: '',
        startDate: null,
        endDate: null,
        badgeId: this.badge.badgeId,
        expirationDays,
        expirationHrs,
        expirationMins,
        timeLimitEnabled,
        ...this.badge,
      };
      badgeInternal.awardAttrs = awardAttrs;
      // convert string to Date objects
      badgeInternal.startDate = this.toDate(this.badge.startDate);
      badgeInternal.endDate = this.toDate(this.badge.endDate);
      const limitedTimeframe = !!(this.badge.startDate && this.badge.endDate);
      return {
        canAutoGenerateId: true,
        canEditBadgeId: false,
        badgeInternal,
        originalBadge: {
          badgeId: this.badge.badgeId,
          name: this.badge.name,
          description: this.badge.description,
          helpUrl: this.badge.helpUrl,
          startDate: this.toDate(this.badge.startDate),
          endDate: this.toDate(this.badge.endDate),
          expirationDays,
          expirationHrs,
          expirationMins,
          timeLimitEnabled,
          awardAttrs: {
            name: 'Speedy Finish',
            iconClass: 'fas fa-car-side',
            numMinutes: 60 * 8,
          },
        },
        limitTimeframe: limitedTimeframe,
        show: this.value,
        displayIconManager: false,
        currentFocus: null,
        previousFocus: null,
        tooltipShowing: false,
        loadingComponent: true,
        keysToWatch: ['name', 'description', 'badgeId', 'helpUrl', 'startDate', 'endDate', 'expirationMins', 'expirationHrs', 'expirationDays'],
        restoredFromStorage: false,
        isAwardIcon: false,
      };
    },
    created() {
      this.assignCustomValidation();
    },
    mounted() {
      document.addEventListener('focusin', this.trackFocus);
      this.loadComponent();
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Badge' : 'New Badge';
      },
      componentName() {
        const badgeScope = this.badgeInternal.projectId ? this.badgeInternal.projectId : 'Global';
        return `${badgeScope}-${this.$options.name}${this.isEdit ? 'Edit' : ''}`;
      },
      maxTimeLimitMessage() {
        return `Time Window must be less then ${this.$store.getters.config.maxTimeWindowInMinutes / 60} hours`;
      },
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
      badgeInternal: {
        handler(newValue) {
          this.saveComponentState(this.componentName, newValue);
        },
        deep: true,
      },
    },
    methods: {
      discardChanges(reload = false) {
        this.clearComponentState(this.componentName);
        if (reload) {
          this.restoredFromStorage = false;
          this.loadComponent();
        }
      },
      loadComponent() {
        this.loadingComponent = true;
        this.loadComponentState(this.componentName).then((result) => {
          if (result) {
            if (!this.isEdit || (this.isEdit && result.originalBadgeId === this.originalBadge.badgeId)) {
              this.badgeInternal = result;
              this.limitTimeframe = !!(this.badgeInternal.startDate && this.badgeInternal.endDate);
              this.restoredFromStorage = true;
            } else {
              this.badgeInternal = Object.assign(this.badgeInternal, this.originalBadge);
            }
          } else if (!this.isEdit) {
            this.badgeInternal = Object.assign(this.badgeInternal, this.originalBadge);
          }
        }).finally(() => {
          this.loadingComponent = false;
          if (this.isEdit) {
            setTimeout(() => {
              this.$nextTick(() => {
                const { observer } = this.$refs;
                if (observer) {
                  observer.validate({ silent: false });
                }
              });
            }, 600);
          }
        });
      },
      trackFocus() {
        this.previousFocus = this.currentFocus;
        this.currentFocus = document.activeElement;
      },
      closeMe(e) {
        this.clearComponentState(this.componentName);
        this.hideModal(e);
      },
      publishHidden(e) {
        if (!e.updated && this.hasObjectChanged(this.badgeInternal, this.originalBadge) && !this.loadingComponent) {
          e.preventDefault();
          this.$nextTick(() => this.$announcer.polite('You have unsaved changes.  Discard?'));
          this.msgConfirm('You have unsaved changes.  Discard?', 'Discard Changes?', 'Discard Changes', 'Continue Editing')
            .then((res) => {
              if (res) {
                this.discardChanges(false);
                this.hideModal(e);
                this.$nextTick(() => this.$announcer.polite('Changes discarded'));
              } else {
                this.$nextTick(() => this.$announcer.polite('Continued editing'));
              }
            });
        } else if (this.tooltipShowing) {
          e.preventDefault();
        } else {
          this.clearComponentState(this.componentName);
          this.hideModal(e);
        }
      },
      hideModal(e) {
        this.show = false;
        this.$emit('hidden', e);
      },
      updateDescription(event) {
        this.badgeInternal.description = event;
      },
      updateBadge() {
        this.$refs.observer.validate()
          .then((res) => {
            if (res) {
              this.publishHidden({ updated: true });
              this.badgeInternal.badgeId = InputSanitizer.sanitize(this.badgeInternal.badgeId);
              this.badgeInternal.name = InputSanitizer.sanitize(this.badgeInternal.name);
              this.$emit('badge-updated', { isEdit: this.isEdit, ...this.badgeInternal });
            }
          });
      },
      updateBadgeId() {
        if (!this.isEdit && this.canAutoGenerateId) {
          let id = InputSanitizer.removeSpecialChars(this.badgeInternal.name);
          // Subjects, skills and badges can not have same id under a project
          // by default append Badge to avoid id collision with other entities,
          // user can always override in edit mode
          if (id) {
            id = `${id}Badge`;
          }
          this.badgeInternal.badgeId = id;

          // we're going to need to trigger validation somehow here
        }
      },
      onSelectedIcon(selectedIcon) {
        if (this.isAwardIcon) {
          this.badgeInternal.awardAttrs.iconClass = `${selectedIcon.css}`;
        } else {
          this.badgeInternal.iconClass = `${selectedIcon.css}`;
        }
        this.displayIconManager = false;
      },
      onEnableGemFeature(value) {
        if (!value) {
          this.$nextTick(() => {
            this.badgeInternal.startDate = null;
            this.badgeInternal.endDate = null;
          });
        }
      },
      toDate(value) {
        let dateVal = value;
        if (value && !(value instanceof Date)) {
          dateVal = new Date(Date.parse(value.replace(/-/g, '/')));
        }
        return dateVal;
      },
      toggleEditId() {
        this.canEditBadgeId = !this.canEditBadgeId && !this.isEdit;
        this.updateBadgeId();
      },
      toggleIconDisplay(shouldDisplay, isAwardIcon) {
        this.displayIconManager = shouldDisplay;
        this.isAwardIcon = isAwardIcon;
      },
      resetTimeLimit(checked) {
        if (!checked) {
          this.badgeInternal.expirationDays = 0;
          this.badgeInternal.expirationHrs = 8;
          this.badgeInternal.expirationMins = 0;
        }
      },
      assignCustomValidation() {
        // only want to validate for a new badge, existing subjects will override
        // name and badge id
        const self = this;
        extend('uniqueName', {
          message: (field) => `The value for ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && (value === self.badge.name || self.badge.name.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
              return true;
            }
            if (self.global) {
              return GlobalBadgeService.badgeWithNameExists(value);
            }
            return BadgesService.badgeWithNameExists(self.badgeInternal.projectId, value);
          },
        }, {
          immediate: false,
        });

        extend('uniqueId', {
          message: (field) => `The value for ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.badge.badgeId === value) {
              return true;
            }
            if (self.global) {
              return GlobalBadgeService.badgeWithIdExists(value);
            }
            return BadgesService.badgeWithIdExists(self.badgeInternal.projectId, value);
          },
        });

        if (this.global) {
          extend('help_url', {
            message: (field) => `${field} must start with "http(s)"`,
            validate(value) {
              if (!value) {
                return true;
              }
              return value.startsWith('http') || value.startsWith('https');
            },
          });
        } else {
          extend('help_url', {
            message: (field) => `${field} must start with "/" or "http(s)"`,
            validate(value) {
              if (!value) {
                return true;
              }
              return value.startsWith('http') || value.startsWith('https') || value.startsWith('/');
            },
          });
        }

        /*
        Provider's reset() method triggers an infinite loop if we use it in dateOrder
        we need to explicitly set the flags and manually clear errors
         */
        const resetProvider = (provider) => {
          if (provider) {
            provider.setErrors([]);
            provider.setFlags({
              valid: true,
              invalid: false,
              passed: true,
              failed: false,
            });
          }
        };

        extend('dateOrder', {
          message: 'Start Date must come before End Date',
          validate() {
            let valid = true;
            if (self.limitTimeframe && self.badgeInternal.startDate && self.badgeInternal.endDate) {
              valid = dayjs(self.badgeInternal.startDate).isBefore(dayjs(self.badgeInternal.endDate));
              if (valid) {
                // manually clear errors in case the orig error occurred when setting startDate,
                // but was fixed by updating endDate (or vise-versa)
                resetProvider(self.$refs.startDateValidationProvider);
                resetProvider(self.$refs.endDateValidationProvider);
              }
            }
            return valid;
          },
        });

        extend('noHistoricalEnd', {
          message: 'End Date cannot be in the past',
          validate() {
            let valid = true;
            // only trigger this validation on new badge entry, not edits
            if (self.limitTimeframe && self.badgeInternal.endDate && !self.badge.badgeId) {
              valid = dayjs(self.badgeInternal.endDate).isAfter(dayjs());
            }
            return valid;
          },
        });

        const validateLimit = (windowDays, windowHours, windowMinutes, validator) => {
          let days = 0;
          let hours = 0;
          let minutes = 0;
          if (windowHours) {
            hours = parseInt(windowHours, 10);
          }

          if (windowMinutes) {
            minutes = parseInt(windowMinutes, 10);
          }

          if (windowDays) {
            days = parseInt(windowDays, 10);
          }

          if (validator === 'daysMaxTimeLimit' && days === 0) {
            return true;
          }
          if (validator === 'hoursMaxTimeLimit' && hours === 0) {
            return true;
          }
          if (validator === 'minutesMaxTimeLimit' && minutes === 0) {
            return true;
          }

          return ((days * 24 * 60) + (hours * 60) + minutes) <= this.$store.getters.config.maxTimeWindowInMinutes;
        };

        extend('daysMaxTimeLimit', {
          message: () => this.maxTimeLimitMessage,
          params: ['hours', 'minutes'],
          validate(value, { hours, minutes }) {
            return validateLimit(value, hours, minutes, 'daysMaxTimeLimit');
          },
        });
        extend('hoursMaxTimeLimit', {
          message: () => this.maxTimeLimitMessage,
          params: ['days', 'minutes'],
          validate(value, { days, minutes }) {
            return validateLimit(days, value, minutes, 'hoursMaxTimeLimit');
          },
        });
        extend('minutesMaxTimeLimit', {
          message: () => this.maxTimeLimitMessage,
          params: ['days', 'hours'],
          validate(value, { days, hours }) {
            return validateLimit(days, hours, value, 'minutesMaxTimeLimit');
          },
        });
        extend('cantBe0IfHours0Minutes0', {
          message: (field) => `${field} must be > 0 if Hours = 0 and Minutes = 0`,
          validate(value) {
            if (parseInt(value, 10) > 0 || parseInt(self.badgeInternal.expirationHrs, 10) > 0 || parseInt(self.badgeInternal.expirationMins, 10) > 0) {
              return true;
            }
            return false;
          },
        });
        extend('cantBe0IfHours0Days0', {
          message: (field) => `${field} must be > 0 if Hours = 0 and Days = 0`,
          validate(value) {
            if (parseInt(value, 10) > 0 || parseInt(self.badgeInternal.expirationHrs, 10) > 0 || parseInt(self.badgeInternal.expirationDays, 10) > 0) {
              return true;
            }
            return false;
          },
        });
        extend('cantBe0IfMins0Days0', {
          message: (field) => `${field} must be > 0 if Minutes = 0 and Days = 0`,
          validate(value) {
            if (parseInt(value, 10) > 0 || parseInt(self.badgeInternal.expirationMins, 10) > 0 || parseInt(self.badgeInternal.expirationDays, 10) > 0) {
              return true;
            }
            return false;
          },
        });
      },
    },
  };

</script>

<style scoped>
</style>
