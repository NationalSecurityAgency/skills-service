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
      <b-card v-if="!hasMetrics">
        <no-content2 title="No Metrics Yet..." class="my-5"
                     :message="`Metrics will be prominently displayed once at least 1 ${metrics.quizType} is completed`"/>
      </b-card>
      <div v-if="hasMetrics">
        <div class="row">
          <div class="col-md-6 col-xl-3 mb-2">
            <stats-card title="Total" icon="fas fa-pen-square text-info" :stat-num="metrics.numTaken">
                <b-badge variant="info">{{ metrics.numTaken }}</b-badge> attempts by <b-badge variant="success">{{ metrics.numTakenDistinctUsers }}</b-badge> users
            </stats-card>
          </div>
          <div v-if="!isSurvey" class="col-md-6 col-xl-3 mb-2">
            <stats-card title="Passed" :stat-num="metrics.numPassed" icon="fas fa-trophy text-success">
              <b-badge variant="success">{{ metrics.numPassed }}</b-badge> attempts <span class="text-success text-uppercase">passed</span> by <b-badge variant="success">{{ metrics.numPassedDistinctUsers }}</b-badge> users
            </stats-card>
          </div>
          <div v-if="!isSurvey" class="col-md-6 col-xl-3 mb-2">
            <stats-card title="Failed" :stat-num="metrics.numFailed" icon="far fa-sad-tear text-warning">
              <b-badge variant="danger">{{ metrics.numFailed }}</b-badge> attempts <span class="text-danger text-uppercase">failed</span> by <b-badge variant="success">{{ metrics.numFailedDistinctUsers }}</b-badge> users
            </stats-card>
          </div>
          <div class="col-md-6 col-xl-3 mb-2">
            <stats-card title="Average Runtime" :stat-num="metrics.avgAttemptRuntimeInMs" icon="fas fa-user-clock skills-color-points">
              <template #card-value>
                <span class="h4 font-weight-bold">{{ metrics.avgAttemptRuntimeInMs | formatDuration }}</span>
              </template>
              Average runtime for <b-badge variant="success">{{ metrics.numTaken }}</b-badge> attempts
            </stats-card>
          </div>

        </div>

        <b-card class="mt-3" body-class="p-0">
          <div v-for="(q, index) in metrics.questions" :key="q.id" class="mb-5">
            <quiz-question-metrics :q="q" :num="index" :is-survey="isSurvey" />
          </div>
        </b-card>
      </div>
    </div>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import QuizService from '@/components/quiz/QuizService';
  import StatsCard from '@/components/metrics/utils/StatsCard';
  import QuizQuestionMetrics from '@/components/quiz/metrics/QuizQuestionMetrics';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import NoContent2 from '@/components/utils/NoContent2';

  export default {
    name: 'QuizMetrics',
    components: {
      NoContent2,
      SkillsSpinner,
      QuizQuestionMetrics,
      StatsCard,
      SubPageHeader,
    },
    data() {
      return {
        isLoading: true,
        quizId: this.$route.params.quizId,
        metrics: null,
      };
    },
    computed: {
      isSurvey() {
        return this.metrics.quizType === 'Survey';
      },
      hasMetrics() {
        return this.metrics && this.metrics.numTaken > 0;
      },
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
