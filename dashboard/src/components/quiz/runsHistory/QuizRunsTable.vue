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

import Badge from "primevue/badge";
import Column from "primevue/column";
import QuizRunStatus from "@/components/quiz/runsHistory/QuizRunStatus.vue";
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import InputText from "primevue/inputtext";
import HighlightedValue from "@/components/utils/table/HighlightedValue.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import {onMounted, ref, watch} from "vue";
import {useStorage} from "@vueuse/core";
import QuizService from "@/components/quiz/QuizService.js";
import {useRoute} from "vue-router";
import {useUserInfo} from "@/components/utils/UseUserInfo.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {useFocusState} from "@/stores/UseFocusState.js";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import {useUserTagsUtils} from "@/components/utils/UseUserTagsUtils.js";
import RemovalValidation from "@/components/utils/modal/RemovalValidation.vue";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";

const route = useRoute()
const userInfo = useUserInfo()
const timeUtils = useTimeUtils()
const focusState = useFocusState()
const announcer = useSkillsAnnouncer()
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const numberFormat = useNumberFormat()
const userTagsUtils = useUserTagsUtils()

const emits = defineEmits(['on-clear-filter'])
const props = defineProps({
  tableStoredStateId: String,
  dateRange: Array,
  refreshOnDateRangeChange: {
    type: Boolean,
    default: true,
  },
  showControls: {
    type: Boolean,
    default: true,
  },
  enableToShowUserTagColumn: {
    type: Boolean,
    default: true,
  },
  onlyRunsForUserId: String,
  showQuizNameAndTypeColumns: {
    type: Boolean,
    default: false,
  }
})

const loadingDataInitially = ref(true)
const hasDataToShow = ref(false)
const internalDateRange = ref([])
const runsHistory = ref([])
const pageSize = useStorage('quizRunsHistory-tablePageSize', 10)
const sortInfo = ref({ sortOrder: -1, sortBy: 'started' })
const deleteQuizRunInfo = ref({
  showDialog: false,
  quizRun: {},
})

const totalRows = ref(0)
const filtering = ref(false)
const fields = []
if (!props.onlyRunsForUserId) {
  fields.push({
    key: 'userIdForDisplay',
    label: 'User',
    sortable: true,
    imageClass: 'fas fa-user skills-color-users'
  })
}
if (props.showQuizNameAndTypeColumns) {
  fields.push({
    key: 'quizName',
    label: 'Name',
    sortable: true,
    imageClass: 'fas fa-user skills-color-users'
  }, {
    key: 'quizType',
    label: 'Type',
    sortable: true,
    imageClass: 'fas fa-user skills-color-users'
  })

}
fields.push(
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
  }
)
if (props.showControls) {
  fields.push({
    key: 'controls',
    label: '',
    sortable: false,
    class: 'controls-column'
  })
}
const options = ref({
  emptyText: 'Click Test+ on the top-right to create a test!',
  busy: false,
  bordered: true,
  outlined: true,
  stacked: 'md',
  fields,
  pagination: {
    server: true,
    currentPage: 1,
    totalRows: 0,
    possiblePageSizes: [10, 20, 50]
  }
})

onMounted(() => {
  if (props.enableToShowUserTagColumn && userTagsUtils.showUserTagColumn()) {
    const position = props.showQuizNameAndTypeColumns ? 3 : 1
    options.value.fields.splice(position, 0, {
      key: 'userTag',
      label: userTagsUtils.userTagLabel(),
      sortable: true
    })
  }
  loadData().then(() => {
    loadingDataInitially.value = false
    hasDataToShow.value = runsHistory.value?.length > 0
  })
})

watch(() => props.dateRange, (newVal) => {
  internalDateRange.value = newVal
  if (props.refreshOnDateRangeChange) {
    loadData()
  }
})


