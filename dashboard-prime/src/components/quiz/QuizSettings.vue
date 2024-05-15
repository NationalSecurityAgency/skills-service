<script setup>

import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router'
import { useQuizSummaryState } from '@/stores/UseQuizSummaryState.js';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useForm } from "vee-validate";
import * as yup from 'yup'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import QuizService from '@/components/quiz/QuizService.js';
import NoContent2 from '@/components/utils/NoContent2.vue';
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import SkillsNumberInput from '@/components/utils/inputForm/SkillsNumberInput.vue'
import SkillsInputSwitch from '@/components/utils/inputForm/SkillsInputSwitch.vue';

const announcer = useSkillsAnnouncer();
const route = useRoute();
const quizSummaryState = useQuizSummaryState()
const isLoadingSettings = ref(true);
const isSaving = ref(false);
const quizId = ref(route.params.quizId);
const showSavedMsg = ref(false);
const settings = ref({
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
  quizTimeLimit: {
    value: 3600,
    unlimited: true,
    setting: 'quizTimeLimit',
    lastLoadedValue: 3600,
    lastLoadedUnlimited: true,
  },
});
const hoursForQuiz = ref(1)
const minutesForQuiz = ref( 0)
const errMsg = ref('')

const isLoadingData = computed(() => {
  return isLoadingSettings.value || quizSummaryState.loadingQuizSummary
})
const numRequiredQuestionsOptions = computed(() => {
  const num = quizSummaryState.quizSummary ? quizSummaryState.quizSummary.numQuestions : 0;
  const questionBasedOptions = Array.from({ length: num }, (_, index) => ({ value: `${index + 1}`, text: `${index + 1} Correct Question${index > 0 ? 's' : ''}` }));
  return [{ value: '-1', text: 'ALL Questions - 100%' }].concat(questionBasedOptions);
})
const quizLengthOptions = computed(() => {
  const num = quizSummaryState.quizSummary ? quizSummaryState.quizSummary.numQuestions : 0;
  const questionBasedOptions = Array.from({ length: num }, (_, index) => ({ value: `${index + 1}`, text: `${index + 1} Question${index > 0 ? 's' : ''}` }));
  return [{ value: '-1', text: 'ALL Questions - 100%' }].concat(questionBasedOptions);
})
const hasChanged = computed(() => {
  return settings.value.passingReq.value !== settings.value.passingReq.lastLoadedValue
      || settings.value.numAttempts.unlimited !== settings.value.numAttempts.lastLoadedUnlimited
      || (!settings.value.numAttempts.unlimited && settings.value.numAttempts.value !== settings.value.numAttempts.lastLoadedValue)
      || (settings.value.randomizeAnswers.value !== settings.value.randomizeAnswers.lastLoadedValue)
      || (settings.value.randomizeQuestions.value !== settings.value.randomizeQuestions.lastLoadedValue)
      || settings.value.quizLength.value !== settings.value.quizLength.lastLoadedValue
      || (!settings.value.quizTimeLimit.unlimited && settings.value.quizTimeLimit.value !== settings.value.quizTimeLimit.lastLoadedValue)
      || settings.value.quizTimeLimit.unlimited !== settings.value.quizTimeLimit.lastLoadedUnlimited;
})
const isSurveyType = computed(() => {
  return quizSummaryState.quizSummary && quizSummaryState.quizSummary.type === 'Survey';
})

onMounted(() => {
  isLoadingSettings.value =  true;
  loadAndUpdateQuizSettings()
      .then(() => {
        isLoadingSettings.value =  false;
      });
})

