<script setup>
import { ref, computed } from 'vue'
import { object, string } from 'yup'
import { useDebounceFn } from '@vueuse/core'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import QuizService from '@/components/quiz/QuizService.js';
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import DescriptionValidatorService from '@/common-components/validators/DescriptionValidatorService.js'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';

const model = defineModel()
const props = defineProps({
  quiz: Object,
  isEdit: {
    type: Boolean,
    default: false,
  }
})
const emit = defineEmits(['quiz-saved'])
const loadingComponent = ref(false)

const modalTitle = computed(() => {
  return props.isEdit ? 'Editing Existing Quiz/Survey' : 'New Quiz/Survey'
})
const modalId = props.isEdit ? `ediQuizDialog${props.quiz.quizId}` : 'newQuizDialog'
const appConfig = useAppConfig()


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

const customQuizDescriptionValidator = useDebounceFn((value, context) => {
  if (!value || value.trim().length === 0 || !appConfig.paragraphValidationRegex) {
    return true
  }

  return DescriptionValidatorService.validateDescription(value, false).then((result) => {
    if (result.valid) {
      return true
    }
    if (result.msg) {
      return context.createError({ message: `Quiz/Survey Description - ${result.msg}` })
    }
    return context.createError({ message: 'Quiz/Survey Description is invalid' })
  })
}, appConfig.formFieldDebounceInMs)

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
      .test('uniqueId', 'The value for the Quiz/Survey ID is already taken', (value) => checkQuizIdUnique(value))
      .label('Quiz/Survey ID'),
  'type': string()
      .required()
      .nullValueNotAllowed()
      .label('Type'),
  'description': string()
      .max(appConfig.descriptionMaxLength)
      .label('Description')
      .test('customQuizDescriptionValidator', (value, context) => customQuizDescriptionValidator(value, context))
})
const loadDescription = () => {
  return QuizService.getQuizDef(props.quiz.quizId).then((data) => {
    return { 'description': data.description || '' }
  })
}
const asyncLoadData = props.isEdit ? loadDescription : null
const initialQuizData = {
  originalQuizId: props.quiz.quizId,
  quizId: props.quiz.quizId,
  quizName: props.quiz.name,
  type: props.quiz.type,
  // description: props.quiz.description || '',
}
const close = () => { model.value = false }

const onSubmit = (values) => {
  const quizToSave = {
    ...values,
    name: InputSanitizer.sanitize(values.quizName),
    quizId: InputSanitizer.sanitize(values.quizId),
  }
  emit('quiz-saved', quizToSave)
  close()
}
</script>

<template>
  <SkillsInputFormDialog
      :id="modalId"
      v-model="model"
      :header="modalTitle"
      :loading="loadingComponent"
      :validation-schema="schema"
      :initial-values="initialQuizData"
      :async-load-data-function="asyncLoadData"
      @saved="onSubmit"
      @close="close"
  >
    <template #default>
      <SkillsNameAndIdInput
          name-label="Name"
          name-field-name="quizName"
          id-label="Quiz/Survey ID"
          id-field-name="quizId"
          :name-to-id-sync-enabled="!isEdit"
          @keydown-enter="onSubmit" />

      <div data-cy="quizTypeSection">
        <SkillsDropDown
            label="Type"
            name="type"
            data-cy="quizTypeSelector"
            :isRequire="true"
            :disabled="isEdit"
            :options="['Quiz', 'Survey']" />
          <div v-if="isEdit" class="text-color-secondary font-italic text-ms">** Can only be modified for a new quiz/survey **</div>
      </div>

      <markdown-editor
          id="quizDescription"
          :quiz-id="isEdit ? quiz.quizId : null"
          data-cy="quizDescription"
          class="mt-5"
          name="description" />

    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>