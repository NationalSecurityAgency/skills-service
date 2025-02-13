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
import { computed, nextTick, onMounted, ref } from 'vue'
import { array, boolean, object, string } from 'yup'
import { useRoute } from 'vue-router';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import QuizService from '@/components/quiz/QuizService.js';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import ConfigureAnswers from '@/components/quiz/testCreation/ConfigureAnswers.vue';
import QuizType from "@/skills-display/components/quiz/QuizType.js";

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
});

function questionTypeChanged(inputItem) {
  questionType.value.selectedType = inputItem;
  if (isSurveyType.value
      && inputItem.id !== QuestionType.TextInput && inputItem.id !== QuestionType.Rating
      && (!initialQuestionData.answers || initialQuestionData.answers.length < 2)) {
    nextTick(() => {
      answersRef.value.replaceAnswers([{
        id: null,
        answer: '',
        isCorrect: false,
      }, {
        id: null,
        answer: '',
        isCorrect: false,
      }]);
    })
  }

}

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
  if (isSurveyType.value || !isDirty.value || isQuestionTypeTextInput.value || isQuestionTypeRatingInput.value) {
    return true;
  }
  const numCorrect = value.filter((a) => a.isCorrect).length;
  return numCorrect >= 1;
}
const atLeastTwoAnswersFilledIn = (value) => {
  if (!isDirty.value || isQuestionTypeTextInput.value || isQuestionTypeRatingInput.value) {
    return true;
  }
  const numWithContent = value.filter((a) => (a.answer && a.answer.trim().length > 0)).length;
  return numWithContent >= 2;
}
const correctAnswersMustHaveText = (value) => {
  if (isSurveyType.value || !isDirty.value || isQuestionTypeTextInput.value || isQuestionTypeRatingInput.value) {
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
  if (isSurveyType.value || !isDirty.value || !QuestionType.isSingleChoice(questionType.value.selectedType.id)) {
    return true;
  }
  const numCorrect = value.filter((a) => (a.isCorrect)).length;
  return numCorrect === 1;
}
const multipleChoiceQuestionsMustHaveAtLeast2Answer = (value) => {
  if (isSurveyType.value || !isDirty.value || !QuestionType.isMultipleChoice(questionType.value.selectedType.id)) {
    return true;
  }
  const numCorrect = value.filter((a) => (a.isCorrect)).length;
  return numCorrect >= 2;
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
  'answers': array()
      .of(
          object({
            'answer': string().nullable().label('Answer'),
            'isCorrect': boolean().label('Is Correct')
          })
      )
      .test('atLeastOneCorrectAnswer', 'Must have at least 1 correct answer selected', (value) => atLeastOneCorrectAnswer(value))
      .test('atLeastTwoAnswersFilledIn', 'Must have at least 2 answers', (value) => atLeastTwoAnswersFilledIn(value))
      .test('correctAnswersMustHaveText', 'Answers labeled as correct must have text', (value) => correctAnswersMustHaveText(value))
      .test('maxNumAnswers', `Exceeded maximum number of [${appConfig.maxAnswersPerQuizQuestion}] answers`, (value) => maxNumAnswers(value))
      .test('singleChoiceQuestionsMustHave1Answer', 'Multiple Choice Question must have 1 correct answer', (value) => singleChoiceQuestionsMustHave1Answer(value))
      .test('multipleChoiceQuestionsMustHaveAtLeast2Answer', 'Multiple Answers Question must have at least 2 correct answers', (value) => multipleChoiceQuestionsMustHaveAtLeast2Answer(value))
  ,
})
const initialQuestionData = {
  questionType: props.isEdit || props.isCopy ? questionType.value.options.find((o) => o.id === props.questionDef.questionType) : questionType.value.selectedType,
  question: props.questionDef.question || '',
  answers: props.questionDef.answers || [],
  currentScaleValue: props.questionDef.questionType === QuestionType.Rating && props.isEdit || props.isCopy ? props.questionDef.answers.length : 5,
}

const close = () => { model.value = false }

const saveQuiz = (values) => {
  const { question, answers, currentScaleValue } = values
  const removeEmptyQuestions = answers.filter((a) => (a.answer && a.answer.trim().length > 0));
  const numCorrect = answers.filter((a) => a.isCorrect).length;
  let { questionType : { id : questionType } } = values
  const quizToSave = {
    id: props.questionDef.id,
    quizId: quizId.value,
    question,
    questionType,
    answers: (questionType === QuestionType.TextInput || questionType === QuestionType.Rating) ? [] : removeEmptyQuestions,
  };
  if (questionType === QuestionType.Rating) {
    quizToSave.questionScale = currentScaleValue;
  }

  if (props.isEdit) {
    return QuizService.updateQuizQuestionDef(quizId.value, quizToSave)
        .then((updatedQuizQuestionDef) => {
          return {
            ...updatedQuizQuestionDef,
            isEdit: props.isEdit,
          }
        });
  } else {
    return QuizService.saveQuizQuestionDef(quizId.value, quizToSave)
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

</script>

<template>
  <SkillsInputFormDialog
      :id="modalId"
      v-model="model"
      :is-edit="isEdit"
      :is-copy="isCopy"
      :header="modalTitle"
      :loading="loadingComponent"
      :validation-schema="schema"
      :initial-values="initialQuestionData"
      :save-data-function="saveQuiz"
      @saved="onSavedQuestion"
      @close="close"
      @isDirty="isDirty = !isDirty"
      @errors="answersErrorMessage = $event['answers']"
  >
    <template #default>
      <markdown-editor
          id="quizDescription"
          :quiz-id="quizId"
          data-cy="questionText"
          label="Question"
          label-class="text-primary font-bold"
          :resizable="true"
          markdownHeight="150px"
          name="question" />

      <div class="mt-4 mb-2">
        <span class="font-bold text-primary">Answers</span>
      </div>
      <div class="mb-2">
        <SkillsDropDown
            name="questionType"
            data-cy="answerTypeSelector"
            v-model="questionType.selectedType"
            aria-label="Selection Question Type"
            @update:modelValue="questionTypeChanged"
            :isRequired="true"
            :options="questionType.options">
          <template #value="slotProps">
            <div v-if="slotProps.value" class="p-1" :data-cy="`selectionItem_${slotProps.value.id}`" :aria-label="`Select ${slotProps.value.label}`">
              <i :class="slotProps.value.icon" style="min-width: 1.2rem" class="border rounded p-1 mr-2" aria-hidden="true"></i>
              <span class="">{{ slotProps.value.label }}</span>
            </div>
          </template>

          <template #option="slotProps">
            <div class="p-1" :data-cy="`selectionItem_${slotProps.option.id}`">
              <i :class="slotProps.option.icon" style="min-width: 1.2rem" class="border rounded p-1 mr-2" aria-hidden="true"></i>
              <span class="">{{ slotProps.option.label }}</span><span class="hidden sm:inline">: {{ slotProps.option.description }}</span>
            </div>
          </template>
        </SkillsDropDown>
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
        </div>
        <ConfigureAnswers
            ref="answersRef"
            v-model="props.questionDef.answers"
            :quiz-type="props.questionDef.quizType"
            :question-type="questionType.selectedType.id "
            :class="{ 'p-invalid': answersErrorMessage }"
            :aria-invalid="!!answersErrorMessage"
            aria-errormessage="answersError"
              aria-describedby="answersError" />
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