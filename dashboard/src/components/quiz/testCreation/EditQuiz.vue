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
import { ref, computed } from 'vue'
import { boolean, object, string, ValidationError } from 'yup'
import { useDebounceFn } from '@vueuse/core'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import QuizService from '@/components/quiz/QuizService.js';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js';
import { useDescriptionValidatorService } from '@/common-components/validators/UseDescriptionValidatorService.js';
import CommunityProtectionControls from '@/components/projects/CommunityProtectionControls.vue';

const model = defineModel()
const props = defineProps({
  quiz: Object,
  isEdit: {
    type: Boolean,
    default: false,
  },
  isCopy: {
    type: Boolean,
    default: false,
  }
})
const emit = defineEmits(['quiz-saved'])
const loadingComponent = ref(false)

const modalTitle = ref('New Quiz/Survey');
const modalId = ref('newQuizDialog');
if(props.isEdit) {
  modalTitle.value = 'Editing Existing Quiz/Survey'
  modalId.value = `editQuizDialog${props.quiz.quizId}`
} else if(props.isCopy) {
  modalTitle.value = 'Copy Quiz/Survey'
  modalId.value = `copyQuizDialog${props.quiz.quizId}`
}

const appConfig = useAppConfig()

const communityLabels = useCommunityLabels()
const initialValueForEnableProtectedUserCommunity = communityLabels.isRestrictedUserCommunity(props.quiz.userCommunity)
const enableProtectedUserCommunity = ref(initialValueForEnableProtectedUserCommunity)

