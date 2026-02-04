/*
Copyright 2026 SkillTree

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
import {onMounted, ref} from 'vue';

import {useRoute} from 'vue-router';
import * as yup from "yup";
import {number, string} from "yup";
import {useForm} from 'vee-validate';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import QuizService from "@/components/quiz/QuizService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";
import SkillsNumberInput from "@/components/utils/inputForm/SkillsNumberInput.vue";
import ThinkingIndicator from "@/common-components/utilities/learning-conent-gen/ThinkingIndicator.vue";
import AiConfidenceTag from "@/components/quiz/testCreation/AiConfidenceTag.vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const route = useRoute();
const appConfig = useAppConfig()

const question = ref(null)
const savingSettings = ref(false)
const showSavedMsg = ref(false);
const graderEnabled = ref(true);
const initDataLoading = ref(true)

const schema = yup.object().shape({
  'answerUsedForGrading': string()
      .trim()
      .required()
      .max(appConfig.maxTextInputAiGradingCorrectAnswerLength)
      .label('Answer Used for Grading'),
  'minimumConfidenceLevel': number()
      .required()
      .min(1)
      .max(100)
      .label('Minimum Correct Confidence %'),
})
const initialValues = {
  answerUsedForGrading: '',
  minimumConfidenceLevel: 0
}
const {values, meta, handleSubmit, resetForm, validate, errors, setFieldValue, isSubmitting} = useForm({
  validationSchema: schema,
  initialValues: initialValues
})

const loadQuestion = () => {
  return QuizService.getQuizQuestionDef(route.params.quizId, route.params.questionId)
      .then((response) => {
        question.value = response
      })
}
const loadTextInputAiGradingAttrs = () => {
  return QuizService.getTextInputAiGradingAttrs(route.params.quizId, route.params.questionId)
      .then((response) => {
        graderEnabled.value = response.enabled
        setFieldValue('answerUsedForGrading', response.correctAnswer)
        setFieldValue('minimumConfidenceLevel', response.minimumConfidenceLevel)
      })
}

onMounted(() => {
  Promise.all([loadQuestion(), loadTextInputAiGradingAttrs()])
      .finally(() => {
        initDataLoading.value = false
      })
})

const overallErrMsg = ref('')
const submitSaveSettingsForm = () => {
  validate().then(({valid}) => {
    if (!valid) {
      overallErrMsg.value = 'Form did NOT pass validation, please fix and try to Save again';
    } else {
      saveSettings(values)
    }
  })
}
const saveSettings = () => {
  console.log(values)
  savingSettings.value = true
  QuizService.saveTextInputAiGradingAttrs(route.params.quizId, route.params.questionId, {
    enabled: graderEnabled.value,
    correctAnswer: values.answerUsedForGrading,
    minimumConfidenceLevel: values.minimumConfidenceLevel
  }).finally(() => {
    savingSettings.value = false
    showSavedMsg.value = true;
    setTimeout(() => {
      showSavedMsg.value = false;
    }, 3500);
  })
}

const loadingTestAnswer = ref(false)
const testAnswerRes = ref(null)
const testAnAnswer = () => {
  validate().then(({valid}) => {
    if (!valid) {
      overallErrMsg.value = 'Form did NOT pass validation, please fix and try to Save again';
    } else {
      loadingTestAnswer.value = true
      const correctAnswer = values.answerUsedForGrading
      const minimumConfidenceLevel = values.minimumConfidenceLevel
      const answerToTest = values.answerToTest
      QuizService.testTextInputAiGrading(route.params.quizId, route.params.questionId, correctAnswer, minimumConfidenceLevel, answerToTest)
          .then((res) => {
            testAnswerRes.value = {...res, minimumConfidenceLevel}
          }).finally(() => {
        loadingTestAnswer.value = false
      })
    }
  })
}
</script>

<template>
  <div>
    <SubPageHeader title="Configure AI Grader">
      <router-link :to="{ name: 'Questions' }" v-if="route.params.quizId" tabindex="-1">
        <SkillsButton size="small" icon="fas fa-arrow-alt-circle-left" label="Back" data-cy="backToQuestionsPage"/>
      </router-link>
    </SubPageHeader>
    <Card>
      <template #content>
        <skills-spinner :is-loading="initDataLoading"/>
        <div class="flex items-center gap-2 mb-2">
          <Checkbox id="enabledCheckbox" v-model="graderEnabled" binary data-cy="aiGraderEnabled"/>
          <label for="enabledCheckbox">AI Grader Enabled</label>
        </div>
        <div v-if="!initDataLoading">
          <BlockUI :blocked="!graderEnabled" class="p-2 flex flex-col gap-2">
            <div class="flex flex-col gap-4">
              <div class="flex flex-col gap-1">
                <label for="questionTxt" class="font-bold">Question:</label>
                <div class="border-2 border-dashed rounded py-2 px-4">
                  <markdown-text id="questionTxt" :text="question.question"/>
                </div>
              </div>

              <SkillsTextarea
                  label="Answer Used for Grading:"
                  id="answerUsedForGradingInput"
                  placeholder="Enter an answer that will be use for grading"
                  aria-label="Answer key"
                  rows="4"
                  max-rows="10"
                  name="answerUsedForGrading"
                  data-cy="answerForGrading"
              />
            </div>

            <div class="flex items-end gap-5">
              <div class="flex-1">
                <SkillsNumberInput
                    name="minimumConfidenceLevel"
                    label="Minimum Correct Confidence %"
                />
              </div>
            </div>
          </BlockUI>

          <div class="flex items-center gap-2 align-center mt-3">
            <SkillsButton
                severity="success"
                outlined
                data-cy="saveGraderSettingsBtn"
                aria-label="Save ai grader settings"
                @click="submitSaveSettingsForm"
                :loading="savingSettings || isSubmitting"
                :disabled="!meta.valid"
                icon="fa-solid fa-save"
                label="Save Changes"/>
            <InlineMessage v-if="showSavedMsg" aria-hidden="true" data-cy="settingsSavedMsg" severity="success" size="small"
                           icon="fas fa-check">Settings Saved
            </InlineMessage>
          </div>

          <Card v-if="graderEnabled" class="mt-5">
            <template #header>
              <SkillsCardHeader title="AI Grading Preview">
                <template #headerIcon><i class="fa-solid fa-vial mr-2" aria-hidden="true"/></template>
              </SkillsCardHeader>
            </template>
            <template #content>
              <div class="flex flex-col gap-4">
                <SkillsTextarea
                    label="Answer:"
                    id="answerToTest"
                    placeholder="Enter a sample answer to test the AI Grader and refine your grading settings."
                    aria-label="Answer To test"
                    rows="2"
                    max-rows="6"
                    name="answerToTest"
                    data-cy="answerToTestInput"
                />
                <div class="flex gap-3">
                  <SkillsButton
                      severity="info"
                      outlined
                      data-cy="testAnswersBtn"
                      aria-label="Test Answers"
                      @click="testAnAnswer"
                      :loading="savingSettings || loadingTestAnswer"
                      :disabled="!meta.valid || isSubmitting || !values.answerToTest?.trim()"
                      icon="fa-solid fa-clipboard-question"
                      label="Test Answers"/>
                  <div v-if="loadingTestAnswer" class="flex gap-2 items-center">
                    <skills-spinner :is-loading="true" :size-in-rem="2"/>
                    <div>Loading AI Grading Results. <thinking-indicator value="This may take a while"/>.</div>
                  </div>
                </div>
              </div>

              <BlockUI v-if="testAnswerRes" :blocked="loadingTestAnswer">
                <div class="mt-5 flex flex-col gap-3">
                  <div class="text-xl font-bold mb-1 border-b-2">Results:</div>
                  <div class="flex gap-3 items-center">
                    <div><span>Answer is: </span>
                      <Tag v-if="testAnswerRes.confidenceLevel >= testAnswerRes.minimumConfidenceLevel">CORRECT</Tag>
                      <Tag v-else severity="warn">WRONG</Tag>
                    </div>
                    <div>|</div>
                    <div class="flex gap-2 items-center"><span>AI Confidence Level:</span>
                      <div class="flex gap-2 items-center">
                        <span class="font-bold">{{ testAnswerRes.confidenceLevel }}%</span>
                        <ai-confidence-tag :confidence-percent="testAnswerRes.confidenceLevel" />
                      </div>
                    </div>
                  </div>
                  <div class="flex flex-col gap-2">
                    <label>AI Justification: </label>
                    <p class="border rounded p-4">{{ testAnswerRes.gradingDecisionReason }}</p>
                  </div>
                </div>
              </BlockUI>
            </template>
          </Card>
        </div>

      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>