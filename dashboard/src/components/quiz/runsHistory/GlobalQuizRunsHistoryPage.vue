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

import {ref} from 'vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import QuizRunsTable from "@/components/quiz/runsHistory/QuizRunsTable.vue";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

const announcer = useSkillsAnnouncer()

const filterRange = ref([]);

const clearDateFilter = () => {
  announcer.polite("Clearing the date range filter")
  filterRange.value = [];
};
</script>

<template>
  <div class="flex flex-col flex-wrap">
    <SubPageHeader title="Quiz and Survey Runs"
                   aria-label="Quiz and Survey Runs" />

    <Card :pt="{ body: { class: 'p-0!' } }">
      <template #content>

        <quiz-runs-table
            table-stored-state-id="globalQuizRunsHistoryTable"
            :show-quiz-name-and-type-columns="true"
            :show-controls="false"
            :date-range="filterRange"
            @on-clear-filter="clearDateFilter"
            :refresh-on-date-range-change="false">
          <template #header>
            <div class="flex mb-2 justify-end">
                <SkillsCalendarInput
                    label="Date Range:"
                    label-icon="fas fa-calendar-alt"
                    :label-on-same-line="true"
                    selectionMode="range"
                    name="filterRange" v-model="filterRange" :maxDate="new Date()"
                    placeholder="Select a date range" data-cy="metricsDateFilter"/>
            </div>
          </template>
        </quiz-runs-table>
      </template>
    </Card>

  </div>
</template>

<style scoped>
</style>