const loadAndUpdateQuizSettings = () => {
    return QuizService.getQuizSettings(quizId.value)
      .then((resSettings) => {
        if (resSettings) {
          const settingsKeys = Object.keys(settings.value);
          settingsKeys.forEach((key) => {
            const settingValue = settings.value[key];
            const foundFromServer = resSettings.find((confS) => settingValue.setting === confS.setting);
            if (foundFromServer) {
              if (foundFromServer.setting === settings.value.numAttempts.setting) {
                if (Number(foundFromServer.value) === -1) {
                  settings.value.numAttempts.value = 3;
                  settings.value.numAttempts.lastLoadedValue = 3;
                  settings.value.numAttempts.unlimited = true;
                  settings.value.numAttempts.lastLoadedUnlimited = true;
                } else {
                  settings.value.numAttempts.value = Number(foundFromServer.value);
                  settings.value.numAttempts.lastLoadedValue = Number(foundFromServer.value);
                  settings.value.numAttempts.unlimited = false;
                  settings.value.numAttempts.lastLoadedUnlimited = false;
                }
              } else if (foundFromServer.setting === settings.value.quizTimeLimit.setting) {
                if (Number(foundFromServer.value) === -1) {
                  settings.value.quizTimeLimit.value = 3600;
                  settings.value.quizTimeLimit.lastLoadedValue = 3600;
                  hoursForQuiz.value =  1;
                  minutesForQuiz.value =  0;
                  settings.value.quizTimeLimit.unlimited = true;
                  settings.value.quizTimeLimit.lastLoadedUnlimited = true;
                } else {
                  settings.value.quizTimeLimit.value = Number(foundFromServer.value);
                  hoursForQuiz.value = Math.floor(foundFromServer.value / 3600);
                  const remainingTime = foundFromServer.value - (hoursForQuiz.value * 3600);
                  minutesForQuiz.value =  remainingTime / 60;
                  settings.value.quizTimeLimit.lastLoadedValue = Number(foundFromServer.value);
                  settings.value.quizTimeLimit.unlimited = false;
                  settings.value.quizTimeLimit.lastLoadedUnlimited = false;
                }
              } else if ([settings.value.randomizeQuestions.setting, settings.value.randomizeAnswers.setting].includes(foundFromServer.setting)) {
                settings.value[key].value = (/true/i).test(foundFromServer.value);
                settings.value[key].lastLoadedValue = foundFromServer.value;
              } else {
                settings.value[key].value = foundFromServer.value;
                settings.value[key].lastLoadedValue = foundFromServer.value;
              }
            }
          });
        }
        setFieldValue('quizLength', settings.value.quizLength.value)
        setFieldValue('quizPassingReq', settings.value.passingReq.value);
        setFieldValue('quizNumberOfAttemptsUnlimited', settings.value.numAttempts.unlimited);
        setFieldValue('quizNumberOfAttempts', settings.value.numAttempts.value);
        setFieldValue('quizRandomizeQuestions', settings.value.randomizeQuestions.value)
        setFieldValue('quizRandomizeAnswers', settings.value.randomizeAnswers.value)
        setFieldValue('quizTimeLimitUnlimited', settings.value.quizTimeLimit.unlimited);
        setFieldValue('quizTimeLimitHours', hoursForQuiz.value);
        setFieldValue('quizTimeLimitMinutes', minutesForQuiz.value);
      });
}

const updateTimeLimit = () => {
  settings.value.quizTimeLimit.value = ((parseInt(hoursForQuiz.value, 10) * 60) + parseInt(minutesForQuiz.value, 10)) * 60;
};

const lessThanOrEqualToLength = (value) => {
  const quizNumQuestions = settings.value.quizLength.value;
  return (quizNumQuestions !== '-1' && parseInt(value, 10) <= parseInt(quizNumQuestions, 10)) || quizNumQuestions === '-1';
}
const greaterThanOrEqualToPassing = (value) => {
  const quizPassingQuestions = settings.value.passingReq.value;
  return (value !== '-1' && parseInt(value, 10) >= parseInt(quizPassingQuestions, 10)) || value === '-1';
}

const schema = yup.object().shape({
  'quizLength': yup.string()
      .required()
      .test('greaterThanOrEqualToPassing', 'Quiz length must be greater than or equal to the passing requirement', (value) => greaterThanOrEqualToPassing(value))
      .label('Quiz Length'),
  'quizPassingReq': yup.string()
      .required()
      .test('lessThanOrEqualToLength', 'Passing requirement must be less than or equal to the quiz length', (value) => lessThanOrEqualToLength(value))
      .label('Number of Required Questions'),
  'quizNumberOfAttemptsUnlimited': yup.boolean(),
  'quizNumberOfAttempts': yup.number()
    .when('quizNumberOfAttemptsUnlimited', {
      is: false,
      then: (sch)  => sch
          .required()
          .min(1)
          .max(1000)
          .label('Number of Attempts')
          .typeError('Number of Attempts must be a number between 1 and 1000'),
  }),
  'quizRandomizeQuestions': yup.boolean(),
  'quizRandomizeAnswers': yup.boolean(),
  'quizTimeLimitUnlimited': yup.boolean(),
  'quizTimeLimitHours': yup.number()
      .when('quizTimeLimitUnlimited', {
        is: false,
        then: (sch)  => sch
            .required()
            .min(0)
            .max(24)
            .label('Quiz Time Limit Hours')
            .typeError('Quiz Time Limit Hours must be a number between 1 and 24'),
      }),
  'quizTimeLimitMinutes': yup.number()
      .when('quizTimeLimitUnlimited', {
        is: false,
        then: (sch)  => sch
            .required()
            .min(0)
            .max(59)
            .label('Quiz Time Limit Minutes')
            .typeError('Quiz Time Minutes Hours must be a number between 1 and 59'),
      }),
})