const loadData = () => {
  options.value.busy = true
  const dateRange = timeUtils.prepareDateRange(internalDateRange.value)
  const params = {
    query: props.onlyRunsForUserId || (userFilter.value ? userFilter.value.trim() : ''),
    nameQuery: quizNameFilter.value ? quizNameFilter.value.trim() : '',
    limit: pageSize.value,
    ascending: sortInfo.value.sortOrder === 1 ? true : false,
    page: options.value.pagination.currentPage,
    orderBy: sortInfo.value.sortBy,
    startDate: dateRange.startDate,
    endDate: dateRange.endDate
  }

  return getQuizRunHistory(params)
      .then((res) => {
        runsHistory.value = res.data
        options.value.pagination.totalRows = res.count
        totalRows.value = res.count
      })
      .finally(() => {
        options.value.busy = false
      })
}

const getQuizRunHistory = (params) => {
  return route.params.quizId ? QuizService.getQuizRunsHistory(route.params.quizId, params) : QuizService.getGlobalQuizRunsHistory(params)
}

const userFilter = ref(null)
const quizNameFilter = ref(null)

const clearFilter = () => {
  userFilter.value = null
  quizNameFilter.value = null
  if (!props.refreshOnDateRangeChange) {
    internalDateRange.value = []
  }
  emits('on-clear-filter')
  loadData().then(() => filtering.value = false)
}
const onFilter = () => {
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
  QuizService.deleteQuizRunHistoryItem(deleteQuizRunInfo.value.quizRun.quizId, deleteQuizRunInfo.value.quizRun.attemptId)
      .then(() => {
        loadData().then(() => {
          focusState.setElementId('filterResetBtn')
          focusState.focusOnLastElement()
          announcer.polite(`${deleteQuizRunInfo.value.quizRun.quizType} Run for ${deleteQuizRunInfo.value.quizRun.userIdForDisplay} was successfully removed!`)
        })
      })
}
</script>