const checkQuizNameUnique = useDebounceFn((value) => {
  if (!value || value.length === 0) {
    return true
  }
  const origName = props.quiz.name
  if (props.isEdit && (origName === value || origName.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
    return true
  }
  return QuizService.checkIfQuizNameExist(value).then((remoteRes) => !remoteRes)
}, appConfig.formFieldDebounceInMs)
const checkQuizIdUnique = useDebounceFn((value) => {
  if (!value || value.length === 0 || (props.isEdit && props.quiz.quizId === value)) {
    return true
  }
  return QuizService.checkIfQuizIdExist(value)
      .then((remoteRes) => !remoteRes)

}, appConfig.formFieldDebounceInMs)

const descriptionValidatorService = useDescriptionValidatorService()
const checkDescription = useDebounceFn((value, testContext) => {
  if (!value || value.trim().length === 0 || !appConfig.paragraphValidationRegex) {
    return true
  }
  return descriptionValidatorService.validateDescription(value, false, enableProtectedUserCommunity.value, false).then((result) => {
    if (result.valid) {
      return true
    }
    let fieldNameToUse = 'Quiz/Survey Description'
    if (result.msg) {
      return testContext.createError({ message: `${fieldNameToUse ? `${fieldNameToUse} - ` : ''}${result.msg}` })
    }
    return testContext.createError({ message: `${fieldNameToUse || 'Field'} is invalid` })
  })

}, appConfig.formFieldDebounceInMs)

const checkUserCommunityRequirements =(value, testContext) => {
  if (!value || !props.isEdit) {
    return true;
  }
  return QuizService.validateQuizForEnablingCommunity(props.quiz.quizId).then((result) => {
    if (result.isAllowed) {
      return true;
    }
    if (result.unmetRequirements) {
      const errors = result.unmetRequirements.map((req) => {
        return testContext.createError({ message: `${req}` })
      })
      return new ValidationError(errors)
    }
    return true
  });
}

const schema = object({
  'quizName': string()
      .trim()
      .required()
      .min(appConfig.minNameLength)
      .max(appConfig.maxQuizNameLength)
      .nullValueNotAllowed()
      .test('uniqueName', 'The value for the Quiz/Survey Name is already taken', (value) => checkQuizNameUnique(value))
      .customNameValidator()
      .label('Quiz/Survey Name'),
  'quizId': string()
      .required()
      .min(appConfig.minIdLength)
      .max(appConfig.maxIdLength)
      .nullValueNotAllowed()
      .idValidator()
      .test('uniqueId', 'The value for the Quiz/Survey ID is already taken', (value) => checkQuizIdUnique(value))
      .label('Quiz/Survey ID'),
  'type': string()
      .required()
      .nullValueNotAllowed()
      .label('Type'),
  'enableProtectedUserCommunity': boolean()
      .test('communityReqValidation', 'Unmet community requirements', (value, testContext) => checkUserCommunityRequirements(value, testContext))
      .label('Enable Protected User Community'),
  'description': string()
      .max(appConfig.descriptionMaxLength)
      .test('descriptionValidation', 'Description is invalid', (value, testContext) => checkDescription(value, testContext))
      .label('Description')
})

const asyncLoadData = () => {
  const loadDescription = () => {
    if(props.isEdit || props.isCopy) {
      return QuizService.getQuizDef(props.quiz.quizId).then((data) => {
        initialQuizData.value.description = data.description ? data.description : ''
        initialQuizData.value = { ...initialQuizData.value }
        return { 'description': data.description || '' }
      })
    }
    return Promise.resolve({})
  }

  return loadDescription()
}

const initialQuizData = ref({
  quizId: props.isCopy ? `Copyof${props.quiz.quizId}` : (props.quiz.quizId || ''),
  quizName: props.isCopy ? `Copy of ${props.quiz.name}` : (props.quiz.name || ''),
  type: props.quiz.type || '',
  description: props.quiz.description || '',
  enableProtectedUserCommunity: false,
})
const close = () => { model.value = false }

const saveQuiz = (values) => {
  const quizToSave = {
    ...values,
    originalQuizId: props.quiz.quizId,
    name: InputSanitizer.sanitize(values.quizName),
    quizId: InputSanitizer.sanitize(values.quizId),
  }
  if (initialValueForEnableProtectedUserCommunity) {
    quizToSave.enableProtectedUserCommunity = initialValueForEnableProtectedUserCommunity
  }
  if(props.isCopy) {
    return QuizService.copyQuiz(quizToSave).then((newQuizDef) => {
      return {
        ...newQuizDef,
        originalQuizId: newQuizDef.quizId
      }
    })
  }
  return QuizService.updateQuizDef(quizToSave)
    .then((updatedQuizDef) => {
      return {
        ...updatedQuizDef,
        originalQuizId: props.quiz.quizId,
      }
    })
}
const onSavedQuiz = (savedQuiz) => {
  emit('quiz-saved', savedQuiz)
  close()
}

</script>

<template>
  <SkillsInputFormDialog
      :id="modalId"
      v-model="model"
      :is-edit="isEdit"
      :is-copy="isCopy"
      :should-confirm-cancel="true"
      :header="modalTitle"
      :loading="loadingComponent"
      :validation-schema="schema"
      :initial-values="initialQuizData"
      :async-load-data-function="asyncLoadData"
      :save-data-function="saveQuiz"
      @saved="onSavedQuiz"
      @close="close"
  >
    <template #default>
      <SkillsNameAndIdInput
          name-label="Name"
          name-field-name="quizName"
          id-label="Quiz/Survey ID"
          id-field-name="quizId"
          :name-to-id-sync-enabled="!isEdit" />

      <community-protection-controls
          class="mb-3"
          v-model:enable-protected-user-community="enableProtectedUserCommunity"
          :quiz="quiz"
          :is-edit="isEdit"
          :is-copy="isCopy" />

      <div data-cy="quizTypeSection">
        <SkillsDropDown
            label="Type"
            name="type"
            data-cy="quizTypeSelector"
            :isRequired="true"
            :disabled="isEdit || isCopy"
            :options="['Quiz', 'Survey']" />
          <div v-if="isEdit || isCopy" class="text-muted-color italic text-ms">** Can only be modified for a new quiz/survey **</div>
      </div>

      <markdown-editor
          id="quizDescription"
          :quiz-id="isEdit ? quiz.quizId : null"
          :upload-url="isEdit   ? `/admin/quiz-definitions/${props.quiz.quizId}/upload` : null"
          :user-community="isEdit ? props.quiz.userCommunity : null"
          :allow-attachments="isEdit"
          data-cy="quizDescription"
          class="mt-8"
          name="description" />

    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>