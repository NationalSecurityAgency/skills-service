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
import QuizStatus from "@/components/quiz/runsHistory/QuizStatus.js";
import QuizType from "@/skills-display/components/quiz/QuizType.js";

const props = defineProps({
  quizType: String,
  status: String,
})
</script>

<template>
  <div>
    <div v-if="QuizStatus.isFailed(status)" class="text-red-700 dark:text-red-400">
      <i class="far fa-times-circle" aria-hidden="true"></i> Failed
    </div>
    <div v-if="QuizStatus.isPassed(status)" class="text-green-700 dark:text-green-400">
      <i class="fas fa-check-double" aria-hidden="true"></i> <span v-if="QuizType.isSurvey(quizType)">Completed</span><span v-else>Passed</span>
    </div>
    <div v-if="QuizStatus.isInProgress(status)" class="text-orange-700 dark:text-orange-400">
      <i class="fas fa-running" aria-hidden="true"></i> In Progress
    </div>
    <div v-if="QuizStatus.isNeedsGrading(status)" class="text-orange-700 dark:text-orange-400">
      <i class="fas fa-user-check" aria-hidden="true"></i> Needs Grading
    </div>
  </div>
</template>

<style scoped>

</style>