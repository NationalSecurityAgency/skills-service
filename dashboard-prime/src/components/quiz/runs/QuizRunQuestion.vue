<script setup>
import { computed, watch, onMounted, ref } from 'vue'
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
  if (isRating.value) {
    const selectedAnswer = answerOptions.value.find((a) => a.selected);
    if (selectedAnswer) {
      answerRating.value = Number(selectedAnswer.answerOption);
    }
  }
  isLoading.value = false;
})

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
                :name="`questions[${num-1}].answerText`"
                :aria-label="`Please enter text to answer question number ${num}`"
                placeholder="Please enter your response here..."
                rows="10" />
        </div>
        <div v-else-if="isRating">
          <SkillsRating @update:modelValue="ratingChanged" class="flex-initial border-round py-3 px-4" v-model="answerRating" :stars="numberOfStars" :cancel="false" :name="`questions[${num-1}].answerRating`"/>
        </div>
        <div v-else>
          <div v-if="isMultipleChoice" class="text-secondary font-italic small" data-cy="multipleChoiceMsg">(Select <b>all</b> that apply)</div>
          <QuizRunAnswers class="mt-1 pl-1"
                          :name="`questions[${num-1}].quizAnswers`"
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