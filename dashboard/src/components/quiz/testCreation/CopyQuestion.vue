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
const copied = ref(false)
const hasValidationErrors = ref(false)
const validationErrors = ref([])

onMounted(() => {
  loadingComponent.value = true;
  QuizService.getQuizDefs().then((quizzes) => {
    quizDefs.value = quizzes.filter((quiz) => quiz.type === props.questionDef.quizType && quiz.quizId !== props.questionDef.quizId).map((quiz) =>  {
      return {
        label: quiz.name,
        id: quiz.quizId
      };
    });
    loadingComponent.value = false;
  })
})

const copyIsAllowed = async (value) => {
  if(props.questionDef.quizId === value) {
    return true;
  } else {
    const response = await QuizService.validateQuizCopy(props.questionDef.quizId, value, "test")
    if(response) {
      return response;
    }
  }
}

const copyQuestions = async () => {
  copyInProgress.value = true;
  const questionsToSave = Object.keys(props.questionsToSave)
  await QuizService.copyToAnotherQuiz(props.questionDef.quizId, selectedQuiz.value, questionsToSave)
  copyInProgress.value = false;
  copied.value = true;
}

const close = () => {
  emit('copy-modal-closed')
  model.value = false
}

const numberOfQuestions = computed(() => {
  return Object.keys(props.questionsToSave).length
})

const onQuizChanged = async (value) => {
  hasValidationErrors.value = false
  validationErrors.value = []
  const copyResult = await copyIsAllowed(value)
  canCopyQuestion.value = copyResult.isAllowed
  if(!copyResult.isAllowed) {
    hasValidationErrors.value = true
    validationErrors.value = copyResult.validationErrors
  }

}

const canCopy = computed(() => {
  return canCopyQuestion.value && selectedQuiz.value
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
      <span class="font-bold text-primary">Copy Question To:</span>
      <Select
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