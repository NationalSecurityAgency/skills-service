/*
Copyright 2024 SkillTree

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
<script setup>

import SkillsDropDown from "@/components/utils/inputForm/SkillsDropDown.vue";
import SkillsInputSwitch from "@/components/utils/inputForm/SkillsInputSwitch.vue";
import SkillsNumberInput from "@/components/utils/inputForm/SkillsNumberInput.vue";
import {useForm} from "vee-validate";
import * as yup from "yup";
import {computed, ref, watch} from "vue";

const emit = defineEmits(['save-settings']);
const props = defineProps({
  initialValues: {
    type: Object,
  },
  quizSummaryState: {
    type: Object,
  },
  showSavedMsg: {
    type: Boolean,
    default: false,
  },
  isSurvey: {
    type: Boolean,
    default: false,
  }
})

const quizTimeLimit = ref(0);
const errMsg = ref('')
const schema = props.isSurvey ? yup.object().shape({'quizMultipleTakes': yup.boolean()}) : yup.object().shape({
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
  'quizMultipleTakes': yup.boolean(),
  'quizAlwaysShowCorrectAnswers': yup.boolean(),
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

const { values, meta, handleSubmit, errors, resetForm } = useForm({
  validationSchema: schema,
  initialValues: props.initialValues,
})

watch(() => props.initialValues, (newProps) => {
  resetForm({values: newProps});
});

const lessThanOrEqualToLength = (value) => {
  const quizNumQuestions = values.quizLength;
  return (quizNumQuestions !== '-1' && parseInt(value, 10) <= parseInt(quizNumQuestions, 10)) || quizNumQuestions === '-1';
}
const greaterThanOrEqualToPassing = (value) => {
  const quizPassingQuestions = values.quizPassingReq;
  return (value !== '-1' && parseInt(value, 10) >= parseInt(quizPassingQuestions, 10)) || value === '-1';
}

const numRequiredQuestionsOptions = computed(() => {
  const num = props.quizSummaryState.quizSummary ? props.quizSummaryState.quizSummary.numQuestions : 0;
  const questionBasedOptions = Array.from({ length: num }, (_, index) => ({ value: `${index + 1}`, text: `${index + 1} Correct Question${index > 0 ? 's' : ''}` }));
  return [{ value: '-1', text: 'ALL Questions - 100%' }].concat(questionBasedOptions);
})
const quizLengthOptions = computed(() => {
  const num = props.quizSummaryState.quizSummary ? props.quizSummaryState.quizSummary.numQuestions : 0;
  const questionBasedOptions = Array.from({ length: num }, (_, index) => ({ value: `${index + 1}`, text: `${index + 1} Question${index > 0 ? 's' : ''}` }));
  return [{ value: '-1', text: 'ALL Questions - 100%' }].concat(questionBasedOptions);
})

const saveSettings = handleSubmit((values) => {
  const hasErrors = errors.value && errors.value.length > 0;
  if (hasErrors) {
    errMsg.value = 'Form did NOT pass validation, please fix and try to Save again';
  } else {
    const keys = Object.keys(values);
    const newValues = [];
    keys.forEach((key) => {
      if(values[key] !== meta.value.initialValues[key])
        if(key === 'quizNumberOfAttemptsUnlimited') {
          newValues.push({
            unlimited: values.quizNumberOfAttemptsUnlimited,
            value: values.quizNumberOfAttemptsUnlimited ? -1 : values.quizNumberOfAttempts,
            setting: 'quizNumberOfAttempts'
          });
        } else if(key === 'quizTimeLimitUnlimited' || key === 'quizTimeLimitHours' || key === 'quizTimeLimitMinutes') {
          updateTimeLimit();
          newValues.push({
            unlimited: values.quizTimeLimitUnlimited,
            value: values.quizTimeLimitUnlimited ? -1 : quizTimeLimit.value,
            setting: 'quizTimeLimit'
          })
        } else {
          newValues.push({
            value: values[key],
            setting: key
          });
        }
    });
    emit('save-settings', newValues);
  }
});

const updateTimeLimit = () => {
  quizTimeLimit.value = ((parseInt(values.quizTimeLimitHours, 10) * 60) + parseInt(values.quizTimeLimitMinutes, 10)) * 60;
};
</script>

<template>
  <div class="field grid align-items-start" v-if="!isSurvey">
    <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">
      <label for="quizNumQuestions"># of Questions per Quiz Attempt:</label>
    </div>
    <div class="col-12 md:col-9">
      <SkillsDropDown
          name="quizLength"
          inputId="quizNumQuestions"
          data-cy="quizNumQuestions"
          optionLabel="text"
          optionValue="value"
          :options="quizLengthOptions" />
    </div>
  </div>
  <div class="field grid align-items-start" v-if="!isSurvey">
    <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">
      <label for="quizPassingReq">Passing Requirement:</label>
    </div>
    <div class="col-12 md:col-9">
      <SkillsDropDown
          name="quizPassingReq"
          inputId="quizPassingReq"
          data-cy="quizPassingSelector"
          optionLabel="text"
          optionValue="value"
          :options="numRequiredQuestionsOptions" />
    </div>
  </div>

  <div class="field grid align-items-start" v-if="!isSurvey">
    <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">
      <label for="quizNumberOfAttemptsUnlimited">Maximum Number of Attempts:</label>
    </div>
    <div class="col-12 md:col-9">
      <div class="flex flex-wrap">
        <SkillsInputSwitch
            name="quizNumberOfAttemptsUnlimited"
            inputId="quizNumberOfAttemptsUnlimited"
            aria-label="Maximum Number of Attempts setting, unlimited number of attempts checkbox"
            data-cy="unlimitedAttemptsSwitch"/>
        <span class="mx-2">Unlimited</span>
        <div v-if="!values.quizNumberOfAttemptsUnlimited" class="flex-1 border-left-1 ml-2 pl-2">
          <SkillsNumberInput
              label="Number of Attempts"
              id="numAttemptsInput"
              name="quizNumberOfAttempts"
              aria-label="Maximum Number of Attempts"
              data-cy="numAttemptsInput" />
        </div>
      </div>
    </div>
  </div>

  <div class="field grid align-items-start" v-if="!isSurvey">
    <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">
      <label for="randomizeQuestions">Randomize Question Order:</label>
    </div>
    <div class="col-12 md:col-9">
      <SkillsInputSwitch
          name="quizRandomizeQuestions"
          inputId="randomizeQuestions"
          aria-label="Randomize order of the questions"
          data-cy="randomizeQuestionSwitch"/>
      <span class="mx-2 vertical-align-top">Randomize</span>
    </div>
  </div>

  <div class="field grid align-items-start" v-if="!isSurvey">
    <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">
      <label for="randomizeAnswers">
        Randomize Answer Order:
      </label>
    </div>
    <div class="col-12 md:col-9">
      <SkillsInputSwitch
          name="quizRandomizeAnswers"
          inputId="randomizeAnswers"
          aria-label="Randomize order of the answers"
          data-cy="randomizeAnswerSwitch"/>
      <span class="mx-2 vertical-align-top">Randomize</span>
    </div>
  </div>


  <div class="field grid align-items-start" v-if="!isSurvey">
    <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">
      <label for="timeLimitUnlimited">Quiz Time Limit:</label>
    </div>
    <div class="col-12 md:col-9">
      <div class="flex flex-wrap">
        <SkillsInputSwitch inputId="timeLimitUnlimited"
                           name="quizTimeLimitUnlimited"
                           aria-label="Quiz Time Limit setting, unlimited time checkbox"
                           data-cy="unlimitedTimeSwitch"/>
        <div class="flex flex-column flex-1">
          <div class="mx-2">Unlimited</div>
          <div v-if="!values.quizTimeLimitUnlimited" class="flex flex-column sm:flex-row flex-1 gap-2 mt-3">
            <SkillsNumberInput
                class="flex-1"
                label="Hours"
                name="quizTimeLimitHours"
                data-cy="timeLimitHoursInput"
                aria-labelledby="hours-append"
                @update:modelValue="updateTimeLimit"/>
            <SkillsNumberInput
                class="flex-1"
                label="Minutes"
                name="quizTimeLimitMinutes"
                data-cy="timeLimitMinutesInput"
                aria-labelledby="minutes-append"
                @update:modelValue="updateTimeLimit"/>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="field grid align-items-start">
    <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">
      <label for="multipleTakes">Allow Retakes After Completion:</label>
    </div>
    <div class="col-12 md:col-9">
      <SkillsInputSwitch
          name="quizMultipleTakes"
          inputId="multipleTakes"
          aria-label="Allow retaking the quiz/survey after passing"
          data-cy="multipleTakesSwitch"/>
      <span class="mx-2 vertical-align-top">Allow</span>
    </div>
  </div>

  <div class="field grid align-items-start">
    <div class="col-12 mb-2 md:col-3 md:mb-0 text-color-secondary">
      <label for="alwaysShowCorrectAnswers">Show Correct Answers On Failure:</label>
    </div>
    <div class="col-12 md:col-9">
      <SkillsInputSwitch
          name="quizAlwaysShowCorrectAnswers"
          inputId="alwaysShowCorrectAnswers"
          aria-label="Allow retaking the quiz after passing"
          data-cy="alwaysShowCorrectAnswersSwitch"/>
      <span class="mx-2 vertical-align-top">Enabled</span>
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
                    :disabled="!meta.valid || !meta.dirty"
                    aria-label="Save Settings"
                    data-cy="saveSettingsBtn">
      </SkillsButton>

      <InlineMessage v-if="meta.dirty"
                     severity="warn"
                     class="ml-2"
                     data-cy="unsavedChangesAlert"
                     aria-label="Settings have been changed, do not forget to save">
        Unsaved Changes
      </InlineMessage>
      <InlineMessage v-if="!meta.dirty && showSavedMsg"
                     severity="success"
                     class="ml-2"
                     data-cy="settingsSavedAlert">
        Settings Updated!
      </InlineMessage>
    </div>
  </div>
</template>

<style scoped>

</style>