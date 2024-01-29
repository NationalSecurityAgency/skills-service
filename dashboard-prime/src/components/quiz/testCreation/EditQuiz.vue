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
  return props.isEdit ? 'Editing Existing Quiz' : 'New Quiz'
})
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
      return context.createError({ message: result.msg })
    }
    return context.createError({ message: 'Field is invalid' })
  })
}, appConfig.formFieldDebounceInMs)

const schema = object({
  'name': string()
      .trim()
      .required()
      .min(appConfig.minNameLength)
      .max(appConfig.maxQuizNameLength)
      .nullValueNotAllowed()
      .test('uniqueName', 'Quiz Name already exist', (value) => checkQuizNameUnique(value))
      .customNameValidator()
      .label('Quiz Name'),
  'quizId': string()
      .required()
      .min(appConfig.minIdLength)
      .max(appConfig.maxIdLength)
      .nullValueNotAllowed()
      .test('uniqueId', 'Quiz ID already exist', (value) => checkQuizIdUnique(value))
      .label('Quiz Id'),
  'description': string()
      .max(appConfig.descriptionMaxLength)
      .label('Quiz Description')
      .test('customQuizDescriptionValidator', (value, context) => customQuizDescriptionValidator(value, context))
})
const initialQuizData = {
  originalQuizId: props.quiz.quizId,
  isEdit: props.isEdit,
  ...props.quiz }

const close = () => { model.value = false }

const onSubmit = (values) => {
  const quizToSave = {
    ...values,
    name: InputSanitizer.sanitize(values.name),
    quizId: InputSanitizer.sanitize(values.quizId),
  }
  emit('quiz-saved', quizToSave)
  close()
}

</script>

<template>
  <SkillsInputFormDialog
      v-model="model"
      :header="modalTitle"
      :loading="loadingComponent"
      :validation-schema="schema"
      :initial-values="initialQuizData"
      @saved="onSubmit"
      @close="close"
  >
    <template #default>
      <SkillsNameAndIdInput
          name-label="Name"
          name-field-name="name"
          id-label="Quiz/Survey ID"
          id-field-name="quizId"
          :name-to-id-sync-enabled="!props.isEdit"
          @keydown-enter="onSubmit" />
      <markdown-editor
          class="mt-5"
          name="description" />

    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>