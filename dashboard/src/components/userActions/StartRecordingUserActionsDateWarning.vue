/*
Copyright 2020 SkillTree

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
<template>
  <div v-if="show" class="ml-3 text-info" data-cy="activityHistoryStartRecordingWarning">
    <i class="fas fa-exclamation-circle" aria-hidden="true"/> Started recording user activity on <b>{{ actionsTrackingStartDate }}</b>
  </div>
</template>

<script>
  import dayjs from '@/common-components/DayJsCustomizer';
  import ProjectService from '@/components/projects/ProjectService';
  import QuizService from '@/components/quiz/QuizService';

  export default {
    name: 'StartRecordingUserActionsDateWarning',
    data() {
      return {
        show: false,
        configuredStartDate: null,
      };
    },
    mounted() {
      const startDate = this.$store.getters.config.activityHistoryStartDate;
      if (startDate) {
        this.configuredStartDate = dayjs(startDate);
        if (this.$route.params.projectId) {
          ProjectService.getProject(this.$route.params.projectId)
            .then((project) => {
              const projCreated = dayjs(project.created);
              this.show = projCreated.isBefore(this.configuredStartDate);
            });
        } else if (this.$route.params.quizId) {
          QuizService.getQuizDef(this.$route.params.quizId)
            .then((quiz) => {
              const quizCreated = dayjs(quiz.created);
              this.show = quizCreated.isBefore(this.configuredStartDate);
            });
        }
      }
    },
    computed: {
      actionsTrackingStartDate() {
        return this.configuredStartDate.format('ll');
      },
    },
  };
</script>

<style scoped>

</style>
