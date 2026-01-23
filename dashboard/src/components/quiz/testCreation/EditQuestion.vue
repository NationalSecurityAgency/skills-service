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
import {computed, nextTick, onMounted, ref} from 'vue'
import {array, boolean, object, string} from 'yup'
import {useRoute} from 'vue-router';
import {useAppConfig} from '@/common-components/stores/UseAppConfig.js'
import QuizService from '@/components/quiz/QuizService.js';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import ConfigureAnswers from '@/components/quiz/testCreation/ConfigureAnswers.vue';
import QuizType from "@/skills-display/components/quiz/QuizType.js";
import MatchingQuestion from "@/components/quiz/testCreation/MatchingQuestion.vue";
import GenerateSingleQuestionDialog
  from "@/common-components/utilities/learning-conent-gen/GenerateSingleQuestionDialog.vue";
import QuestionTypeDropDown from "@/components/quiz/testCreation/QuestionTypeDropDown.vue";

const model = defineModel()
const props = defineProps({
  questionDef: Object,
  isEdit: {
    type: Boolean,
    default: false,
  },
  isCopy: {
    type: Boolean,
    default: false,
  }
})
const emit = defineEmits(['question-saved'])
const route = useRoute()
const loadingComponent = ref(false)
const currentScaleOptions = ref([3, 4, 5, 6, 7, 8, 9, 10])
const answersRef = ref(null)
const showHint = ref(false)

const modalTitle = computed(() => {
  if( props.isEdit ) {
    return 'Editing Existing Question';
  } else if( props.isCopy ) {
    return 'Copy Question'
  }
  return 'New Question'
})
const modalId = props.isEdit || props.isCopy ? `questionEditModal${props.questionDef.id}` : 'questionEditModal'
const appConfig = useAppConfig()

onMounted(() => {
  if (props.questionDef.questionType === QuestionType.Rating && (props.isEdit || props.isCopy)) {
    initialQuestionData.currentScaleValue = props.questionDef.answers.length;
  }
  if (props.isEdit || props.isCopy) {
    questionType.value.selectedType = questionType.value.options.find((o) => o.id === props.questionDef.questionType)
  }
  if (isQuizType.value && props.questionDef.answerHint) {
    showHint.value = true;
  }
});

function questionTypeChanged(inputItem) {
  questionType.value.selectedType = inputItem;
  if (!inputItem.isInitialLoad && !inputItem.doNotResetOrReplaceAnswers) {
    if (isSurveyType.value
        && inputItem.id !== QuestionType.TextInput && inputItem.id !== QuestionType.Rating && inputItem.id !== QuestionType.Matching
        && (!initialQuestionData.answers || initialQuestionData.answers.length < 2)) {
      nextTick(() => {
        answersRef.value.replaceAnswers(defaultAnswers);
      })
    } else {
      if(answersRef.value) {
        if(inputItem.id === QuestionType.Matching) {
          answersRef.value.replaceAnswers(defaultAnswers);
        }
        answersRef.value.resetAnswers()
      }
    }
  }
}

const defaultAnswers = [{
  id: null,
  answer: '',
  isCorrect: false,
  multiPartAnswer: { term: '', value: '' }
}, {
  id: null,
  answer: '',
  isCorrect: false,
  multiPartAnswer: { term: '', value: '' }
}];

const questionTypes = [{
  label: 'Multiple Answers',
  description: 'Two or more correct answers',
  prop: 'extra',
  id: QuestionType.MultipleChoice,
  icon: 'fas fa-tasks',
}, {
  label: 'Multiple Choice',
  description: 'A single correct answer',
  id: QuestionType.SingleChoice,
  icon: 'far fa-check-square',
}, {
  label: 'Input Text',
  description: 'A free-form text answer',
  id: QuestionType.TextInput,
  icon: 'far fa-keyboard',
}]
if (QuizType.isSurvey(props.questionDef.quizType)) {
  questionTypes.push({
    label: 'Rating',
    description: 'A star-based rating',
    id: QuestionType.Rating,
    icon: 'fa fa-star',
  })
} else {
  questionTypes.push({
    label: 'Matching',
    description: 'Match terms',
    id: QuestionType.Matching,
    icon: 'fas fa-diagram-project',
  })
}

