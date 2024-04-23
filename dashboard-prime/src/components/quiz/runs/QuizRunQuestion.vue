<script setup>
import { computed, watch, onMounted, ref } from 'vue'
import { object, string, number, array } from 'yup';
import { useField, useForm } from "vee-validate";
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue';
import SkillsRating from "@/components/utils/inputForm/SkillsRating.vue";
import QuizRunAnswers from '@/components/quiz/runs/QuizRunAnswers.vue';
import QuestionType from '@/common-components/quiz/QuestionType.js';
import QuizRunService from '@/common-components/quiz/QuizRunService.js';

const props = defineProps({
  q: Object,
  quizId: String,
  quizAttemptId: Number,
  num: Number,
})

const isLoading = ref(true);
const emit = defineEmits(['answer-text-changed', 'selected-answer'])

const answerOptions = ref([])
const answerRating = ref(0)
// const { answerRating, answerRatingErrorMessage } = useField(() => 'answerRating', undefined, {syncVModel: true});
// console.log(`answerRating [${answerRating}], answerRatingErrorMessage [${answerRatingErrorMessage}]`)
const answerText = ref(props.q.questionType === QuestionType.TextInput ? (props.q.answerOptions[0]?.answerText || '') : '')

const isMultipleChoice = computed(() => {
  return props.q.questionType === QuestionType.MultipleChoice;
})
const isSingleChoice = computed(() => {
  return props.q.questionType === QuestionType.SingleChoice;
})
const isTextInput = computed(() => {
  return props.q.questionType === QuestionType.TextInput;
})
const isRating = computed(() => {
  return props.q.questionType === QuestionType.Rating;
})
const isMissingAnswer = computed(() => {
  if (isTextInput.value) {
    return !answerText.value || answerText.value.trimEnd() === '';
  }
  return answerOptions.value.findIndex((a) => a.selected === true) < 0;
})
const questionNumAriaLabel = computed(() => {
  let res = `Question number ${props.num}`;
  if (props.q.gradedInfo) {
    if (props.q.gradedInfo.isCorrect) {
      res = `${res} was answered correctly`;
    } else {
      res = `${res} was answered incorrectly`;
    }
  }
  return res;
})
const numberOfStars = computed(() => {
  return props.q?.answerOptions?.length;
})

watch(answerText, (newValue, oldValue) => {
  console.log(`answerText changed from ${oldValue} to ${newValue}`)
  textAnswerChanged();
})
watch(() => answerRating.value, (newValue, oldValue) => {
  console.log(`answerRating changed from ${oldValue} to ${newValue}`)
  // ratingChanged(newValue)
})

onMounted(() => {
  answerOptions.value = props.q.answerOptions.map((a) => ({ ...a, selected: a.selected ? a.selected : false }));
  setFieldValue('quizAnswers', answerOptions.value)
  if (isRating.value) {
    const selectedAnswer = answerOptions.value.find((a) => a.selected);
    if (selectedAnswer) {
      console.log(`setting selectedAnswer.answerOption [${selectedAnswer.answerOption}]`)
      answerRating.value = Number(selectedAnswer.answerOption);
      console.log('calling setField')
      setFieldValue('answerRating', Number(selectedAnswer.answerOption))
      console.log('done')
    }
  }
  isLoading.value = false;
  setupValidation();
})


const setupValidation = () => { 
  // extend('atLeastOneSelected', {
  //   message: () => 'At least 1 choice must be selected',
  //   validate(value) {
  //     const foundSelected = value && (value.findIndex((a) => a.selected) >= 0);
  //     return foundSelected;
  //   },
  // }, {
  //   immediate: false,
  // });
  //
  // extend('ratingSelected', {
  //   message: () => 'A rating must be selected',
  //   validate(value) {
  //     if (value > 0) {
  //       return true;
  //     }
  //     return false;
  //   },
  // }, {
  //   immediate: false,
  // });
}
const textAnswerChanged = () => { 
  const selectedAnswerIds = answerOptions.value.map((a) => a.id);
  const isAnswerBlank = !answerText.value || answerText.value.trimEnd() === '';
  const currentAnswer = {
    questionId: props.q.id,
    questionType: props.q.questionType,
    selectedAnswerIds,
    changedAnswerId: answerOptions.value[0].id,
    changedAnswerIdSelected: !isAnswerBlank,
    answerText: answerText.value,
  };
  reportAnswer(currentAnswer).then((reportAnswerPromise) => {
    // only 1 answer in case of TextInput
    emit('answer-text-changed', {
      ...currentAnswer,
      reportAnswerPromise,
    });
  });
}
const selectionChanged = (currentAnswer) => { 
  reportAnswer(currentAnswer).then((reportAnswerPromise) => {
    emit('selected-answer', {
      ...currentAnswer,
      reportAnswerPromise,
    });
  });
}
const ratingChanged = (value) => {
  const selectedAnswerIds = answerOptions.value.map((a) => a.id);
  const answerId = selectedAnswerIds[value - 1];
  const currentAnswer = {
    questionId: props.q.id,
    questionType: props.q.questionType,
    selectedAnswerIds: [answerId],
    changedAnswerId: answerId,
    changedAnswerIdSelected: true,
  };
  reportAnswer(currentAnswer).then((reportAnswerPromise) => {
    emit('selected-answer', {
      ...currentAnswer,
      reportAnswerPromise,
    });
  });
}
const reportAnswer = (answer) => {
  if (!isLoading.value) {
    return QuizRunService.reportAnswer(props.quizId, props.quizAttemptId, answer.changedAnswerId, answer.changedAnswerIdSelected, answer.answerText);
  }
  return new Promise((resolve) => {
    resolve(null);
  });
}

