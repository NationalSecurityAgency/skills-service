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

import {onMounted, ref} from 'vue';
import {useRoute} from 'vue-router'
import QuizService from '@/components/quiz/QuizService.js';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import QuizSingleRun from "@/components/quiz/runsHistory/QuizSingleRun.vue";


const loading = ref(true);
const runInfo = ref({});
const route = useRoute();
const quizId = ref(route.params.quizId);
const attemptId = ref(route.params.runId);



const loadData = () => {
  loading.value = true;
  QuizService.getSingleQuizHistoryRun(quizId.value, attemptId.value)
      .then((res) => {
        runInfo.value = res;
      }).finally(() => {
    loading.value = false;
  });
}

onMounted(() => {
  loadData();
})
</script>

<template>

  <div>
    <SubPageHeader title="User Run">
      <router-link :aria-label="`Return back to all the ${runInfo.quizType} results`"
                   :to="{ name: 'QuizRunsHistoryPage' }" tabindex="-1">
        <SkillsButton icon="fas fa-arrow-alt-circle-left"
                      outlined
                      size="small"
                      data-cy="quizRunBackBtn"
                      id="btn_Questions"
                      label="Back">
        </SkillsButton>
      </router-link>
    </SubPageHeader>

    <Card>
      <template #content>
        <SkillsSpinner v-if="loading" :is-loading="true" class="my-20"/>
        <div v-else>
          <quiz-single-run :run-info="runInfo" :show-quiz-under-review="false" :show-ai-grading-meta="true"/>
        </div>
      </template>
    </Card>

  </div>
</template>

<style scoped>

</style>