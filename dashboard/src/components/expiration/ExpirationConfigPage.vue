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
  <div>
    <sub-page-header title="Configure Expiration"/>
    <b-overlay :show="loading || loadingSkill">
      <b-card>
        <div v-if="isReadOnly" class="alert alert-info" data-cy="readOnlyAlert">
          <i class="fas fa-exclamation-triangle" aria-hidden="true"/> Expiration attributes of <span
          v-if="isImported"><b-badge variant="success"><i class="fas fa-book" aria-hidden="true"/> Imported</b-badge></span><span v-if="isReused"><b-badge variant="success"><i class="fas fa-recycle" aria-hidden="true"/> Reused</b-badge></span>
          skills are read-only.
        </div>
        <b-form-group v-slot="{ ariaDescribedby }" class="m-0 p-0" :disabled="isReadOnly">
          <b-form-radio-group
            id="expiration-type"
            v-model="expirationType"
            :aria-describedby="ariaDescribedby"
            name="Achievement Expiration Options"
            aria-label="Achievement Expiration Options"
            data-cy="expirationTypeSelector"
            stacked
          >
            <template #first>
              <div class="row m-0">
                <div class="col-12 col-lg-auto mb-1">
                  <b-form-radio class="" value="NEVER" data-cy="expirationNeverRadio">Never</b-form-radio>
                </div>
              </div>
            </template>

            <hr class="my-3"/>

            <template>
              <div class="row m-0">
                <div class="col-12 col-lg-auto">
                  <b-form-radio class="" value="YEARLY" data-cy="yearlyRadio">Yearly</b-form-radio>
                </div>
              </div>
              <div class="row ml-5">
                <b-form-group :disabled="expirationType !== 'YEARLY'" data-cy="yearlyFormGroup">
                  <div class="input-group">
                      <div class="col-auto mr-0 pr-0" :class="{'text-muted': expirationType !== 'YEARLY'}">
                        <label for="yearlyYears-sb">Skills will expire every</label>
                        <b-form-spinbutton :disabled="expirationType !== 'YEARLY'"
                                           class="m-1"
                                           id="yearlyYears-sb"
                                           data-cy="yearlyYears-sb"
                                           :aria-label="`Skills will expire every ${yearlyYears} years`"
                                           v-model="yearlyYears"
                                           min="1"
                                           max="99"
                                           inline>
                        </b-form-spinbutton>
                        <span>year{{yearlyYears > 1 ? 's' : ''}} on: </span>
                      </div>
                      <div class="col-auto m-1 px-0">
                        <b-form-select v-model="yearlyMonth"
                                       :options="monthsOptions"
                                       @change="yearlyDayOfMonth=1"
                                       aria-label="Month of year"
                                       data-cy="yearlyMonth"/>
                      </div>

                      <div class="col-auto m-1 pl-0">
                        <b-form-select v-model="yearlyDayOfMonth"
                                       :options="dayOptions"
                                       aria-label="Day of month"
                                       data-cy="yearlyDayOfMonth"/>
                      </div>
                  </div>
                </b-form-group>
              </div>
            </template>
            <template>
              <div class="row m-0">
                <div class="col-12 col-lg-auto">
                  <b-form-radio value="MONTHLY" data-cy="monthlyRadio">Monthly</b-form-radio>
                </div>
              </div>
              <div class="row ml-5">
                <b-form-group :disabled="expirationType !== 'MONTHLY'" data-cy="monthlyFormGroup" >
                  <div class="input-group">
                    <div class="col-auto mr-0 pr-0" :class="{'text-muted': expirationType !== 'MONTHLY'}">
                      <label for="monthlyMonths-sb">Skills will expire every</label>
                      <b-form-spinbutton :disabled="expirationType !== 'MONTHLY'"
                                         class="m-1"
                                         id="monthlyMonths-sb"
                                         data-cy="monthlyMonths-sb"
                                         :aria-label="`Skills will expire every ${monthlyMonths} months`"
                                         v-model="monthlyMonths"
                                         min="1"
                                         max="99"
                                         inline>
                      </b-form-spinbutton>
                      <span>month{{monthlyMonths > 1 ? 's' : ''}} on day: </span>
                    </div>
                    <div class="col-auto m-1 px-0">
                      <b-form-group
                        :disabled="expirationType !== 'MONTHLY'"
                        v-model="monthlyDay"
                        class="mb-0"
                        v-slot="{ ariaDescribedby }"
                      >
                        <b-form-radio-group
                          class="pt-2"
                          v-model="monthlyDayOption"
                          data-cy="monthlyDayOption"
                          aria-label="Day of month"
                          :options="[
                            {value: 'FIRST_DAY_OF_MONTH', text: 'First'},
                            {value: 'LAST_DAY_OF_MONTH', text: 'Last'},
                            {value: 'SET_DAY_OF_MONTH', text: 'Other'}
                          ]"
                        :aria-describedby="ariaDescribedby"
                        ></b-form-radio-group>
                      </b-form-group>
                    </div>

                    <div class="col-auto m-1 pl-0">
                      <b-form-select v-model="monthlyDay"
                                     :disabled="monthlyDayOption !== 'SET_DAY_OF_MONTH'"
                                     :options="dayOptions"
                                     aria-label="Set day of month"
                                     data-cy="monthlyDay"/>
                    </div>
                  </div>
                </b-form-group>
              </div>
            </template>

            <hr class="my-3"/>

            <template>
              <div class="row m-0">
                <div class="col-12 col-lg-auto">
                  <b-form-radio class="" value="DAILY" data-cy="dailyRadio">Daily with ability to retain</b-form-radio>
                </div>
              </div>
              <div class="row ml-5">
                <b-form-group :disabled="expirationType !== 'DAILY'"  data-cy="dailyFormGroup">
                  <div class="input-group">
                    <div class="col-auto mr-0 pr-0" :class="{'text-muted': expirationType !== 'DAILY'}">
                      <span for="dailyDays-sb">Achievement will expire after</span>
                      <b-form-spinbutton :disabled="expirationType !== 'DAILY'"
                                         class="m-1"
                                         id="dailyDays-sb"
                                         data-cy="dailyDays-sb"
                                         :aria-label="`Skills will expire every ${dailyDays} days after user earns achievement`"
                                         v-model="dailyDays"
                                         min="1"
                                         max="999"
                                         inline>
                      </b-form-spinbutton>
                      <span>day{{dailyDays > 1 ? 's' : ''}} of inactivity</span>
                    </div>
                  </div>
                </b-form-group>
              </div>
            </template>
          </b-form-radio-group>
        </b-form-group>
        <hr/>
        <div class="row">
          <div class="col">
            <b-button variant="outline-success" @click="saveSettings()" :disabled="!isDirty" data-cy="saveSettingsBtn">
              Save <i class="fas fa-arrow-circle-right"/>
            </b-button>

            <span v-if="isDirty" class="text-warning ml-2" data-cy="unsavedChangesAlert">
                <i class="fa fa-exclamation-circle"
                   aria-label="Settings have been changed, do not forget to save"
                   v-b-tooltip.hover="'Settings have been changed, do not forget to save'"/> Unsaved Changes
              </span>
            <span v-if="!isDirty && showSavedMsg" class="text-success ml-2" data-cy="settingsSavedAlert">
                <i class="fa fa-check" />
                Settings Updated!
              </span>
          </div>
        </div>
      </b-card>

    </b-overlay>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import dayjs from '@/common-components/DayJsCustomizer';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import ExpirationService from './ExpirationService';

  const skills = createNamespacedHelpers('skills');

  const NEVER = 'NEVER';
  const YEARLY = 'YEARLY';
  const MONTHLY = 'MONTHLY';
  const DAILY = 'DAILY';
  const FIRST_DAY_OF_MONTH = 'FIRST_DAY_OF_MONTH';
  const LAST_DAY_OF_MONTH = 'LAST_DAY_OF_MONTH';
  const SET_DAY_OF_MONTH = 'SET_DAY_OF_MONTH';

  export default {
    name: 'ExpirationConfigPage',
    components: { SubPageHeader },
    data() {
      return {
        yearlyYears: 1,
        yearlyMonth: 1,
        yearlyDayOfMonth: 1,
        monthlyMonths: 1,
        monthlyDay: '1',
        monthlyDayOption: FIRST_DAY_OF_MONTH,
        dailyDays: 90,
        expirationType: NEVER,
        loading: true,
        showSavedMsg: false,
        loadedSettings: {},
      };
    },
    mounted() {
      const now = dayjs();
      this.yearlyMonth = now.month();
      this.yearlyDayOfMonth = now.date();
      this.loadSettings();
      this.loadSkillInfo();
    },
    computed: {
      ...skills.mapGetters([
        'skill',
      ]),
      ...skills.mapGetters([
        'loadingSkill',
      ]),
      isDirty() {
        if (this.loading) {
          return false;
        }
        if (this.loadedSettings.expirationType !== this.expirationType) {
          return true;
        }
        const nextExpirationDate = dayjs(this.loadedSettings.nextExpirationDate);
        if (this.expirationType === YEARLY) {
          return this.loadedSettings.every !== this.yearlyYears
            || nextExpirationDate.month() !== this.yearlyMonth
            || nextExpirationDate.date() !== this.yearlyDayOfMonth;
        }
        if (this.expirationType === MONTHLY) {
          const loadedMonthlyDay = this.loadedSettings.monthlyDay;
          return this.loadedSettings.every !== this.monthlyMonths
            || (this.monthlyDayOption === SET_DAY_OF_MONTH && loadedMonthlyDay !== this.monthlyDay)
            || ((this.monthlyDayOption === FIRST_DAY_OF_MONTH || this.monthlyDayOption === LAST_DAY_OF_MONTH) && loadedMonthlyDay !== this.monthlyDayOption);
        }
        if (this.expirationType === DAILY) {
          return this.loadedSettings.every !== this.dailyDays;
        }
        return false;
      },
      monthsOptions() {
        return dayjs.months().map((m, index) => ({ value: index, text: m }));
      },
      dayOptions() {
        const daysInSelectedMonth = dayjs().month(this.yearlyMonth).daysInMonth();
        return Array.from({ length: daysInSelectedMonth }, (_, index) => ({ value: index + 1, text: `${index + 1}` }));
      },
      isImported() {
        return this.skill && this.skill.copiedFromProjectId && this.skill.copiedFromProjectId.length > 0 && !this.skill.reusedSkill;
      },
      isReused() {
        return this.skill && this.skill.reusedSkill;
      },
      isReadOnly() {
        return this.isReused || this.isImported;
      },
    },
    methods: {
      ...skills.mapActions([
        'loadSkill',
      ]),
      loadSettings() {
        this.loading = true;
        ExpirationService.getExpirationSettings(this.$route.params.projectId, this.$route.params.skillId)
          .then((expirationSettings) => {
            this.expirationType = expirationSettings.expirationType;
            this.updateLoadedSettings(expirationSettings);
            if (expirationSettings.expirationType === YEARLY) {
              this.yearlyYears = expirationSettings.every;
              const nextExpirationDate = dayjs(expirationSettings.nextExpirationDate);
              this.yearlyMonth = nextExpirationDate.month();
              this.yearlyDayOfMonth = nextExpirationDate.date();
            } else if (expirationSettings.expirationType === MONTHLY) {
              this.monthlyMonths = expirationSettings.every;
              if (expirationSettings.monthlyDay !== FIRST_DAY_OF_MONTH && expirationSettings.monthlyDay !== LAST_DAY_OF_MONTH) {
                this.monthlyDayOption = SET_DAY_OF_MONTH;
                this.monthlyDay = expirationSettings.monthlyDay; // in this case, monthly day is the actual day of the month
              } else {
                this.monthlyDayOption = expirationSettings.monthlyDay;
              }
            } else if (expirationSettings.expirationType === DAILY) {
              this.dailyDays = expirationSettings.every;
            }
          }).finally(() => {
            this.loading = false;
          });
      },
      updateLoadedSettings(expirationSettings) {
        this.loadedSettings.expirationType = expirationSettings.expirationType;
        this.loadedSettings.every = expirationSettings.every;
        this.loadedSettings.monthlyDay = expirationSettings.monthlyDay;
        this.loadedSettings.nextExpirationDate = expirationSettings.nextExpirationDate;
      },
      saveSettings() {
        this.loading = true;
        const expirationSettings = {
          expirationType: this.expirationType,
          every: null,
          monthlyDay: null,
          nextExpirationDate: null,
        };
        if (this.expirationType !== NEVER) {
          const now = dayjs();
          const currentMonth = now.month();
          const currentDayOfMonth = now.date();
          if (this.expirationType === YEARLY) {
            expirationSettings.every = this.yearlyYears;

            // calculate next expiration date
            let incrementYearBy = this.yearlyYears;
            if (currentMonth < this.yearlyMonth || (currentMonth === this.yearlyMonth && currentDayOfMonth <= this.yearlyDayOfMonth)) {
              incrementYearBy -= 1;
            }
            expirationSettings.nextExpirationDate = dayjs(new Date(now.year() + incrementYearBy, this.yearlyMonth, this.yearlyDayOfMonth));
          } else if (this.expirationType === MONTHLY) {
            expirationSettings.every = this.monthlyMonths;
            expirationSettings.monthlyDay = this.monthlyDayOption;

            // calculate next expiration date
            let incrementMonthBy = this.monthlyMonths;
            if (this.monthlyDayOption === LAST_DAY_OF_MONTH || (this.monthlyDayOption === SET_DAY_OF_MONTH && currentDayOfMonth <= this.monthlyDay)) {
              incrementMonthBy -= 1;
            }
            const nextExpirationDate = dayjs(new Date(now.year(), now.month(), now.day())).date(1).add(incrementMonthBy, 'month');
            if (this.monthlyDayOption === FIRST_DAY_OF_MONTH) {
              expirationSettings.nextExpirationDate = nextExpirationDate.date(1);
            } else if (this.monthlyDayOption === LAST_DAY_OF_MONTH) {
              expirationSettings.nextExpirationDate = nextExpirationDate.endOf('month');
            } else if (this.monthlyDayOption === SET_DAY_OF_MONTH) {
              const nextExpirationDateDay = this.monthlyDay <= nextExpirationDate.daysInMonth() ? this.monthlyDay : nextExpirationDate.endOf('month').date();
              expirationSettings.nextExpirationDate = nextExpirationDate.date(nextExpirationDateDay);
              expirationSettings.monthlyDay = this.monthlyDay;
            }
          } else if (this.expirationType === DAILY) {
            expirationSettings.every = this.dailyDays;
            // any user achievement achievedOn before this date
            // expirationSettings.nextExpirationDate = dayjs(now).subtract(this.dailyDays, 'day');
          }
        }
        ExpirationService.saveExpirationSettings(this.$route.params.projectId, this.$route.params.skillId, expirationSettings)
          .then(() => {
            this.updateLoadedSettings(expirationSettings);
            this.showSavedMsg = true;
            setTimeout(() => {
              this.showSavedMsg = false;
            }, 3500);
            this.$nextTick(() => this.$announcer.polite('Expiration settings were saved'));
          })
          .finally(() => {
            this.loading = false;
          });
      },
      loadSkillInfo() {
        this.loadSkill({
          projectId: this.$route.params.projectId,
          subjectId: this.$route.params.subjectId,
          skillId: this.$route.params.skillId,
        });
      },
    },
  };
</script>

<style scoped>
</style>