const questionType = ref({
  options: questionTypes,
  selectedType: {
    label: 'Multiple Answers',
    id: QuestionType.MultipleChoice,
    icon: 'fas fa-tasks',
  },
})
const isSurveyType = computed(() => {
  return QuizType.isSurvey(props.questionDef.quizType)
})
const isQuizType = computed(() => {
  return QuizType.isQuiz(props.questionDef.quizType);
})
const isQuestionTypeTextInput = computed(() => {
  return questionType.value.selectedType && questionType.value.selectedType.id === QuestionType.TextInput;
})
const isQuestionTypeRatingInput = computed(() => {
  return questionType.value.selectedType && questionType.value.selectedType.id === QuestionType.Rating;
})
const isQuestionTypeMatching = computed(() => {
  return questionType.value.selectedType && questionType.value.selectedType.id === QuestionType.Matching;
})
const isQuestionTypeMultipleChoice = computed(() => {
  return questionType.value.selectedType && questionType.value.selectedType.id === QuestionType.MultipleChoice;
})
const isQuestionTypeSingleChoice = computed(() => {
  return questionType.value.selectedType && questionType.value.selectedType.id === QuestionType.SingleChoice;
})
const quizType = computed(() => {
  return props.questionDef.quizType;
})
const title = computed(() => {
  if ( props.isEdit ) {
    return 'Editing Existing Question';
  } else if( props.isCopy ) {
    return 'Copy Question';
  }
  return 'New Question';
})
const quizId = computed(() => {
  return props.questionDef.quizId ? props.questionDef.quizId : route.params.quizId;
})


const isDirty = ref(false)
const answersErrorMessage = ref('')
const atLeastOneCorrectAnswer = (value) => {
  if (isSurveyType.value || !isDirty.value || isQuestionTypeTextInput.value || isQuestionTypeRatingInput.value || isQuestionTypeMatching.value) {
    return true;
  }
  if (value === undefined) {
    return false
  }
  const numCorrect = value.filter((a) => a.isCorrect).length;
  return numCorrect >= 1;
}
const atLeastTwoAnswersFilledIn = (value) => {
  if (value === undefined) {
    return false
  }
  if (!isDirty.value || isQuestionTypeTextInput.value || isQuestionTypeRatingInput.value || isQuestionTypeMatching.value) {
    return true;
  }
  const numWithContent = value.filter((a) => (a.answer && a.answer.trim().length > 0)).length;
  return numWithContent >= 2;
}
const correctAnswersMustHaveText = (value) => {
  if (value === undefined) {
    return false
  }
  if (isSurveyType.value || !isDirty.value || isQuestionTypeTextInput.value || isQuestionTypeRatingInput.value || isQuestionTypeMatching.value) {
    return true;
  }
  const correctWithoutText = value.filter((a) => (a.isCorrect && (!a.answer || a.answer.trim().length === 0))).length;
  return correctWithoutText === 0;
}
const maxNumAnswers = (value) => {
  if (isQuestionTypeTextInput.value || isQuestionTypeRatingInput.value) {
    return true;
  }
  return value && value.length <= appConfig.maxAnswersPerQuizQuestion;
}
const singleChoiceQuestionsMustHave1Answer = (value) => {
  if (value === undefined) {
    return false
  }
  if (isSurveyType.value || !isDirty.value || !QuestionType.isSingleChoice(questionType.value.selectedType.id)) {
    return true;
  }
  const numCorrect = value.filter((a) => (a.isCorrect)).length;
  return numCorrect === 1;
}
const multipleChoiceQuestionsMustHaveAtLeast2Answer = (value) => {
  if (value === undefined) {
    return false
  }
  if (isSurveyType.value || !isDirty.value || !QuestionType.isMultipleChoice(questionType.value.selectedType.id)) {
    return true;
  }
  const numCorrect = value.filter((a) => (a.isCorrect)).length;
  return numCorrect >= 2;
}
const matchesMustNotBeBlank = (value) => {
  if(!isQuestionTypeMatching.value) {
    return true;
  }

  let emptyAnswers = []
  value.forEach((term) => {
    let answer = term.multiPartAnswer
    if(answer) {
      if(!answer.term || !answer.value || answer.term.trim() === '' || answer.value.trim() === '') {
        emptyAnswers.push(term);
      }
    }
  })
  return emptyAnswers.length === 0;
}
const noRepeatAnswers = (value) => {
  if(!isQuestionTypeMatching.value) {
    return true;
  }
  const terms = value.map((term) => term.multiPartAnswer?.term)
  const values = value.map((term) => term.multiPartAnswer?.value)
  const hasDuplicateTerms = (new Set(terms)).size !== terms.length
  const hasDuplicateValues = (new Set(values)).size !== values.length

  return !hasDuplicateTerms && !hasDuplicateValues;
}