const { values, meta, handleSubmit, isSubmitting, setFieldValue, validate, errors } = useForm({
  validationSchema: schema,
  initialValues: {
    quizLength: settings.value.quizLength.value,
    quizPassingReq: settings.value.passingReq.value,
    quizNumberOfAttemptsUnlimited: settings.value.numAttempts.unlimited,
    quizNumberOfAttempts: settings.value.numAttempts.value,
    quizRandomizeQuestions: settings.value.randomizeQuestions.value,
    quizRandomizeAnswers: settings.value.randomizeAnswers.value,
    quizTimeLimitUnlimited: settings.value.quizTimeLimit.unlimited,
    quizTimeLimitHours: hoursForQuiz.value,
    quizTimeLimitMinutes: minutesForQuiz.value
  }
})

const saveSettings = handleSubmit((values) => {
  const hasErrors = errors.value && errors.value.length > 0;
  if (hasErrors) {
    errMsg.value = 'Form did NOT pass validation, please fix and try to Save again';
  } else {
    collectAndSave(values);
  }
});

const collectAndSave = (values) => {
  let dirtySettings = Object.values(settings.value).filter((s) => {
    if (s.setting === settings.value.numAttempts.setting || s.setting === settings.value.quizTimeLimit.setting) {
      return s.unlimited !== s.lastLoadedUnlimited || (!s.unlimited && s.value !== s.lastLoadedValue);
    }
    return s.value !== s.lastLoadedValue;
  });
  if (dirtySettings) {
    isSaving.value = true;
    dirtySettings = dirtySettings.map((s) => {
      if ((s.setting === settings.value.numAttempts.setting || s.setting === settings.value.quizTimeLimit.setting) && s.unlimited) {
        return ({ ...s, value: '-1' });
      }
      return s;
    });
    QuizService.saveQuizSettings(quizId.value, dirtySettings)
        .then(() => {
          loadAndUpdateQuizSettings()
              .then(() => {
                isSaving.value = false;
                showSavedMsg.value = true;
                announcer.polite('Quiz Settings have been successfully saved');
                setTimeout(() => {
                  showSavedMsg.value = false;
                }, 4000);
              });
        });
  }
};
</script>

