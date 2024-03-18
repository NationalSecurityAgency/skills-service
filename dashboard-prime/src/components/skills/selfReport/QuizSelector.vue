<script setup>
import { onMounted, computed, ref, inject } from 'vue'
import QuizService from '@/components/quiz/QuizService.js';
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';

const props = defineProps({
  initiallySelectedQuizId: {
    type: String,
    default: null,
  },
})
const emit = defineEmits(['changed'])
const setFieldValue = inject('setFieldValue')

const isLoading = ref(true)
const selectedInternal = ref(null)
const availableQuizzes = ref([])

const noQuizzes = computed(() => {
  return availableQuizzes.value && availableQuizzes.value.length === 0;
})
onMounted(() => {
  loadData()
})
const loadData = () => {
  isLoading.value = true
  QuizService.getQuizDefs()
      .then((res) => {
        availableQuizzes.value = res;
        if (props.initiallySelectedQuizId) {
          const found = availableQuizzes.value.find((q) => q.quizId === props.initiallySelectedQuizId);
          if (found) {
            selectedInternal.value = found;
            setFieldValue('associatedQuiz', found);
          }
        }
      }).finally(() => {
        isLoading.value = false
      });
}
const quizSelected = (quiz) => {
  emit('changed', quiz ? quiz.quizId : null);
}
</script>

<template>
  <div class="w-full">
    <SkillsDropDown
        name="associatedQuiz"
        data-cy="quizSelector"
        v-model="selectedInternal"
        showClear
        filter
        optionLabel="name"
        @update:modelValue="quizSelected"
        :emptyMessage="noQuizzes ? 'You currently do not administer any quizzes or surveys.' : 'No results. Please refine your search string.'"
        :isRequired="true"
        :options="availableQuizzes">
      <template #value="slotProps">
        <div v-if="slotProps.value" class="p-1" :data-cy="`quizSelected-${slotProps.value.quizId}`">
          <span class="text-secondary">{{ slotProps.value.type }}:</span><span class="ml-1">{{ slotProps.value.name }}</span>
        </div>
        <span v-else>
            Search available quizzes and surveys...
        </span>
      </template>
      <template #option="slotProps">
        <div :data-cy="`availableQuizSelection-${slotProps.option.quizId}`">
          <span class="text-secondary">{{ slotProps.option.type }}:</span><span class="h6 ml-2">{{ slotProps.option.name }}</span>
        </div>
      </template>
    </SkillsDropDown>
  </div>
</template>

<style scoped>

</style>