const schema = object({
  'answerText': string()
      .trim()
      .required()
      .customDescriptionValidator(`Answer to question #${props.num}`, false)
      .label(`Answer to question #${props.num}`),
  'answerRating': number()
      .nullable()
      .min(1)
      .max(numberOfStars.value)
      .test('ratingSelected', 'A rating must be selected', (value) => ratingSelected(value))
      .label('Rating'),
  'quizAnswers': array().required()
      .test('atLeastOneSelected', 'At least 1 choice must be selected', (value) => atLeastOneSelected(value))
      .label('Answers'),
})
const atLeastOneSelected = (value) => {
  const found = value && (value.findIndex((a) => a.selected) >= 0)
  console.log(`checking values, found: ${found}`)
  return found;
}
const ratingSelected = (value) => {
  const selected = value > 0;
  console.log(`checking values, selected: ${selected}`)
  return selected;
}
const { values, meta, handleSubmit, isSubmitting, setFieldValue, validate, errors } = useForm({
  validationSchema: schema,
  initialValues: {
    answerText: answerText.value,
    answerRating: answerRating.value,
    quizAnswers: answerOptions.value,
  }
})
</script>

<template>
  <div class="flex gap-0 mb-4" :data-cy="`question_${num}`">
    <div class="flex align-items-start pt-2 pr-2">
      <Tag class="d-inline-block"
               :aria-label="questionNumAriaLabel"
               :severity="`${q.gradedInfo ? (q.gradedInfo.isCorrect ? 'success' : 'danger') : 'default'}`">
        {{ num }}
      </Tag>
      <span v-if="q.gradedInfo" class="ml-1 pt-1">
        <span v-if="q.gradedInfo.isCorrect" class="text-success skills-theme-quiz-correct-answer" style="font-size: 1.1rem;" data-cy="questionAnsweredCorrectly"><i class="fas fa-check-double" aria-hidden="true"></i></span>
        <span v-if="!q.gradedInfo.isCorrect" class="text-danger skills-theme-quiz-incorrect-answer" style="font-size: 1.1rem;" data-cy="questionAnsweredWrong"><i class="fas fa-times-circle" aria-hidden="true"></i></span>
      </span>
    </div>
    <div class="flex flex-1">
      <div class="flex flex-column w-full">
        <markdown-text :text="q.question" data-cy="questionsText" :instance-id="`${q.id}`" />
        <div v-if="isTextInput">
            <SkillsTextarea
                :id="`question-${num}`"
                data-cy="textInputAnswer"
                v-model="answerText"
                name="answerText"
                :aria-label="`Please enter text to answer question number ${num}`"
                placeholder="Please enter your response here..."
                rows="10" />
        </div>
        <div v-else-if="isRating">
          <SkillsRating @update:modelValue="ratingChanged" class="flex-initial border-round py-3 px-4" v-model="answerRating" :stars="numberOfStars" :cancel="false" name="answerRating"/>
        </div>
        <div v-else>
          <div v-if="isMultipleChoice" class="text-secondary font-italic small" data-cy="multipleChoiceMsg">(Select <b>all</b> that apply)</div>
          <QuizRunAnswers class="mt-1 pl-1"
                          name="quizAnswers"
                          @selected-answer="selectionChanged"
                          :value="answerOptions"
                          :q="q"
                          :q-num="num"
                          :can-select-more-than-one="isMultipleChoice"/>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>