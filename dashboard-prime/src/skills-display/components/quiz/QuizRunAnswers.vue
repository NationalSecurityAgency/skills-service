<script setup>
import { watch, ref } from 'vue'
import { useField } from 'vee-validate';
import QuizRunAnswer from '@/skills-display/components/quiz/QuizRunAnswer.vue';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';

const props = defineProps({
  value: Array,
  q: Object,
  qNum: Number,
  canSelectMoreThanOne: Boolean,
  name: {
    type: String,
    required: true
  },
})
const emit = defineEmits(['input', 'selected-answer'])
const answerOptionsInternal = ref(props.value.map((a) => ({ ...a })))

watch(() => props.value, (newValue, oldValue) => {
  answerOptionsInternal.value = newValue ? newValue.map((a) => ({ ...a })) : [];
  if (oldValue.length) {
    // do not update value, the prop was updated by vee-validate and value is already set (otherwise it will trigger validation)
    value.value = answerOptionsInternal.value;
  }
})
watch(() => props.q.gradedInfo, (newValue, oldValue) => {
  answerOptionsInternal.value = answerOptionsInternal.value.map((answer) => ({ ...answer, isGraded: true, isCorrect: newValue.correctAnswerIds.indexOf(answer.id) >= 0 }));
  value.value = answerOptionsInternal.value;
})

const selectionChanged = (selectedStatus) => {
  answerOptionsInternal.value = value.value.map((a) => {
    const isThisId = a.id === selectedStatus.id;
    const isSelected = isThisId && selectedStatus.selected;
    const selectRes = isSelected || (props.q.questionType === QuestionType.MultipleChoice && a.selected && !isThisId);
    return {
      ...a,
      selected: selectRes,
    };
  });
  value.value = answerOptionsInternal.value
  const selectedAnswerIds = answerOptionsInternal.value.filter((a) => a.selected).map((a) => a.id);
  const currentAnswer = {
    questionId: props.q.id,
    questionType: props.q.questionType,
    selectedAnswerIds,
    changedAnswerId: selectedStatus.id,
    changedAnswerIdSelected: selectedStatus.selected,
  };
  emit('input', answerOptionsInternal.value);
  emit('selected-answer', currentAnswer);
}
const { value, errorMessage } = useField(() => props.name, undefined, {syncVModel: true});

</script>

<template>
  <div>
    <div v-for="(a, aIndex) in answerOptionsInternal" :key="a.id">
      <QuizRunAnswer
          :data-cy="`answer_${aIndex+1}`"
          :a="a"
          :answer-num="aIndex+1"
          :q-num="qNum"
          :can-select-more-than-one="canSelectMoreThanOne"
          @selection-changed="selectionChanged"/>
    </div>
    <small v-if="errorMessage"
           role="alert"
           class="p-error"
           :data-cy="`${name}Error`"
           :id="`${name}Error`">{{ errorMessage || '&nbsp;' }}</small>
  </div>
</template>

<style scoped>

</style>