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

import {computed, onMounted, ref} from 'vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useUserTagsUtils } from '@/components/utils/UseUserTagsUtils.js'
import { useQuizSummaryState } from '@/stores/UseQuizSummaryState.js'
import { FilterMatchMode } from '@primevue/core/api'
import { useRoute } from 'vue-router'
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { useFocusState } from '@/stores/UseFocusState.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import { useColors } from '@/skills-display/components/utilities/UseColors.js';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import QuizAttemptsTimeChart from '@/components/quiz/metrics/QuizAttemptsTimeChart.vue'
import QuizUserTagsChart from '@/components/quiz/metrics/QuizUserTagsChart.vue'
import InputGroup from 'primevue/inputgroup'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import DateCell from '@/components/utils/table/DateCell.vue'
import InputGroupAddon from 'primevue/inputgroupaddon'
import Column from 'primevue/column'
import QuizService from '@/components/quiz/QuizService.js'
import InputText from 'primevue/inputtext'
import QuizRunStatus from '@/components/quiz/runsHistory/QuizRunStatus.vue'
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import {useStorage} from "@vueuse/core";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import Badge from "primevue/badge";

const route = useRoute()
const userInfo = useUserInfo()
const timeUtils = useTimeUtils()
const focusState = useFocusState()
const quizSummaryState = useQuizSummaryState()
const userTagsUtils = useUserTagsUtils()
const announcer = useSkillsAnnouncer()
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const numberFormat = useNumberFormat()
const layoutSizes = useLayoutSizesState()
const filterRange = ref([]);

const quizType = ref('')
const runsHistory = ref([])
const filtering = ref(false)
const options = ref({
  emptyText: 'Click Test+ on the top-right to create a test!',
  busy: false,
  bordered: true,
  outlined: true,
  stacked: 'md',
  fields: [
    {
      key: 'userIdForDisplay',
      label: 'User',
      sortable: true,
      imageClass: 'fas fa-user skills-color-users'
    },
    {
      key: 'status',
      label: 'Status',
      sortable: true,
      imageClass: 'fas fa-trophy skills-color-points'
    },
    {
      key: 'runtime',
      label: 'Runtime',
      sortable: false,
      imageClass: 'fas fa-user-clock skills-color-access'
    },
    {
      key: 'started',
      label: 'Started',
      sortable: true,
      imageClass: 'fas fa-clock text-warning'
    },
    {
      key: 'results',
      label: 'Results',
      sortable: false,
    },
    {
      key: 'controls',
      label: '',
      sortable: false,
      class: 'controls-column'
    }
  ],
  pagination: {
    server: true,
    currentPage: 1,
    totalRows: 0,
    possiblePageSizes: [10, 20, 50]
  }
})
const pageSize = useStorage('quizRunsHistory-tablePageSize', 10)
const sortInfo = ref({ sortOrder: -1, sortBy: 'started' })
const deleteQuizRunInfo = ref({
  showDialog: false,
  quizRun: {}
})
const totalRows = ref(0)

onMounted(() => {
  if (userTagsUtils.showUserTagColumn()) {
    options.value.fields.splice(1, 0, {
      key: 'userTag',
      label: userTagsUtils.userTagLabel(),
      sortable: true
    })
  }
  loadData()
})

const loadData = () => {
  options.value.busy = true
  const dateRange = timeUtils.prepareDateRange(filterRange.value)
  const params = {
    query: filters.value.global.value ? filters.value.global.value.trim() : '',
    limit: pageSize.value,
    ascending: sortInfo.value.sortOrder === 1 ? true : false,
    page: options.value.pagination.currentPage,
    orderBy: sortInfo.value.sortBy,
    startDate: dateRange.startDate,
    endDate: dateRange.endDate
  }

  return quizSummaryState.afterQuizSummaryLoaded().then((quizSummary) => {
    quizType.value = quizSummary.type
    return QuizService.getQuizRunsHistory(route.params.quizId, params)
      .then((res) => {
        let items = res.data
        runsHistory.value = items
        options.value.pagination.totalRows = res.count
        totalRows.value = res.count
      })
      .finally(() => {
        options.value.busy = false
      })
  })
}