const schema = object({
  'questionType': object()
      .required()
      .label('Type'),
  'question': string()
      .required()
      // .nullValueNotAllowed()
      .max(appConfig.descriptionMaxLength)
      .customDescriptionValidator('Question', false)
      .label('Question'),
  'answerHint': string()
      .test('answerHintRequired', 'Answer Hint is required when enabled', (value) => !!(!showHint.value || value))
      .max(appConfig.maxQuizAnswerHintLength)
      .customDescriptionValidator('Answer Hint', false)
      .label('Answer Hint'),
  'answers': array()
      .of(
          object({
            'answer': string().nullable().label('Answer'),
            'isCorrect': boolean().label('Is Correct'),
          })
      )
      .test('atLeastOneCorrectAnswer', 'Must have at least 1 correct answer selected', (value) => atLeastOneCorrectAnswer(value))
      .test('atLeastTwoAnswersFilledIn', 'Must have at least 2 answers', (value) => atLeastTwoAnswersFilledIn(value))
      .test('correctAnswersMustHaveText', 'Answers labeled as correct must have text', (value) => correctAnswersMustHaveText(value))
      .test('maxNumAnswers', `Exceeded maximum number of [${appConfig.maxAnswersPerQuizQuestion}] answers`, (value) => maxNumAnswers(value))
      .test('singleChoiceQuestionsMustHave1Answer', 'Multiple Choice Question must have 1 correct answer', (value) => singleChoiceQuestionsMustHave1Answer(value))
      .test('multipleChoiceQuestionsMustHaveAtLeast2Answer', 'Multiple Answers Question must have at least 2 correct answers', (value) => multipleChoiceQuestionsMustHaveAtLeast2Answer(value))
      .test('matchesMustNotBeBlank', 'Answers must include both a term and a value', (value) => matchesMustNotBeBlank(value))
      .test('noRepeatAnswers', 'Answers can not contain duplicate terms or values', (value) => noRepeatAnswers(value))
  ,
})
const initialQuestionData = {
  questionType: props.isEdit || props.isCopy ? questionType.value.options.find((o) => o.id === props.questionDef.questionType) : questionType.value.selectedType,
  question: props.questionDef.question || '',
  answerHint: props.questionDef.answerHint || '',
  answers: props.questionDef.answers || [],
  currentScaleValue: props.questionDef.questionType === QuestionType.Rating && props.isEdit || props.isCopy ? props.questionDef.answers.length : 5,
}

const close = () => { model.value = false }

