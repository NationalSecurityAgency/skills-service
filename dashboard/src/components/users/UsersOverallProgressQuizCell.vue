/*
Copyright 2026 SkillTree

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
const props = defineProps({
  userProgress: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="flex-1 flex pr-2 flex-wrap items-center">
    <div class="flex-1">
      <Tag severity="success" data-cy="quizAttempts">{{ userProgress.numQuizAttempts }}</Tag>
    </div>
    <div v-if="userProgress.numQuizAttempts > 0">
      <div class="flex gap-3">
        <div class="flex-1">Passed:</div>
        <div data-cy="quizAttemptsPassed">{{ userProgress.numQuizzesPassed }}</div>
      </div>
      <div class="flex gap-2">
        <div class="flex-1">Failed:</div>
        <div data-cy="quizAttemptsFailed">{{ userProgress.numQuizzesFailed }}</div>
      </div>
      <div class="flex gap-1" v-if="userProgress.numQuizzesInProgress > 0">
        <div class="flex-2">In-Progress:</div>
        <div data-cy="quizAttemptsInProgress">{{ userProgress.numQuizzesInProgress }}</div>
      </div>
      <div class="flex gap-1" v-if="userProgress.numQuizzesNeedsGrading > 0">
        <div class="flex-2">Needs Grading:</div>
        <div data-cy="quizAttemptsNeedsGrading">{{ userProgress.numQuizzesNeedsGrading }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>