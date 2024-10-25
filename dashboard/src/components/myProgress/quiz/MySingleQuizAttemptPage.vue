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
import {onMounted, ref} from "vue";
import MyProgressTitle from "@/components/myProgress/MyProgressTitle.vue";
import QuizRunService from "@/skills-display/components/quiz/QuizRunService.js";
import {useRoute} from "vue-router";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import QuizSingleRun from "@/components/quiz/runsHistory/QuizSingleRun.vue";
import QuizStatus from "@/components/quiz/runsHistory/QuizStatus.js";

const route = useRoute()

const loadingAttempt = ref(true)
const attempt = ref({})

const loadQuizAttempt = () => {
  loadingAttempt.value = true
  QuizRunService.getSingleQuizAttempt(route.params.attemptId).then((res) => {
    attempt.value = res
  }).finally(() => {
    loadingAttempt.value = false
  })
}
onMounted(() => {
  loadQuizAttempt()
})
</script>

<template>
  <div>
    <SkillsSpinner v-if="loadingAttempt" :is-loading="true" class="my-8"/>
    <div v-else>
      <my-progress-title :title="`My ${attempt.quizType}`"/>
      <Card class="my-4">
        <template #content>
          <div class="text-2xl mb-3 font-medium">{{ attempt.quizName }}</div>
          <quiz-single-run :run-info="attempt"  :show-user-card="false" :show-question-card="!QuizStatus.isNeedsGrading(attempt.status)"/>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>

</style>