<template>
  <div>
    <skills-spinner v-if="loadingDataInitially" :is-loading="true" class="my-10" />
    <slot v-if="!loadingDataInitially && !hasDataToShow" name="noResults"><no-content2 title="No Quiz Runs Available" message="No quiz runs have been completed yet. Once users take quizzes, their results will appear here." class="my-10" /></slot>
    <SkillsDataTable v-if="!loadingDataInitially && hasDataToShow"
        :tableStoredStateId="tableStoredStateId"
        aria-label="Quiz Run History"
        :value="runsHistory"
        :loading="options.busy"
        show-gridlines
        striped-rows
        lazy
        paginator
        data-cy="quizRunsHistoryTable"
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
        <slot name="header"></slot>

        <div class="flex gap-5">
          <div v-if="!props.onlyRunsForUserId" class="flex gap-1 flex-1">
            <label class="flex gap-1 items-center"><i class="fas fa-user" aria-hidden="true"/>
              <div>User:</div>
            </label>
            <InputText class="flex grow"
                       v-model="userFilter"
                       v-on:keydown.enter="onFilter"
                       data-cy="userNameFilter"
                       placeholder="User Filter"
                       aria-label="User Filter"/>
          </div>
          <div v-if="props.showQuizNameAndTypeColumns" class="flex gap-1 flex-1">
            <label class="flex gap-1 items-center"><i class="fas fa-tag" aria-hidden="true"/>
              <div>Name:</div>
            </label>
            <InputText class="flex grow"
                       v-model="quizNameFilter"
                       v-on:keydown.enter="onFilter"
                       data-cy="userNameFilter"
                       placeholder="Name Filter"
                       aria-label="Name Filter"/>
          </div>
        </div>
        <div class="flex flex-wrap pt-4">
          <SkillsButton label="Filter"
                        icon="fa fa-filter"
                        size="small"
                        outlined
                        @click="onFilter"
                        :aria-label="`Filter results`"
                        data-cy="userFilterBtn"/>
          <SkillsButton id="filterResetBtn"
                        class="ml-1"
                        label="Reset"
                        icon="fa fa-times"
                        size="small"
                        outlined
                        @click="clearFilter"
                        :aria-label="`Reset filter for results`"
                        data-cy="userResetBtn"/>
        </div>
      </template>

      <template #paginatorstart>
        <span>Total Rows:</span> <span class="font-semibold"
                                       data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
      </template>

      <template #empty>
        <table-no-res :showResetFilter="filtering" @resetFilter="clearFilter"/>
      </template>
      <Column v-for="(col, index) in options.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
              :class="{'flex': responsive.lg.value }">
        <template #header>
          <span v-if="col.key === 'controls'" class="sr-only">Controls Heading - Not sortable</span>
          <span v-else><i :class="[col.imageClass, colors.getTextClass(index + 1)]" aria-hidden="true"></i> {{
              col.label
            }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field === 'userIdForDisplay'" class="flex flex-row flex-wrap"
               :data-cy="`row${slotProps.index}-userCell`">
            <div class="flex items-start justify-start">
              <highlighted-value :value="userInfo.getUserDisplay(slotProps.data, true)"
                                 :filter="userFilter"/>
            </div>
            <div class="flex grow items-start justify-end">
              <router-link :data-cy="`row${slotProps.index}-viewRun`" tabindex="-1"
                           :to="{ name: 'QuizSingleRunPage', params: { quizId: slotProps.data.quizId, runId: slotProps.data.attemptId } }">
                <SkillsButton icon="fas fa-list-ul"
                              class="ml-2"
                              outlined
                              :aria-label="`View Run Details for user ${slotProps.data.userIdForDisplay}`"
                              size="small"/>
              </router-link>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'quizName'">
            <router-link :data-cy="`row${slotProps.index}-quizPageLink`"
                         :to="{ name: 'Questions', params: { quizId: slotProps.data.quizId } }"
                         class="underline">
              <highlighted-value :value="slotProps.data.quizName" :filter="quizNameFilter"/>
            </router-link>
          </div>
          <div v-else-if="slotProps.field === 'status'">
            <QuizRunStatus :quiz-type="slotProps.data.quizType" :status="slotProps.data.status"/>
          </div>
          <div v-else-if="slotProps.field === 'runtime'">
                <span
                    :data-cy="`row${slotProps.index}-runtime`">{{
                    timeUtils.formatDurationDiff(slotProps.data.started, slotProps.data.completed)
                  }}</span>
          </div>
          <div v-else-if="slotProps.field === 'started'">
            <DateCell :value="slotProps.data[col.key]"/>
          </div>
          <div v-else-if="slotProps.field === 'results'">
            <div>
              <Badge severity="success">{{ slotProps.data.numberCorrect }}</Badge>
              correct
            </div>
            <div>out of {{ slotProps.data.totalAnswers }} <span class="text-gray-500 dark:text-gray-200">({{
                Math.trunc(100 * (slotProps.data.numberCorrect / slotProps.data.totalAnswers))
              }}%)</span></div>
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
                          :aria-label="`delete ${slotProps.data.quizType} result for ${slotProps.data.userIdForDisplay}`"/>
          </div>
          <div v-else>
            <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ slotProps.data[col.key] }}</span>
          </div>
        </template>
      </Column>

      <RemovalValidation
          v-if="deleteQuizRunInfo.showDialog"
          v-model="deleteQuizRunInfo.showDialog"
          @do-remove="deleteRun"
          :removal-text-prefix="`This will remove the ${deleteQuizRunInfo.quizRun.quizType} result for`"
          :item-name="deleteQuizRunInfo.quizRun.userIdForDisplay"
          item-type="user"
          :enable-return-focus="true">
        <div>
          Deletion <b>cannot</b> be undone and permanently removes all of the underlying user's answers.
        </div>
      </RemovalValidation>
    </SkillsDataTable>
  </div>
</template>

<style scoped>
.controls-column {
  max-width: 2.5rem !important;
}
</style>