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
  <sub-page-header title="Settings"/>

  <skills-spinner :is-loading="isLoadingData" />
  <b-card v-if="!isLoadingData">
    <no-content2 v-if="isSurveyType" title="No Settings" class="my-5" data-cy="noSettingsAvailable"
                    message="Surveys do not have any available settings."/>
    <div v-if="!isSurveyType">
      <ValidationObserver ref="observer" v-slot="{ invalid, handleSubmit }" slim>
        <div class="row">
          <div id="quizNumQuestions" class="col col-md-3 text-secondary" >
            # of Questions per Quiz Attempt:
          </div>
          <div class="col">
            <ValidationProvider name="Quiz Length" vid="quizNumQuestions" rules="required|greaterThanOrEqualToPassing:@quizPassingQuestions" v-slot="{errors}">
              <b-form-select v-model="settings.quizLength.value"
                             :options="quizLengthOptions"
                             aria-labelledby="quizNumQuestions"
                             ref="quizNumQuestions"
                             data-cy="quizNumQuestions" />
              <small role="alert" class="form-text text-danger" v-show="errors[0]">{{
                  errors[0]}}
              </small>
            </ValidationProvider>
          </div>
        </div>
        <div class="row mt-3">
          <div id="quizPassingReq" class="col col-md-3 text-secondary" >
            Passing Requirement:
          </div>
          <div class="col">
            <ValidationProvider name="Number of Required Questions" vid="quizPassingQuestions" rules="required|lessThanOrEqualToLength:@quizNumQuestions" v-slot="{errors}">
              <b-form-select v-model="settings.passingReq.value"
                             :options="numRequiredQuestionsOptions"
                             aria-labelledby="quizPassingReq"
                             data-cy="quizPassingSelector" />
              <small role="alert" class="form-text text-danger" v-show="errors[0]">{{
                  errors[0]}}
              </small>
            </ValidationProvider>
        </div>
      </div>
      <div class="row mt-3">
        <div id="quizMaxNumOfAttempts" class="col col-md-3 text-secondary" >
          Maximum Number of Attempts:
        </div>
        <div class="col">
          <div class="row">
            <div class="col-auto">
              <b-form-checkbox v-model="settings.numAttempts.unlimited"
                               aria-label="Maximum Number of Attempts setting, unlimited number of attempts checkbox"
                               name="Unlimited Attempts"
                               data-cy="unlimitedAttemptsSwitch" switch>
                Unlimited
              </b-form-checkbox>
            </div>
            <div class="col" v-if="!settings.numAttempts.unlimited">
              <ValidationProvider name="Number of Attempts" rules="optionalNumeric|required|min_value:1|max_value:1000" v-slot="{errors}">
              <b-form-input
                aria-labelledby="quizMaxNumOfAttempts"
                data-cy="numAttemptsInput"
                v-model="settings.numAttempts.value" />
                <small role="alert" class="form-text text-danger" v-show="errors[0]">{{
                    errors[0]}}
                </small>
              </ValidationProvider>
            </div>
          </div>
        </div>
      </div>

      <div class="row mt-3">
        <div id="randomizeQuestions" class="col col-md-3 text-secondary" >
          Randomize Question Order:
        </div>
        <div class="col">
          <div class="row">
            <div class="col-auto">
              <b-form-checkbox v-model="settings.randomizeQuestions.value"
                               aria-label="Randomize order of the questions"
                               name="Randomize Question Order"
                               data-cy="randomizeQuestionSwitch" switch>
                Randomize
              </b-form-checkbox>
            </div>
          </div>
        </div>
      </div>

      <div class="row mt-3">
        <div id="randomizeAnswers" class="col col-md-3 text-secondary" >
          Randomize Answer Order:
        </div>
        <div class="col">
          <div class="row">
            <div class="col-auto">
              <b-form-checkbox v-model="settings.randomizeAnswers.value"
                               aria-label="Randomize order of the answers"
                               name="Randomize Answer Order"
                               data-cy="randomizeAnswerSwitch" switch>
                Randomize
              </b-form-checkbox>
            </div>
          </div>
        </div>
      </div>

      <div v-if="errMsg" class="alert alert-danger">
        {{ errMsg }}
      </div>

      <hr/>

      <div class="row">
        <div class="col">
          <b-overlay
            :show="isSaving"
            rounded
            opacity="0.6"
            spinner-small
            spinner-variant="primary"
            class="d-inline-block"
          >
            <b-button variant="outline-success" @click="handleSubmit(saveSettings)" :disabled="invalid || !hasChanged"
                      aria-label="Save Settings"
                      data-cy="saveSettingsBtn">
              Save <i class="fas fa-arrow-circle-right"/>
            </b-button>
          </b-overlay>

          <span v-if="hasChanged" class="text-warning ml-2" data-cy="unsavedChangesAlert">
                  <i class="fa fa-exclamation-circle"
                     aria-label="Settings have been changed, do not forget to save"
                     v-b-tooltip.hover="'Settings have been changed, do not forget to save'"/> Unsaved Changes
                </span>
          <span v-if="!hasChanged && showSavedMsg" class="text-success ml-2" data-cy="settingsSavedAlert">
                  <i class="fa fa-check" />
                  Settings Updated!
                </span>
        </div>
      </div>
    </ValidationObserver>
    </div>
  </b-card>
