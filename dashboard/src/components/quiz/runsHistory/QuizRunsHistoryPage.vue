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

import {ref} from 'vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import QuizAttemptsTimeChart from '@/components/quiz/metrics/QuizAttemptsTimeChart.vue'
import QuizUserTagsChart from '@/components/quiz/metrics/QuizUserTagsChart.vue'
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import QuizRunsTable from "@/components/quiz/runsHistory/QuizRunsTable.vue";
import {useUserTagsUtils} from "@/components/utils/UseUserTagsUtils.js";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

const layoutSizes = useLayoutSizesState()
const userTagsUtils = useUserTagsUtils()
const announcer = useSkillsAnnouncer()

const filterRange = ref([]);
const subFilter = ref([]);

const applyDateFilter = () => {
  announcer.polite(`Results have been filtered by date, from ${filterRange.value[0]}` + filterRange.value.length > 1 ? ` to ${filterRange.value[1]}` : '')
  subFilter.value = filterRange.value
};

const clearDateFilter = () => {
  announcer.polite("Clearing the date range filter")
  filterRange.value = [];
  subFilter.value = [];
};
</script>

<template>
  <div class="flex flex-col flex-wrap">
    <SubPageHeader title="Runs"
                   aria-label="Runs">
      <template #underTitle>
        <div class="flex flex-wrap gap-2 items-center">
            <SkillsCalendarInput selectionMode="range" name="filterRange" v-model="filterRange"
                                 :maxDate="new Date()" placeholder="Select a date range"
                                 label="Filter by Date(s):"
                                 :label-on-same-line="true"
                                 data-cy="metricsDateFilter" />
            <SkillsButton label="Apply" @click="applyDateFilter" data-cy="applyDateFilterButton" />
            <SkillsButton label="Clear" @click="clearDateFilter" data-cy="clearDateFilterButton" />
        </div>
      </template>
    </SubPageHeader>
    <div class="relative">
      <div class="absolute inset-0">
        <QuizAttemptsTimeChart class="flex-1 w-full my-4" :dateRange="subFilter" />
        <QuizUserTagsChart v-if="userTagsUtils.showUserTagColumn()" class="flex-1 w-full mb-4" :style="`width: ${layoutSizes.tableMaxWidth}px;`" :dateRange="subFilter"/>
        <Card :pt="{ body: { class: 'p-0!' } }">
          <template #content>
            <quiz-runs-table table-stored-state-id="quizAdminRunsHistoryTable" :dateRange="subFilter"/>
          </template>
        </Card>
      </div>
    </div>

  </div>
</template>

<style scoped>
</style>