const saveQuestionDef = (values) => {
  const { question, answerHint, answers, currentScaleValue } = values
  let processedAnswers = answers
  let { questionType : { id : questionType } } = values

  if(QuestionType.isMatching(questionType)) {
    processedAnswers.forEach((answer) => {
      answer.isCorrect = true
    })
  } else {
    processedAnswers.forEach((answer) => {
      delete answer.multiPartAnswer
    })
    processedAnswers = answers.filter((a) => a.answer && a.answer.trim().length > 0)
  }

  if(!QuestionType.isMatching(questionType)) {
    // address a race condition where isCorrect could be undefined
    processedAnswers = processedAnswers.map((ans) => ans.isCorrect !== undefined ? ans : ({...ans, isCorrect: false}))
  }
  const questionToSave = {
    id: props.questionDef.id,
    quizId: quizId.value,
    question,
    answerHint,
    questionType,
    answers: (questionType === QuestionType.TextInput || questionType === QuestionType.Rating) ? [] : processedAnswers,
  };
  if (questionType === QuestionType.Rating) {
    questionToSave.questionScale = currentScaleValue;
  }

  if (props.isEdit) {
    return QuizService.updateQuizQuestionDef(quizId.value, questionToSave)
        .then((updatedQuizQuestionDef) => {
          return {
            ...updatedQuizQuestionDef,
            isEdit: props.isEdit,
          }
        });
  } else {
    return QuizService.saveQuizQuestionDef(quizId.value, questionToSave)
        .then((updatedQuizQuestionDef) => {
          return {
            ...updatedQuizQuestionDef,
            isEdit: props.isEdit,
          }
        });
  }
}
const onSavedQuestion = (savedQuestion) => {
  emit('question-saved', savedQuestion)
  close()
}

const showAiButton = computed(() => !props.disableAiPrompt && appConfig.enableOpenAIIntegration)
const showGenQDialog = ref(false)

const skillsInputFormDialogRef = ref(null)
const markdownEditorRef = ref(null)
const onQuestionGenerated = (questionInfo) => {
  markdownEditorRef.value.setMarkdownText(questionInfo.question)
  const newQType = questionType.value.options.find((o) => o.id === questionInfo.questionTypeId)
  skillsInputFormDialogRef.value.setFieldValue('questionType', {...newQType, doNotResetOrReplaceAnswers: true})

  if (QuestionType.isMultipleChoice(questionInfo.questionTypeId) || QuestionType.isSingleChoice(questionInfo.questionTypeId) || QuestionType.isMatching(questionInfo.questionTypeId)) {
    const existingValues = skillsInputFormDialogRef.value.getFieldValues()
    const existingAnswers = existingValues.answers
    const answersToSet = questionInfo.answers.map((a, index) => {
      const id = existingAnswers.length > index ? existingAnswers[index].id : null
      const answer = { ...a, id, displayOrder: (index + 1) }
      if (a.multiPartAnswer) {
        answer.multiPartAnswer = { ...a.multiPartAnswer }
      }
      return answer
    })
    skillsInputFormDialogRef.value.setFieldValue('answers', answersToSet)
    answersRef.value.replaceAnswers(answersToSet)
  }

  setTimeout(() => {
    skillsInputFormDialogRef.value?.validate()
  }, 500)
}

const existingQuestionInfo = ref(null)
const startAiAssistant = () => {
  const fieldValues = skillsInputFormDialogRef.value.getFieldValues()
  if (fieldValues.question?.trim()?.length > 0) {
    existingQuestionInfo.value = {
      question: fieldValues.question,
      answers: fieldValues.answers?.filter((a) => a.answer?.trim()?.length > 0).map((a) => ({...a}))
    }
  } else {
    existingQuestionInfo.value = null
  }
  showGenQDialog.value = true
}
</script>

