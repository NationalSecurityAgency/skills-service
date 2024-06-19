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
import { onMounted, computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import dayjs from '@/common-components/DayJsCustomizer';
import ProjectService from '@/components/projects/ProjectService';
import QuizService from '@/components/quiz/QuizService';

const route = useRoute()
const appConfig = useAppConfig()

const show = ref(false)
const configuredStartDate = ref(null);

const actionsTrackingStartDate = computed(() => {
  return configuredStartDate.value.format('ll');
})

onMounted(() => {
  const startDate = appConfig.activityHistoryStartDate;
  if (startDate) {
    configuredStartDate.value = dayjs(startDate);
    if (route.params.projectId) {
      ProjectService.getProject(route.params.projectId)
          .then((project) => {
            const projCreated = dayjs(project.created);
            show.value = projCreated.isBefore(configuredStartDate.value);
          });
    } else if (route.params.quizId) {
      QuizService.getQuizDef(route.params.quizId)
          .then((quiz) => {
            const quizCreated = dayjs(quiz.created);
            show.value = quizCreated.isBefore(configuredStartDate.value);
          });
    }
  }
})
</script>

<template>
  <div v-if="show" class="ml-3 text-info" data-cy="activityHistoryStartRecordingWarning">
    <i class="fas fa-exclamation-circle" aria-hidden="true"/> Started recording user activity on <b>{{ actionsTrackingStartDate }}</b>
  </div>
</template>

<style scoped>

</style>