const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS }
})

const clearFilter = () => {
  filters.value.global.value = null
  loadData().then(() => filtering.value = false)
}
const onFilter = (filterEvent) => {
  loadData().then(() => filtering.value = true)
}
const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
  options.value.pagination.currentPage = pagingInfo.page + 1
  loadData()
}
const sortField = (column) => {
  // set to the first page
  options.value.pagination.currentPage = 1
  loadData()
}

const initiateDelete = (quizRun) => {
  deleteQuizRunInfo.value.quizRun = quizRun
  deleteQuizRunInfo.value.showDialog = true
}
const deleteRun = () => {
  options.value.busy = true
  QuizService.deleteQuizRunHistoryItem(route.params.quizId, deleteQuizRunInfo.value.quizRun.attemptId)
    .then(() => {
      loadData().then(() => {
        focusState.setElementId('filterResetBtn')
        focusState.focusOnLastElement()
        announcer.polite(`${quizType.value} Run for ${deleteQuizRunInfo.value.quizRun.userIdForDisplay} was successfully removed!`)
      })
    })
}

const subFilter = ref([]);

const applyDateFilter = () => {
  announcer.polite(`Results have been filtered by date, from ${filterRange.value[0]}` + filterRange.value.length > 1 ? ` to ${filterRange.value[1]}` : '')
  subFilter.value = filterRange.value
  loadData()
};

const clearDateFilter = () => {
  announcer.polite("Clearing the date range filter")
  filterRange.value = [];
  subFilter.value = [];
  loadData()
};
</script>