</div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import { extend } from 'vee-validate';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import QuizService from '@/components/quiz/QuizService';
  import NoContent2 from '@/components/utils/NoContent2';

  const { mapGetters } = createNamespacedHelpers('quiz');

  export default {
    name: 'QuizSettings',
    components: { NoContent2, SkillsSpinner, SubPageHeader },
    data() {
      return {
        isLoadingSettings: true,
        isSaving: false,
        quizId: this.$route.params.quizId,
        showSavedMsg: false,
        settings: {
          passingReq: {
            value: '-1',
            setting: 'quizPassingReq',
            lastLoadedValue: '-1',
          },
          numAttempts: {
            value: 3,
            unlimited: true,
            setting: 'quizNumberOfAttempts',
            lastLoadedValue: 3,
            lastLoadedUnlimited: true,
          },
          randomizeQuestions: {
            value: false,
            setting: 'quizRandomizeQuestions',
            lastLoadedValue: false,
          },
          randomizeAnswers: {
            value: false,
            setting: 'quizRandomizeAnswers',
            lastLoadedValue: false,
          },
          quizLength: {
            value: '-1',
            setting: 'quizLength',
            lastLoadedValue: '-1',
          },
        },
        errMsg: null,
      };
    },
    computed: {
      ...mapGetters([
        'quizSummary',
        'loadingQuizSummary',
      ]),
      isLoadingData() {
        return this.isLoadingSettings || this.loadingQuizSummary;
      },
      numRequiredQuestionsOptions() {
        const num = this.quizSummary.numQuestions;
        const questionBasedOptions = Array.from({ length: num }, (_, index) => ({ value: `${index + 1}`, text: `${index + 1} Correct Question${index > 0 ? 's' : ''}` }));
        return [{ value: '-1', text: 'ALL Questions - 100%' }].concat(questionBasedOptions);
      },
      quizLengthOptions() {
        const num = this.quizSummary.numQuestions;
        const questionBasedOptions = Array.from({ length: num }, (_, index) => ({ value: `${index + 1}`, text: `${index + 1} Question${index > 0 ? 's' : ''}` }));
        return [{ value: '-1', text: 'ALL Questions - 100%' }].concat(questionBasedOptions);
      },
      hasChanged() {
        return this.settings.passingReq.value !== this.settings.passingReq.lastLoadedValue
          || this.settings.numAttempts.unlimited !== this.settings.numAttempts.lastLoadedUnlimited
          || (!this.settings.numAttempts.unlimited && this.settings.numAttempts.value !== this.settings.numAttempts.lastLoadedValue)
          || (this.settings.randomizeAnswers.value !== this.settings.randomizeAnswers.lastLoadedValue)
          || (this.settings.randomizeQuestions.value !== this.settings.randomizeQuestions.lastLoadedValue)
          || this.settings.quizLength.value !== this.settings.quizLength.lastLoadedValue;
      },
      isSurveyType() {
        return this.quizSummary.type === 'Survey';
      },
    },
    mounted() {
      this.isLoadingSettings = true;
      this.loadAndUpdateQuizSettings()
        .then(() => {
          this.isLoadingSettings = false;
          this.setupValidation();
        });
    },
    methods: {
      setupValidation() {
        extend('lessThanOrEqualToLength', {
          message: () => 'Passing requirement must be less than or equal to the quiz length',
          params: ['quizNumQuestions'],
          validate(value, { quizNumQuestions }) {
            const foundSelected = (quizNumQuestions !== '-1' && value <= quizNumQuestions) || quizNumQuestions === '-1';
            return foundSelected;
          },
        });

        extend('greaterThanOrEqualToPassing', {
          message: () => 'Quiz length must be greater than or equal to the passing requirement',
          params: ['quizPassingQuestions'],
          validate(value, { quizPassingQuestions }) {
            const foundSelected = (value !== '-1' && value >= quizPassingQuestions) || value === '-1';
            return foundSelected;
          },
        });
      },
      saveSettings() {
        this.$refs.observer.validate()
          .then((res1) => {
            if (!res1) {
              this.errMsg = 'Form did NOT pass validation, please fix and try to Save again';
            } else {
              this.collectAndSave();
            }
          });
      },
      collectAndSave() {
        let dirtySettings = Object.values(this.settings).filter((s) => {
          if (s.setting === this.settings.numAttempts.setting) {
            return s.unlimited !== s.lastLoadedUnlimited || (!s.unlimited && s.value !== s.lastLoadedValue);
          }
          return s.value !== s.lastLoadedValue;
        });
        if (dirtySettings) {
          this.isSaving = true;
          dirtySettings = dirtySettings.map((s) => {
            if (s.setting === this.settings.numAttempts.setting && s.unlimited) {
              return ({ ...s, value: '-1' });
            }
            return s;
          });
          QuizService.saveQuizSettings(this.quizId, dirtySettings)
            .then(() => {
              this.loadAndUpdateQuizSettings()
                .then(() => {
                  this.isSaving = false;
                  this.showSavedMsg = true;
                  this.$nextTick(() => {
                    this.$announcer.polite('Quiz Settings have been successfully saved');
                  });
                  setTimeout(() => {
                    this.showSavedMsg = false;
                  }, 4000);
                });
            });
        }
      },
      loadAndUpdateQuizSettings() {
        return QuizService.getQuizSettings(this.quizId)
          .then((settings) => {
            if (settings) {
              const settingsKeys = Object.keys(this.settings);
              settingsKeys.forEach((key) => {
                const settingValue = this.settings[key];
                const foundFromServer = settings.find((confS) => settingValue.setting === confS.setting);
                if (foundFromServer) {
                  if (foundFromServer.setting === this.settings.numAttempts.setting) {
                    if (foundFromServer.value === '-1') {
                      this.settings.numAttempts.value = '3';
                      this.settings.numAttempts.lastLoadedValue = '3';
                      this.settings.numAttempts.unlimited = true;
                      this.settings.numAttempts.lastLoadedUnlimited = true;
                    } else {
                      this.settings.numAttempts.value = foundFromServer.value;
                      this.settings.numAttempts.lastLoadedValue = foundFromServer.value;
                      this.settings.numAttempts.unlimited = false;
                      this.settings.numAttempts.lastLoadedUnlimited = false;
                    }
                  } else {
                    this.settings[key].value = foundFromServer.value;
                    this.settings[key].lastLoadedValue = foundFromServer.value;
                  }
                }
              });
            }
          });
      },
    },
  };
</script>

<style scoped>

</style>
