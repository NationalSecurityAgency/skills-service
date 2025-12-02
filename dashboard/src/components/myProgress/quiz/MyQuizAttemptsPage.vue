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
import {onMounted, ref, computed} from "vue";
import MyProgressTitle from "@/components/myProgress/MyProgressTitle.vue";
import QuizRunService from "@/skills-display/components/quiz/QuizRunService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import Column from "primevue/column";
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import QuizRunStatus from "@/components/quiz/runsHistory/QuizRunStatus.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import InputGroup from "primevue/inputgroup";
import InputText from "primevue/inputtext";
import InputGroupAddon from "primevue/inputgroupaddon";
import {FilterMatchMode} from '@primevue/core/api';
import HighlightedValue from "@/components/utils/table/HighlightedValue.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import BackToMyProgressBtn from "@/components/myProgress/BackToMyProgressBtn.vue";
import {useStorage} from "@vueuse/core";

const responsive = useResponsiveBreakpoints()
const colors = useColors()
const timeUtils = useTimeUtils()
const numberFormat = useNumberFormat()

const pageSize = useStorage('myQuizAttempts-pageSize', 10)
const currentPage = ref(1)
const sortInfo = ref({sortOrder: -1, sortBy: 'started'})
const attemptHistory = ref([])
const totalRows = ref(0)
const loadingQuizAttemptsInitially = ref(true)
const loadingQuizAttempts = ref(false)
const possiblePageSizes = [10, 20, 50]

const loadQuizAttempts = () => {
  loadingQuizAttempts.value = true
  const params = {
    quizNameQuery: filters.value.global.value ? filters.value.global.value.trim() : '',
    limit: pageSize.value,
    ascending: sortInfo.value.sortOrder === 1,
    page: currentPage.value,
    orderBy: sortInfo.value.sortBy
  }
  return QuizRunService.getQuizAttempts(params).then((res) => {
    attemptHistory.value = res.data
    totalRows.value = res.count
  }).finally(() => {
    loadingQuizAttemptsInitially.value = false
    loadingQuizAttempts.value = false
  })
}

onMounted(() => {
  loadQuizAttempts()
})

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
  currentPage.value = pagingInfo.page + 1
  loadQuizAttempts()
}
const sortField = (column) => {
  // set to the first page
  currentPage.value = 1
  loadQuizAttempts()
}

const filtering = ref(false)
const filters = ref({
  global: {value: null, matchMode: FilterMatchMode.CONTAINS}
})
const clearFilter = () => {
  filters.value.global.value = null
  loadQuizAttempts().then(() => filtering.value = false)
}
const onFilter = (filterEvent) => {
  loadQuizAttempts().then(() => filtering.value = true)
}

const hasAttempts = computed(() => attemptHistory.value.length > 0)
</script>

