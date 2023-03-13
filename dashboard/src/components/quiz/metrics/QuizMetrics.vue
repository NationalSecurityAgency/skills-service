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
  <div>
    <sub-page-header title="Metrics" aria-label="metrics"/>

    <skills-spinner :is-loading="isLoading" />

    <div v-if="!isLoading">
      <div class="row">
        <div class="col">
          <stats-card title="Total # Attempts" :stat-num="metrics.numTaken" icon="fas fa-pen-square text-info"/>
        </div>
        <div class="col">
          <stats-card title="# Passed" :stat-num="metrics.numPassed" icon="fas fa-trophy text-success"/>
        </div>
        <div class="col">
          <stats-card title="# Failed" :stat-num="metrics.numFailed" icon="far fa-sad-tear text-warning"/>
        </div>
      </div>

      <b-card class="mt-3" body-class="p-0">
        <div v-for="(q, index) in metrics.questions" :key="q.id" class="mb-5">
          <quiz-question-metrics :q="q" :num="index" />
        </div>
      </b-card>
    </div>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import QuizService from '@/components/quiz/QuizService';
  import StatsCard from '@/components/metrics/utils/StatsCard';
  import QuizQuestionMetrics from '@/components/quiz/metrics/QuizQuestionMetrics';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  export default {
    name: 'QuizMetrics',
    components: {
      SkillsSpinner,
      QuizQuestionMetrics,
      StatsCard,
      SubPageHeader,
    },
    data() {
      return {
        isLoading: false,
        quizId: this.$route.params.quizId,
        metrics: null,
      };
    },
    mounted() {
      this.isLoading = true;
      QuizService.getQuizMetrics(this.quizId)
        .then((res) => {
          this.metrics = res;
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
  };
</script>

<style scoped>

</style>