<template>
  <div>
    <SubPageHeader title="Settings" aria-label="Settings" />
    <SkillsSpinner :is-loading="isLoadingData"/>
    <Card>
      <template #content>
        <NoContent2 v-if="isSurveyType"
                    title="No Settings"
                    class="my-5"
                    message="Surveys do not have any available settings."
                    data-cy="noSettingsAvailable"/>
        <div v-if="!isSurveyType && !isLoadingData">
          <div class="field grid align-items-start">
            <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary"># of Questions per Quiz Attempt:</div>
            <div class="col-12 md:col-9">
              <SkillsDropDown
                  name="quizLength"
                  id="quizNumQuestions"
                  data-cy="quizNumQuestions"
                  optionLabel="text"
                  optionValue="value"
                  v-model="settings.quizLength.value"
                  :options="quizLengthOptions" />
            </div>
          </div>
          <div class="field grid align-items-start">
            <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">Passing Requirement:</div>
            <div class="col-12 md:col-9">
              <SkillsDropDown
                  name="quizPassingReq"
                  id="quizPassingReq"
                  data-cy="quizPassingSelector"
                  optionLabel="text"
                  optionValue="value"
                  v-model="settings.passingReq.value"
                  :options="numRequiredQuestionsOptions" />
            </div>
          </div>

          <div class="field grid align-items-start">
            <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">Maximum Number of Attempts:</div>
            <div class="col-12 md:col-9">
              <div class="flex flex-wrap">
                <SkillsInputSwitch
                    v-model="settings.numAttempts.unlimited"
                    name="quizNumberOfAttemptsUnlimited"
                    aria-label="Maximum Number of Attempts setting, unlimited number of attempts checkbox"
                    data-cy="unlimitedAttemptsSwitch"/>
                <span class="mx-2">Unlimited</span>
                <SkillsNumberInput
                    v-if="!settings.numAttempts.unlimited"
                    class="flex-1"
                    name="quizNumberOfAttempts"
                    aria-label="Maximum Number of Attempts"
                    data-cy="numAttemptsInput"
                    v-model="settings.numAttempts.value"/>
              </div>
            </div>
          </div>

          <div class="field grid align-items-start">
            <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">Randomize Question Order:</div>
            <div class="col-12 md:col-9">
              <SkillsInputSwitch
                  v-model="settings.randomizeQuestions.value"
                  name="quizRandomizeQuestions"
                  id="randomizeQuestions"
                  aria-label="Randomize order of the questions"
                  data-cy="randomizeQuestionSwitch"/>
              <span class="mx-2 vertical-align-top">Randomize</span>
            </div>
          </div>

          <div class="field grid align-items-start">
            <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">Randomize Answer Order:</div>
            <div class="col-12 md:col-9">
              <SkillsInputSwitch
                  v-model="settings.randomizeAnswers.value"
                  name="quizRandomizeAnswers"
                  id="randomizeAnswers"
                  aria-label="Randomize order of the answers"
                  data-cy="randomizeAnswerSwitch"/>
              <span class="mx-2 vertical-align-top">Randomize</span>
            </div>
          </div>


          <div class="field grid align-items-start">
            <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">Quiz Time Limit:</div>
            <div class="col-12 md:col-9">
              <div class="flex flex-wrap">
                <SkillsInputSwitch v-model="settings.quizTimeLimit.unlimited"
                             id="timeLimitUnlimited"
                             name="quizTimeLimitUnlimited"
                             aria-label="Quiz Time Limit setting, unlimited time checkbox"
                             data-cy="unlimitedTimeSwitch"/>
                <span class="mx-2">Unlimited</span>
                <div v-if="!settings.quizTimeLimit.unlimited" class="flex flex-row flex-1">
                  <InputGroup class="align-items-start mr-1">
                    <SkillsNumberInput
                        class="flex-1"
                        name="quizTimeLimitHours"
                        data-cy="timeLimitHoursInput"
                        aria-labelledby="hours-append"
                        v-model="hoursForQuiz"
                        @update:modelValue="updateTimeLimit">
                      <template #addOnAfter>
                        <InputGroupAddon id="hours-append">Hours</InputGroupAddon>
                      </template>
                    </SkillsNumberInput>
                  </InputGroup>
                  <InputGroup class="align-items-start ml-1">
                    <SkillsNumberInput
                        class="flex-1"
                        name="quizTimeLimitMinutes"
                        data-cy="timeLimitMinutesInput"
                        aria-labelledby="minutes-append"
                        v-model="minutesForQuiz"
                        @update:modelValue="updateTimeLimit">
                      <template #addOnAfter>
                        <InputGroupAddon id="minutes-append">Minutes</InputGroupAddon>
                      </template>
                    </SkillsNumberInput>
                  </InputGroup>
                </div>
              </div>
            </div>
          </div>

          <div v-if="errMsg" class="alert alert-danger text-red-500">
            {{ errMsg }}
          </div>

          <hr/>

          <div class="flex flex-row">
            <div class="">
              <SkillsButton variant="outline-success"
                            label="Save"
                            icon="fas fa-arrow-circle-right"
                            @click="saveSettings"
                            :disabled="!meta.valid || !hasChanged"
                            aria-label="Save Settings"
                            data-cy="saveSettingsBtn">
              </SkillsButton>

              <InlineMessage v-if="hasChanged"
                             severity="warn"
                             class="ml-2"
                             data-cy="unsavedChangesAlert"
                             aria-label="Settings have been changed, do not forget to save">
                Unsaved Changes
              </InlineMessage>
              <InlineMessage v-if="!hasChanged && showSavedMsg"
                             severity="success"
                             class="ml-2"
                             data-cy="settingsSavedAlert">
                Settings Updated!
              </InlineMessage>
            </div>
          </div>


        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>