<template>
  <SkillsInputFormDialog
      ref="skillsInputFormDialogRef"
      :id="modalId"
      v-model="model"
      :is-edit="isEdit"
      :is-copy="isCopy"
      :header="modalTitle"
      :loading="loadingComponent"
      :validation-schema="schema"
      :initial-values="initialQuestionData"
      :save-data-function="saveQuestionDef"
      @saved="onSavedQuestion"
      @close="close"
      @isDirty="isDirty = !isDirty"
      @errors="answersErrorMessage = $event['answers']"
  >
    <template #default>
      <div class="flex justify-end">
        <SkillsButton v-if="showAiButton"
                      icon="fa-solid fa-wand-magic-sparkles"
                      label="AI"
                      size="small"
                      data-cy="aiButton"
                      @click="startAiAssistant"/>
      </div>
      <generate-single-question-dialog
          v-if="showGenQDialog"
          ref="generateDescriptionDialogRef"
          v-model="showGenQDialog"
          :question-type="questionType"
          :existing-question="existingQuestionInfo"
          @question-generated="onQuestionGenerated"
      />

      <markdown-editor
          ref="markdownEditorRef"
          id="quizDescription"
          :quiz-id="quizId"
          :upload-url="`/admin/quiz-definitions/${route.params.quizId}/upload`"
          :allow-community-elevation="true"
          :disable-ai-prompt="true"
          data-cy="questionText"
          label="Question"
          label-class="text-primary font-bold"
          :resizable="true"
          markdownHeight="150px"
          name="question" />
      <div data-cy="answerHintSection" v-if="isQuizType">
        <div class="flex mb-2">
          <SkillsCheckboxInput
              v-model="showHint"
              :binary="true"
              inputId="answerHintEnable"
              name="answerHintEnable"
              data-cy=answerHintEnableCheckbox />
          <div class="flex-1 align-content-end">
            <label for="answerHintEnable" class="font-bold text-primary ml-2">Enable Answer Hint</label>
          </div>
        </div>
        <SkillsTextarea v-if="showHint"
            id="answerHintInput"
            placeholder="Hint to be presented to user"
            aria-label="Hint to be presented to user"
            rows="3"
            max-rows="3"
            name="answerHint"
            data-cy="answerHint"
            :submit-on-enter="false"
            :disabled="!showHint"
        />
      </div>

      <div class="mt-4 mb-2">
        <span class="font-bold text-primary">Answers</span>
      </div>
      <div class="mb-2">
        <question-type-drop-down
            name="questionType"
            data-cy="answerTypeSelector"
            v-model="questionType.selectedType"
            :options="questionType.options"
            @selection-changed="questionTypeChanged"
        />
      </div>

      <div v-if="isQuestionTypeTextInput" class="flex pl-4">
        <label for="textInputPlaceholder" hidden>Text Input Answer Placeholder:</label>
        <Textarea
            style="resize: none"
            class="flex-1"
            id="textInputPlaceholder"
            placeholder="Users will be required to enter text."
            data-cy="textAreaPlaceHolder"
            :disabled="true"
            rows="3"/>
      </div>

      <div v-if="isQuestionTypeRatingInput" class="flex flex-col">
        <SkillsDropDown
            label="Scale"
            name="currentScaleValue"
            data-cy="ratingScaleSelect"
            id="ratingScaleSelect"
            aria-label="Please select the rating's scale"
            :options="currentScaleOptions" />
      </div>

      <div v-if="!isQuestionTypeTextInput && !isQuestionTypeRatingInput" class="pl-4">
        <div class="mb-2" v-if="isQuizType">
          <span
              v-if="isQuestionTypeMultipleChoice"
              class="text-secondary">Check two or more correct answers on the left:</span>
          <span
              v-if="isQuestionTypeSingleChoice"
              class="text-secondary">Check one correct answer on the left:</span>
          <span
            v-if="isQuestionTypeMatching" class="text-secondary">Add pairs of terms and their matching values:</span>
        </div>
        <ConfigureAnswers
            v-if="!isQuestionTypeMatching && props.questionDef.quizType"
            ref="answersRef"
            v-model="props.questionDef.answers"
            :quiz-type="props.questionDef.quizType"
            :question-type="questionType.selectedType.id "
            :class="{ 'p-invalid': answersErrorMessage }"
            :aria-invalid="!!answersErrorMessage"
            aria-errormessage="answersError"
              aria-describedby="answersError" />

        <matching-question ref="answersRef" v-model="props.questionDef.answers" v-if="isQuestionTypeMatching" />

        <Message severity="error"
                 variant="simple"
                 size="small"
                 :closable="false"
                 data-cy="answersError"
                 id="answersError">{{ answersErrorMessage || '' }}</Message>
      </div>
    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>