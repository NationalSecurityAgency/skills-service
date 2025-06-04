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

import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router'
import QuizService from '@/components/quiz/QuizService.js';
import NoContent2 from '@/components/utils/NoContent2.vue';
import StatsCard from '@/components/metrics/utils/StatsCard.vue';
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import QuizQuestionMetrics from '@/components/quiz/metrics/QuizQuestionMetrics.vue';
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import dayjs from "dayjs";

const timeUtils = useTimeUtils();
const route = useRoute();
const colors = useColors()
const isLoading = ref(true);
const quizId = ref(route.params.quizId);
const metrics = ref(null);
const filterRange = ref([]);

const isSurvey = computed(() => metrics.value && metrics.value.quizType === 'Survey');
const hasMetrics = computed(() => metrics.value && metrics.value.numTaken > 0);

onMounted(() => {
  loadQuizMetrics()
})

const loadQuizMetrics = () => {
  isLoading.value = true;
  const dateRange = timeUtils.prepareDateRange(filterRange.value)

  QuizService.getQuizMetrics(quizId.value, { startDate: dateRange.startDate, endDate: dateRange.endDate })
      .then((res) => {
        metrics.value = res;
      })
      .finally(() => {
        isLoading.value = false;
      });
}

const applyDateFilter = () => {
  loadQuizMetrics()
};

const clearDateFilter = () => {
  filterRange.value = [];
  loadQuizMetrics()
};

const isFiltered = computed(() => {
  return filterRange.value.length > 0
})
</script>

<template>
  <div>
    <SubPageHeader title="Results"
                   aria-label="results">
      <template #underTitle>
        <div class="flex gap-2 items-center">
          Filter by Date(s):
          <SkillsCalendarInput selectionMode="range" name="filterRange" v-model="filterRange" :maxDate="new Date()" placeholder="Select a date range" />
          <SkillsButton label="Apply" @click="applyDateFilter" />
          <SkillsButton label="Clear" @click="clearDateFilter" />
        </div>
      </template>
    </SubPageHeader>

    <SkillsSpinner :is-loading="isLoading"/>

    <Card :pt="{ body: { class: 'p-0!' } }">
      <template #content>
        <NoContent2 v-if="!hasMetrics && !isLoading && !isFiltered"
                    title="No Results Yet..."
                    class="my-8 py-8"
                    :message="`Results will be available once at least 1 ${metrics.quizType} is completed`"
                    data-cy="noMetricsYet"/>
        <NoContent2 v-if="isFiltered && !hasMetrics && !isLoading"
                    title="No Results For This Period"
                    class="my-8 py-8"
                    :message="`There are no results available for this time period. Please clear the filter or try a new timeframe.`"
                    data-cy="noMetricsYet"/>
      </template>
    </Card>
    <div v-if="hasMetrics && !isLoading">
      <div class="flex gap-4 flex-col lg:flex-row flex-wrap mb-4">
        <div class="flex-1">
          <StatsCard class="w-full h-full w-min-14rem" title="Total" :icon="`fas fa-pen-square ${colors.getTextClass(0)}`" :stat-num="metrics.numTaken" data-cy="metricsCardTotal">
            <span v-if="!isSurvey"><Tag severity="info">{{ metrics.numTaken }}</Tag> attempt{{ metrics.numTaken!=1 ? 's' : '' }} by <Tag severity="success">{{ metrics.numTakenDistinctUsers }}</Tag> user{{ metrics.numTakenDistinctUsers !=1 ? 's' : '' }}</span>
            <span v-if="isSurvey">Survey was completed <Tag severity="info">{{ metrics.numTaken }}</Tag> time{{ metrics.numTaken!=1 ? 's' : '' }}</span>
          </StatsCard>
        </div>
        <div v-if="!isSurvey" class="flex-1" data-cy="metricsCardPassed">
          <StatsCard class="w-full h-full w-min-14rem" title="Passed" :stat-num="metrics.numPassed" :icon="`fas fa-trophy ${colors.getTextClass(1)}`">
            <Tag severity="success">{{ metrics.numPassed }}</Tag>
            attempt{{ metrics.numPassed != 1 ? 's' : '' }} <span
              class="text-success uppercase">passed</span>
            by
            <Tag severity="success">{{ metrics.numPassedDistinctUsers }}</Tag>
            user{{ metrics.numPassedDistinctUsers != 1 ? 's' : '' }}
          </StatsCard>
        </div>
        <div v-if="!isSurvey" class="flex-1" data-cy="metricsCardFailed">
          <StatsCard class="w-full h-full w-min-14rem" title="Failed" :stat-num="metrics.numFailed" :icon="`far fa-sad-tear ${colors.getTextClass(2)}`">
            <Tag severity="danger">{{ metrics.numFailed }}</Tag>
            attempt{{ metrics.numFailed != 1 ? 's' : '' }} <span class="text-danger uppercase">failed</span> by
            <Tag severity="success">{{ metrics.numFailedDistinctUsers }}</Tag>
            user{{ metrics.numFailedDistinctUsers != 1 ? 's' : '' }}
          </StatsCard>
        </div>
        <div class="flex-1">
          <StatsCard class="w-full h-full w-min-14rem" title="Average Runtime" :stat-num="metrics.avgAttemptRuntimeInMs"
                     :icon="`fas fa-user-clock ${colors.getTextClass(3)}`"
                     data-cy="metricsCardRuntime">
            <template #card-value>
              <span class="text-2xl font-bold">{{ timeUtils.formatDuration(metrics.avgAttemptRuntimeInMs) }}</span>
            </template>
            Average {{ metrics.quizType }} runtime for
            <Tag severity="success">{{ metrics.numTaken }}</Tag>
            {{ isSurvey ? 'user' : 'attempt' }}{{ metrics.numTaken != 1 ? 's' : '' }}
          </StatsCard>
        </div>
      </div>

      <Card :pt="{ body: { class: 'p-0!' } }">
        <template #content>
          <div v-for="(q, index) in metrics.questions" :key="q.id" class="mb-8">
            <QuizQuestionMetrics :q="q" :num="index" :is-survey="isSurvey" :dateRange="filterRange"/>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
</style>