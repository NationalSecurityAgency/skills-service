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

import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import QuizService from "@/components/quiz/QuizService.js";
import {useRoute} from "vue-router";
import {computed, onMounted, ref} from "vue";
import QuestionType from "@/skills-display/components/quiz/QuestionType.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import QuizStatus from "@/components/quiz/runsHistory/QuizStatus.js";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import Column from "primevue/column";
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import {useUserInfo} from "@/components/utils/UseUserInfo.js";
import DateCell from "@/components/utils/table/DateCell.vue";
import GradeQuizAttempt from "@/components/quiz/grade/GradeQuizAttempt.vue";

const route = useRoute()
const numberFormat = useNumberFormat()
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const userInfo = useUserInfo()

const hasGradableQuestionsDefined = ref(false)
const runningQuizDefinitionCheck = ref(true)
const checkIfQuizHasInputTextQuestions = () => {
  return QuizService.getQuizQuestionDefs(route.params.quizId).then((res) => {
    if (res.questions) {
      hasGradableQuestionsDefined.value = res.questions.find((q) => QuestionType.isTextInput(q.questionType)) !== undefined
      return hasGradableQuestionsDefined.value
    }
    return false
  }).finally(() => {
    runningQuizDefinitionCheck.value = false
  })
}

const sortInfo = ref({ sortOrder: -1, sortBy: 'started' })
const pagination = ref({
  currentPage: 1,
  totalRows: 0,
  pageSize: 10,
  possiblePageSizes: [10, 20, 50]
})
const loadingQuizRunsFirstTime = ref(true)
const loadingQuizRuns = ref(true)
const quizRunsThatNeedGrading = ref([])
const loadQuizRuns = () => {
  loadingQuizRuns.value = true
  const params = {
    query: '',
    quizAttemptStatus: QuizStatus.NeedsGrading,
    limit: pagination.value.pageSize,
    ascending: sortInfo.value.sortOrder === 1,
    page: pagination.value.currentPage,
    orderBy: sortInfo.value.sortBy
  }

  return QuizService.getQuizRunsHistory(route.params.quizId, params).then((res) => {
    quizRunsThatNeedGrading.value = res.data.map((q) => ({ ...q, isGraded: false }))
    pagination.value.totalRows = res.count
  }).finally(() => {
    loadingQuizRuns.value = false
    loadingQuizRunsFirstTime.value = false
  })
}
const pageChanged = (pagingInfo) => {
  pagination.value.pageSize = pagingInfo.rows
  pagination.value.currentPage = pagingInfo.page + 1
  return loadQuizRuns()
}
const sortField = (column) => {
  // set to the first page
  pagination.value.currentPage = 1
  return loadQuizRuns()
}

const expandedRows = ref([]);

const toggleRow = (row) => {
  if(expandedRows.value[row]) {
    delete expandedRows.value[row];
  }
  else {
    expandedRows.value[row] = true;
  }

  expandedRows.value = { ...expandedRows.value };
}

onMounted(() => {
  checkIfQuizHasInputTextQuestions().then((hasGradableQuestions) => {
    if (hasGradableQuestions) {
      loadQuizRuns()
    } else {
      loadingQuizRuns.value = false
      loadingQuizRunsFirstTime.value = false
    }
  })
})

const onGraded = (quizAttempt, gradedInfo) => {
  quizAttempt.isGraded = gradedInfo.doneGradingAttempt
}
const isLoading = computed(() => runningQuizDefinitionCheck.value || loadingQuizRunsFirstTime.value)
</script>

<template>
  <div>
    <SubPageHeader title="Grading"/>
    <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
      <template #content>

        <skills-spinner v-if="isLoading" :is-loading="isLoading" class="py-8"/>
        <div v-else>
          <SkillsDataTable
              tableStoredStateId="quizRunsToGradeTable"
              aria-label="Quiz Runs to Grade"
              :value="quizRunsThatNeedGrading"
              :loading="loadingQuizRuns"
              show-gridlines
              striped-rows
              lazy
              paginator
              data-cy="quizRunsToGradeTable"
              @page="pageChanged"
              @sort="sortField"
              :rows="pagination.pageSize"
              :rowsPerPageOptions="pagination.possiblePageSizes"
              :total-records="pagination.totalRows"
              v-model:expandedRows="expandedRows"
              dataKey="attemptId"
              v-model:sort-field="sortInfo.sortBy"
              v-model:sort-order="sortInfo.sortOrder">

            <Column header="User" field="userIdForDisplay" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <div class="mr-2"><i class="fas fa-user skills-color-users" :class="colors.getTextClass(1)" aria-hidden="true"></i> </div>
              </template>
              <template #body="slotProps">
                <div :data-cy="`userCell_${slotProps.data.userId}`" class="flex flex-row flex-wrap">
                  <div class="flex align-items-start justify-content-start">
                    {{ userInfo.getUserDisplay(slotProps.data, true) }}
                  </div>
                  <div class="flex flex-grow-1 align-items-start justify-content-end">
                    <SkillsButton v-if="!slotProps.data.isGraded"
                                  icon="fas fa-pencil-alt"
                                  label="Grade"
                                  @click="toggleRow(slotProps.data.attemptId)"
                                  class="ml-2"
                                  outlined
                                  :aria-label="`Grade Quiz for ${slotProps.data.userIdForDisplay}`"
                                  size="small"/>
                    <div v-else><Tag severity="success"><i class="fas fa-check mr-1" aria-hidden="true" /> Graded</Tag></div>
                  </div>
                </div>
              </template>
            </Column>
            <Column header="Date" field="completed" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <div class="mr-2"><i class="fas fa-user skills-color-users" :class="colors.getTextClass(1)" aria-hidden="true"></i> </div>
              </template>
              <template #body="slotProps">
                  <DateCell :value="slotProps.data.completed" />
              </template>
            </Column>

            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(pagination.totalRows) }}</span>
            </template>
            <template #empty>
              <div>
                There are currently no Quiz Runs to grade.
              </div>
            </template>
            <template #expansion="slotProps">
              <grade-quiz-attempt
                  :quiz-attempt-id="slotProps.data.attemptId"
                  :user-id="slotProps.data.userId"
                  @on-graded="onGraded(slotProps.data, $event)"
              />
            </template>
          </SkillsDataTable>


          <NoContent2
              v-if="!hasGradableQuestionsDefined"
              title="No Manual Grading Required" class="pt-5 pb-8">
            No Input Text questions have been added to this quiz. Note that only free-form Input Text answers require
            manual grading. If you add questions with Input Text answers, additional grading controls will become
            available on this page.
          </NoContent2>
        </div>

      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>