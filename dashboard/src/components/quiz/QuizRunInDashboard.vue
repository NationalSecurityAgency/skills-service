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
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import QuizRunService from '@/skills-display/components/quiz/QuizRunService.js';
import QuizRun from '@/skills-display/components/quiz/QuizRun.vue';

const router = useRouter()
const route = useRoute()
const quizInfo = ref({});
const loadingQuizInfo = ref(true);

const quizId = computed(() => {
  return route.params.quizId
})
const skillId = computed(() => {
  return route.params.skillId
})
const projectId = computed(() => {
  return route.params.projectId
})

onMounted(() => {
  loadQuizInfo();
})

const loadQuizInfo = () => {
  loadingQuizInfo.value = true;
  QuizRunService.getQuizInfo(quizId.value, skillId.value, projectId.value)
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
      :skillId="skillId"
      :projectId="projectId"
      :multipleTakes="quizInfo.multipleTakes"
      class="mb-5"
      @testWasTaken="navToProgressAndRanking"
      @cancelled="navToProgressAndRanking"/>
    </div>
  </div>
</template>

<style scoped>

</style>