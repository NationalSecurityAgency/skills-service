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
import MyProgressInfoCardUtil from '@/components/myProgress/MyProgressInfoCardUtil.vue'
import {useMyProgressState} from '@/stores/UseMyProgressState.js'
import {computed} from 'vue'
import ProgressCardIcon from "@/components/myProgress/ProgressCardIcon.vue";

const myProgressState = useMyProgressState()
const myProgress = computed(() => myProgressState.myProgress)

const totalRuns = computed(() => myProgress.value.numQuizAttempts + myProgress.value.numSurveyAttempts)

</script>

<template>
  <my-progress-info-card-util title="Quizzes and Surveys">
    <template #left-content>
      <div>
        <div class="text-4xl text-orange-700 dark:text-orange-400" data-cy="numQuizAndSurveyRuns">{{ totalRuns }}</div>
        <div class="flex">
          <span class="mr-1 w-20">Quizzes:</span> <Tag severity="info" data-cy="numQuizzes">{{ myProgress.numQuizAttempts }}</Tag>
        </div>
        <div class="flex my-1">
          <span class="mr-1 w-20">Surveys:</span> <Tag severity="success" data-cy="numSurveys">{{ myProgress.numSurveyAttempts }}</Tag>
        </div>
      </div>
    </template>
    <template #right-content>
      <div class="flex justify-center sm:justify-end">
        <div class="flex justify-center">
          <progress-card-icon icon="fas fa-spell-check" />
        </div>
      </div>
    </template>
    <template #footer>
      <div class="flex gap-2 items-center flex-col sm:flex-row">
        <div data-cy="quizzes-card-footer" class="flex-1 w-min-10rem">
          Explore quiz and survey history!
        </div>
        <div>
          <router-link :to="{ name: 'MyQuizAttemptsPage' }">
            <SkillsButton
                label="History"
                icon="fas fa-spell-check"
                outlined
                size="small"
                aria-label="View my quiz and survey hisotyr"
                data-cy="viewQuizzesAttemptsBtn" />
          </router-link>
        </div>
      </div>
    </template>
  </my-progress-info-card-util>
</template>

<style scoped>

</style>