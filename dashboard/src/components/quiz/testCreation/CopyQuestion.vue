<script setup>
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import SkillsDropDown from "@/components/utils/inputForm/SkillsDropDown.vue";
import {ref, onMounted, computed} from "vue";
import QuizService from "@/components/quiz/QuizService.js";
import {object, string} from "yup";
import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";

const emit = defineEmits(['copy-modal-closed'])
const model = defineModel()
const props = defineProps({
  questionDef: Object,
  questionsToSave: Object,
})
const quizDefs = ref([]);
const loadingComponent = ref(false)
const copyInProgress = ref(false);

const initialData = {
  selectedQuiz: undefined,
}

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

const schema = object({
  'selectedQuiz': string().required().label('Copy Question To')
      .test('copyIsAllowed', 'Unmet community requirements', (value) => copyIsAllowed(value))
})

const copyIsAllowed = async (value) => {
  if(props.questionDef.quizId === value) {
    return true;
  } else {
    const response = await QuizService.validateQuizCopy(props.questionDef.quizId, value, "test")
    if(response) {
      return response.isAllowed;
    }
  }
}

const copyQuestions = async (values) => {
  const { selectedQuiz } = values
  copyInProgress.value = true;
  const questionsToSave = Object.keys(props.questionsToSave)
  await QuizService.copyToAnotherQuiz(props.questionDef.quizId, selectedQuiz, questionsToSave)
  copyInProgress.value = false;
}

const close = () => {
  emit('copy-modal-closed')
  model.value = false
}

const numberOfQuestions = computed(() => {
  return Object.keys(props.questionsToSave).length
})
</script>

<template>
  <SkillsInputFormDialog
        ref="copyQuestionDialogRef"
        id="copyQuestionDialog"
        data-cy="copyQuestionDialog"
        :enable-return-focus="true"
        :initial-values="initialData"
        :maximizable="false"
        v-model="model"
        header="Copy Questions"
        :loading="loadingComponent"
        :save-data-function="copyQuestions"
        @close="close"
        @cancelled="close"
        :validation-schema="schema">
    <div class="flex flex-col gap-1" v-if="!copyInProgress">
      <Message severity="info" :closable="false">
        This will copy {{ numberOfQuestions }} questions, including attachments, to the selected quiz.
      </Message>
      <span class="font-bold text-primary">Copy Question To:</span>
      <SkillsDropDown
          name="selectedQuiz"
          :options="quizDefs"
          aria-label="Copy Question To"
          optionLabel="label"
          optionValue="id"
          data-cy="quizzes-to-copy" />
    </div>
    <div v-if="copyInProgress">
      Copying...
    </div>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>