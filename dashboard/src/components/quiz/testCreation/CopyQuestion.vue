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
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import SkillsDropDown from "@/components/utils/inputForm/SkillsDropDown.vue";
import {ref, onMounted, computed} from "vue";
import QuizService from "@/components/quiz/QuizService.js";
import {object, string} from "yup";
import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import InputSanitizer from "@/components/utils/InputSanitizer.js";

const emit = defineEmits(['copy-modal-closed'])
const model = defineModel()
const props = defineProps({
  questionDef: Object,
  questionsToSave: Object,
})
const quizDefs = ref([]);
const loadingComponent = ref(false)
const copyInProgress = ref(false);
const selectedQuiz = ref(null)
const canCopyQuestion = ref(false);
const validatingQuiz = ref(false)
let validationRequestId = 0
const copied = ref(false)
const hasValidationErrors = ref(false)
const validationErrors = ref([])
const copyError = ref('')
const loadError = ref('')

const loadQuizDefs = async () => {
  loadingComponent.value = true;
  loadError.value = ''
  try {
    const quizzes = await QuizService.getQuizDefs(false)
    quizDefs.value = quizzes.filter((quiz) => quiz.type === props.questionDef.quizType && quiz.quizId !== props.questionDef.quizId).map((quiz) =>  {
      return {
        label: quiz.name,
        id: quiz.quizId
      };
    });
  } catch (error) {
    loadError.value = error.response?.data?.explanation || 'Unable to load destination quizzes. Please try again.'
  } finally {
    loadingComponent.value = false;
  }
}

onMounted(loadQuizDefs)

const copyQuestions = async () => {
  copyInProgress.value = true;
  copyError.value = ''
  try {
    const questionsToSave = Object.keys(props.questionsToSave)
    await QuizService.copyToAnotherQuiz(props.questionDef.quizId, selectedQuiz.value, questionsToSave)
    copied.value = true;
  } catch (error) {
    copyError.value = error.response?.data?.explanation || 'Unable to copy questions. Please try again.'
  } finally {
    copyInProgress.value = false;
  }
}

const close = () => {
  emit('copy-modal-closed')
  model.value = false
}

const numberOfQuestions = computed(() => {
  return Object.keys(props.questionsToSave).length
})

const onQuizChanged = async (value) => {
  const requestId = ++validationRequestId
  canCopyQuestion.value = false
  hasValidationErrors.value = false
  validationErrors.value = []
  if (!value) {
    validatingQuiz.value = false
    return
  }

  validatingQuiz.value = true
  try {
    const copyResult = await QuizService.validateQuizCopy(props.questionDef.quizId, value, "test")
    if (requestId !== validationRequestId) return

    canCopyQuestion.value = copyResult.isAllowed
    if (!copyResult.isAllowed) {
      hasValidationErrors.value = true
      validationErrors.value = copyResult.validationErrors
    }
  } catch (error) {
    if (requestId !== validationRequestId) return
    hasValidationErrors.value = true
    validationErrors.value = [error.response?.data?.explanation || 'Unable to validate the selected quiz. Please try again.']
  } finally {
    if (requestId === validationRequestId) validatingQuiz.value = false
  }
}

const canCopy = computed(() => {
  return canCopyQuestion.value && !loadError.value && !validatingQuiz.value && !copyInProgress.value && selectedQuiz.value
})
const showOkButton = computed(() => !copied.value)
</script>

<template>
  <SkillsDialog
        ref="copyQuestionDialogRef"
        id="copyQuestionDialog"
        data-cy="copyQuestionDialog"
        :enable-return-focus="true"
        :maximizable="false"
        cancel-button-severity="secondary"
        ok-button-severity="danger"
        ok-button-icon="fas fa-copy"
        ok-button-label="Copy"
        :ok-button-disabled="!canCopy"
        v-model="model"
        :show-ok-button="showOkButton"
        :cancel-button-label="!showOkButton ? 'Close' : 'Cancel'"
        :cancel-button-severity="!showOkButton ? 'success' : 'secondary'"
        :cancel-button-icon="!showOkButton ? 'fas fa-check' : 'far fa-times-circle'"
        header="Copy Questions"
        :loading="loadingComponent"
        @on-ok="copyQuestions"
        @on-cancel="close">

    <div class="flex flex-col gap-1 mb-4" v-if="!copyInProgress && !copied">
      <Message severity="info" :closable="false">
        This will copy {{ numberOfQuestions }} question(s), including attachments, to the selected quiz.
      </Message>
      <div v-if="loadError" class="flex flex-col items-start gap-2">
        <Message severity="error" :closable="false" data-cy="quizDestinationsLoadError">
          {{ loadError }}
        </Message>
        <SkillsButton label="Retry" icon="fas fa-sync" size="small" data-cy="retryQuizDestinationsBtn" @click="loadQuizDefs" />
      </div>
      <span v-else class="font-bold text-primary">Copy Question To:</span>
      <Select
          v-if="!loadError"
          :options="quizDefs"
          aria-label="Copy Question To"
          optionLabel="label"
          v-model="selectedQuiz"
          @update:model-value="onQuizChanged"
          optionValue="id"
          data-cy="quizzes-to-copy" />
      <Message v-if="hasValidationErrors" :closable="false" severity="error" data-cy="validationFailedMsg">
        <div>Questions cannot be copied:</div>
        <ul>
          <li v-for="error in validationErrors" :key="error"><span v-html="InputSanitizer.sanitize(error)"></span></li>
        </ul>
      </Message>
      <Message v-if="copyError" :closable="false" severity="error" data-cy="copyQuestionsFailedMsg">
        {{ copyError }}
      </Message>
    </div>
    <skills-spinner :is-loading="copyInProgress" class="mt-6 mb-4"/>
    <div v-if="copied" class="mb-4">
      <Message severity="info" :closable="false">
        Successfully copied {{ numberOfQuestions }} question(s) to {{ selectedQuiz }}.
      </Message>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>