<template>
  <div class="flex flex-col flex-wrap">
    <SubPageHeader title="Runs"
                   aria-label="Runs">
      <template #underTitle>
        <div class="flex flex-wrap gap-2 items-center">
          <div>
            Filter by Date(s):
          </div>
          <div class="flex gap-2">
            <SkillsCalendarInput selectionMode="range" name="filterRange" v-model="filterRange" :maxDate="new Date()" placeholder="Select a date range" data-cy="metricsDateFilter" />
            <SkillsButton label="Apply" @click="applyDateFilter" data-cy="applyDateFilterButton" />
            <SkillsButton label="Clear" @click="clearDateFilter" data-cy="clearDateFilterButton" />
          </div>
        </div>
      </template>
    </SubPageHeader>
    <div :style="`width: ${layoutSizes.tableMaxWidth}px;`">
      <QuizAttemptsTimeChart class="flex-1 w-full my-4" :dateRange="subFilter" />
      <QuizUserTagsChart v-if="userTagsUtils.showUserTagColumn()" class="flex-1 w-full mb-4" :style="`width: ${layoutSizes.tableMaxWidth}px;`" :dateRange="subFilter"/>
    </div>

    <Card :pt="{ body: { class: 'p-0!' } }">
      <template #content>
        <SkillsDataTable
          tableStoredStateId="quizRunHistory"
          aria-label="Quiz Run History"
          :value="runsHistory"
          :loading="options.busy"
          show-gridlines
          striped-rows
          lazy
          paginator
          data-cy="quizRunsHistoryTable"
          v-model:filters="filters"
          :globalFilterFields="['userIdForDisplay']"
          @filter="onFilter"
          @page="pageChanged"
          @sort="sortField"
          :rows="pageSize"
          :rowsPerPageOptions="options.pagination.possiblePageSizes"
          :total-records="options.pagination.totalRows"
          v-model:sort-field="sortInfo.sortBy"
          v-model:sort-order="sortInfo.sortOrder">
          <template #header>
            <div class="flex gap-1">
              <InputGroup>
                <InputGroupAddon>
                  <i class="fas fa-search" aria-hidden="true" />
                </InputGroupAddon>
                <InputText class="flex grow"
                           v-model="filters['global'].value"
                           v-on:keydown.enter="onFilter"
                           data-cy="userNameFilter"
                           placeholder="User filter"
                           aria-label="User Name Filter" />
              </InputGroup>
            </div>
            <div class="flex flex-wrap pt-4">
              <SkillsButton label="Filter"
                            icon="fa fa-filter"
                            size="small"
                            outlined
                            @click="onFilter"
                            :aria-label="`Filter ${quizType} results`"
                            data-cy="userFilterBtn" />
              <SkillsButton id="filterResetBtn"
                            class="ml-1"
                            label="Reset"
                            icon="fa fa-times"
                            size="small"
                            outlined
                            @click="clearFilter"
                            :aria-label="`Reset filter for ${quizType} results`"
                            data-cy="userResetBtn" />
            </div>
          </template>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
          </template>

          <template #empty>
            <table-no-res :showResetFilter="filtering" @resetFilter="clearFilter"/>
          </template>
          <Column v-for="(col, index) in options.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
                  :class="{'flex': responsive.lg.value }">
            <template #header>
              <span v-if="col.key === 'controls'" class="sr-only">Controls Heading - Not sortable</span>
              <span v-else><i :class="[col.imageClass, colors.getTextClass(index + 1)]" aria-hidden="true"></i> {{ col.label }}</span>
            </template>
            <template #body="slotProps">
              <div v-if="slotProps.field === 'userIdForDisplay'" class="flex flex-row flex-wrap"
                   :data-cy="`row${slotProps.index}-userCell`">
                <div class="flex items-start justify-start">
                  <highlighted-value :value="userInfo.getUserDisplay(slotProps.data, true)"
                                     :filter="filters.global.value" />
                </div>
                <div class="flex grow items-start justify-end">
                  <router-link :data-cy="`row${slotProps.index}-viewRun`" tabindex="-1"
                               :to="{ name: 'QuizSingleRunPage', params: { runId: slotProps.data.attemptId } }">
                    <SkillsButton icon="fas fa-list-ul"
                                  class="ml-2"
                                  outlined
                                  :aria-label="`View Run Details for user ${slotProps.data.userIdForDisplay}`"
                                  size="small" />
                  </router-link>
                </div>
              </div>
              <div v-else-if="slotProps.field === 'status'">
                <QuizRunStatus :quiz-type="quizType" :status="slotProps.data.status" />
              </div>
              <div v-else-if="slotProps.field === 'runtime'">
                <span
                  :data-cy="`row${slotProps.index}-runtime`">{{ timeUtils.formatDurationDiff(slotProps.data.started, slotProps.data.completed)
                  }}</span>
              </div>
              <div v-else-if="slotProps.field === 'started'">
                <DateCell :value="slotProps.data[col.key]" />
              </div>
              <div v-else-if="slotProps.field === 'results'">
                <div><Badge severity="success">{{ slotProps.data.numberCorrect }}</Badge> correct</div>
                <div>out of {{ slotProps.data.totalAnswers }} <span class="text-gray-500 dark:text-gray-200">({{ Math.trunc(100 * (slotProps.data.numberCorrect / slotProps.data.totalAnswers)) }}%)</span></div>
              </div>
              <div v-else-if="slotProps.field === 'controls'">
                <SkillsButton :data-cy="`row${slotProps.index}-deleteBtn`"
                              :id="`deleteAttempt-${slotProps.data.attemptId}`"
                              @click="initiateDelete(slotProps.data)"
                              icon="fa fa-trash"
                              size="small"
                              outlined
                              severity="warn"
                              :track-for-focus="true"
                              :aria-label="`delete ${quizType} result for ${slotProps.data.userIdForDisplay}`" />
              </div>
              <div v-else>
                <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ slotProps.data[col.key] }}</span>
              </div>
            </template>
          </Column>
        </SkillsDataTable>
      </template>
    </Card>

    <RemovalValidation
      v-if="deleteQuizRunInfo.showDialog"
      v-model="deleteQuizRunInfo.showDialog"
      @do-remove="deleteRun"
      :removal-text-prefix="`This will remove the ${quizType} result for`"
      :item-name="deleteQuizRunInfo.quizRun.userIdForDisplay"
      item-type="user"
      :enable-return-focus="true">
      <div>
        Deletion <b>cannot</b> be undone and permanently removes all of the underlying user's answers.
      </div>
    </RemovalValidation>
  </div>
</template>

<style scoped>
.controls-column {
  max-width: 2.5rem !important;
}
</style>