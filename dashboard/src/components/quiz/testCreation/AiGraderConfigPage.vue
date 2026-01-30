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
import {ref, onMounted, computed} from 'vue';
import {useRoute} from 'vue-router';
import * as yup from "yup";
import { useForm } from 'vee-validate';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import QuizService from "@/components/quiz/QuizService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";
import SkillsNumberInput from "@/components/utils/inputForm/SkillsNumberInput.vue";

const route = useRoute();

const question = ref(null)
const savingSettings = ref(false)
const showSavedMsg = ref(false);
const graderEnabled = ref(true);
const initDataLoading = ref(true)

const schema = yup.object().shape({

})
const initialValues = {
  answerUsedForGrading: '',
  minimumConfidenceLevel: 0
}
const { values, meta, handleSubmit, resetForm, validate, errors, setFieldValue } = useForm({
  validationSchema: schema,
  initialValues: initialValues
})

const loadQuestion = () => {
  return  QuizService.getQuizQuestionDef(route.params.quizId, route.params.questionId)
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
</script>

<template>
  <div>
    <SubPageHeader title="Configure AI Grader">
      <router-link :to="{ name: 'Questions' }" v-if="route.params.quizId" tabindex="-1">
        <SkillsButton size="small" icon="fas fa-arrow-alt-circle-left" label="Back"/>
      </router-link>
    </SubPageHeader>
    <Card>
      <template #content>
        <skills-spinner :is-loading="initDataLoading"/>
        <div class="flex items-center gap-2 mb-2">
          <Checkbox id="enabledCheckbox" v-model="graderEnabled" binary/>
          <label for="enabledCheckbox">AI Grader Enabled</label>
        </div>
        <div v-if="!initDataLoading">
          <BlockUI :blocked="!graderEnabled" class="p-2">
            <div class="flex flex-col gap-3">
              <div class="flex flex-col gap-1">
                <label for="questionTxt">Question:</label>
                <div class="border rounded py-2 px-4">
                  <markdown-text id="questionTxt" :text="question.question"/>
                </div>
              </div>

              <SkillsTextarea
                  label="Answer Used for Grading:"
                  id="answerUsedForGradingInput"
                  placeholder="Enter an answer that will be use for grading"
                  aria-label="Answer key"
                  rows="6"
                  max-rows="6"
                  name="answerUsedForGrading"
                  data-cy="videoCaptions"
              />
            </div>

            <div class="flex items-end gap-5">
              <div class="flex-1">
                <SkillsNumberInput
                    name="minimumConfidenceLevel"
                    label="Minimum Correct Confidence %"
                />
              </div>
              <div>
                <SkillsButton
                    severity="warn"
                    outlined
                    data-cy="testAnswersBtn"
                    aria-label="Test Answers"
                    @click="submitSaveSettingsForm"
                    :disabled="!graderEnabled"
                    icon="fa-solid fa-clipboard-question"
                    label="Test Answers"/>
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
                :loading="savingSettings"
                icon="fa-solid fa-save"
                label="Save Changes"/>
            <InlineMessage v-if="showSavedMsg" aria-hidden="true" data-cy="savedMsg" severity="success" size="small"
                           icon="fas fa-check">Settings Saved
            </InlineMessage>
          </div>
        </div>

      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>