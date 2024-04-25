<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import QuizRunService from '@/common-components/quiz/QuizRunService.js';
import QuizRun from '@/components/quiz/runs/QuizRun.vue';

const router = useRouter()
const route = useRoute()
const quizInfo = ref({});
const loadingQuizInfo = ref(true);

const quizId = computed(() => {
  return route.params.quizId
})

onMounted(() => {
  loadQuizInfo();
})

const loadQuizInfo = () => {
  loadingQuizInfo.value = true;
  QuizRunService.getQuizInfo(quizId.value)
      .then((res) => {
        quizInfo.value = res;
      })
      .finally(() => {
        loadingQuizInfo.value = false;
      });
}

const navToProgressAndRanking = () => {
  router.push({ name: 'MyProgressPage' });
}
</script>

<template>
  <div>
    <SkillsSpinner :is-loading="loadingQuizInfo"/>
    <div v-if="!loadingQuizInfo">
      <SubPageHeader :title="quizInfo.quizType" class="pt-4 pl-3">
      </SubPageHeader>

      <QuizRun v-if="quizId"
      :quiz-id="quizId"
      :quiz="quizInfo"
      class="mb-5"
      @testWasTaken="navToProgressAndRanking"
      @cancelled="navToProgressAndRanking"/>
    </div>
  </div>
</template>

<style scoped>

</style>