<template>
  <div>
    <my-progress-title title="My Quizzes and Surveys" data-cy="myQuizAndSurveysTitle">
      <template #rightContent>
        <back-to-my-progress-btn />
      </template>
    </my-progress-title>
    <Card class="my-6" :pt="{ body: { class: 'p-0!' } }">
      <template #content>
        <SkillsSpinner v-if="loadingQuizAttemptsInitially" :is-loading="true" class="my-20"/>
        <div v-else>

          <div class="p-4">
            <div class="flex gap-1">
              <InputGroup>
                <InputGroupAddon>
                  <i class="fas fa-search" aria-hidden="true"/>
                </InputGroupAddon>
                <InputText class="flex grow"
                           v-model="filters['global'].value"
                           v-on:keydown.enter="onFilter"
                           data-cy="quizNameFilter"
                           placeholder="Name filter"
                           aria-label="Name Filter"/>
              </InputGroup>
            </div>
            <div class="flex flex-wrap pt-4">
              <SkillsButton label="Filter"
                            icon="fa fa-filter"
                            size="small"
                            outlined
                            @click="onFilter"
                            :aria-label="`Filter quiz/survey results`"
                            data-cy="userFilterBtn"/>
              <SkillsButton id="filterResetBtn"
                            class="ml-1"
                            label="Reset"
                            icon="fa fa-times"
                            size="small"
                            outlined
                            @click="clearFilter"
                            :aria-label="`Reset filter for quiz/survey results`"
                            data-cy="filterResetBtn"/>
            </div>
          </div>

          <SkillsDataTable
              v-if="hasAttempts"
              tableStoredStateId="myQuizAttemptsTable"
              aria-label="Quiz and Survey Attempts History"
              :value="attemptHistory"
              :loading="loadingQuizAttempts"
              show-gridlines
              striped-rows
              lazy
              paginator
              v-model:filters="filters"
              :globalFilterFields="['userIdForDisplay']"
              @filter="onFilter"
              data-cy="myQuizAttemptsTable"
              @page="pageChanged"
              @sort="sortField"
              :rows="pageSize"
              :rowsPerPageOptions="possiblePageSizes"
              :total-records="totalRows"
              v-model:sort-field="sortInfo.sortBy"
              v-model:sort-order="sortInfo.sortOrder">

            <Column header="Name" field="quizName" :sortable="true"
                    :class="{'flex': responsive.md.value }">
              <template #header>
                <div class="mr-2"><i class="fas fa-user skills-color-users" :class="colors.getTextClass(0)"
                                     aria-hidden="true"></i></div>
              </template>
              <template #body="slotProps">
                <router-link data-cy="viewQuizAttempt"
                             :to="{ name:'MySingleQuizAttemptPage', params: { attemptId: slotProps.data.attemptId }}"
                             :aria-label="`View attempt for ${slotProps.data.quizName} quiz`">
                  <highlighted-value :value="slotProps.data.quizName"
                                     :filter="filters.global.value"/>
                </router-link>
              </template>
            </Column>
            <Column header="Type" field="quizType" :sortable="true"
                    :class="{'flex': responsive.md.value }">
              <template #header>
                <div class="mr-2"><i class="fas fa-pencil-ruler" :class="colors.getTextClass(1)"
                                     aria-hidden="true"></i></div>
              </template>
              <template #body="slotProps">
                {{ slotProps.data.quizType }}
              </template>
            </Column>
            <Column header="Status" field="status" :sortable="true"
                    :class="{'flex': responsive.md.value }">
              <template #header>
                <div class="mr-2"><i class="fas fa-trophy" :class="colors.getTextClass(2)"
                                     aria-hidden="true"></i></div>
              </template>
              <template #body="slotProps">
                <QuizRunStatus :quiz-type="slotProps.data.quizType" :status="slotProps.data.status"/>
              </template>
            </Column>
            <Column header="Runtime" field="completed"
                    :class="{'flex': responsive.md.value }">
              <template #header>
                <div class="mr-2"><i class="fas fa-user-clock" :class="colors.getTextClass(3)"
                                     aria-hidden="true"></i></div>
              </template>
              <template #body="slotProps">
           <span
               :data-cy="`row${slotProps.index}-runtime`">{{
               timeUtils.formatDurationDiff(slotProps.data.started, slotProps.data.completed)
             }}</span>
              </template>
            </Column>
            <Column header="Started" field="started" :sortable="true"
                    :class="{'flex': responsive.md.value }">
              <template #header>
                <div class="mr-2"><i class="fas fa-clock" :class="colors.getTextClass(3)"
                                     aria-hidden="true"></i></div>
              </template>
              <template #body="slotProps">
                <DateCell :value="slotProps.data.started"/>
              </template>
            </Column>

            <template #paginatorstart>
              <span>Total Attempts:</span> <span class="font-semibold"
                                                 data-cy=skillsBTableTotalRows>{{
                numberFormat.pretty(totalRows)
              }}</span>
            </template>
          </SkillsDataTable>

          <NoContent2 v-if="!hasAttempts"
                      data-cy="noQuizzesOrSurveys"
                      title="No Quizzes or Surveys Completed Yet" class="py-20"
                      message="Quizzes and Surveys are often associated to projects' skills and once completed will be listed on this page. "
          />
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>