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

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useTruncateFormatter } from '@/components/utils/UseTruncateFormatter.js'
import { useUserTagsUtils } from '@/components/utils/UseUserTagsUtils.js'
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import Column from 'primevue/column'
import QuizService from '@/components/quiz/QuizService.js'
import DateCell from '@/components/utils/table/DateCell.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";

const props = defineProps({
  answerDefId: Number,
  isSurvey: Boolean,
  questionType: {
    type: String,
    default: '',
  },
})

const route = useRoute();
const truncateFormatter = useTruncateFormatter();
const userTagsUtils = useUserTagsUtils();
const userInfo = useUserInfo();
const numberFormat = useNumberFormat()
const responsive = useResponsiveBreakpoints()
const quizId = ref(route.params.quizId);
const answerHistory = ref([]);
const sortInfo = ref({  sortOrder: 1, sortBy: 'updated' })
const tableOptions = ref({
  busy: false,
  bordered: true,
  outlined: true,
  stacked: 'md',
  fields: [],
  pagination: {
    server: true,
    currentPage: 1,
    totalRows: 0,
    pageSize: 5,
    possiblePageSizes: [5, 10, 15, 20],
  },
})
const answerTxtTruncate = {
  truncateThreshold: 600,
  truncateTo: 550,
};

onMounted(() => {
  const fields = [];
  fields.push({
    key: 'userIdForDisplay',
    label: 'User',
    sortable: true,
    imageClass: 'fas fa-user skills-color-users',
    dataCy: 'usrColumnHeader',
  });
  if (isTextInput.value && !props.isSurvey) {
    fields.push({
      key: 'status',
      label: 'Result',
      sortable: true,
      imageClass: 'far fa-check-square',
      dataCy: 'answerCorrectCol',
    });
  }
  if (userTagsUtils.showUserTagColumn()) {
    fields.push({
      key: 'userTag',
      label: userTagsUtils.userTagLabel(),
      sortable: true,
    });
  }
  fields.push({
    key: 'updated',
    label: 'Date',
    sortable: true,
    imageClass: 'far fa-clock skills-color-events',
  });
  tableOptions.value.fields = fields;
  loadData();
});

const isTextInput = computed(() => {
  return props.questionType === 'TextInput';
});

const loadData = () => {
  tableOptions.value.busy = true;
  const params = {
    limit: tableOptions.value.pagination.pageSize,
    ascending: sortInfo.value.sortOrder === 1,
    page: tableOptions.value.pagination.currentPage,
    orderBy: sortInfo.value.sortBy,
  };
  return QuizService.getQuizAnswerSelectionHistory(quizId.value, props.answerDefId, params)
      .then((res) => {
        answerHistory.value = res.data.map((item) => {
          const toTruncate = item.answerTxt && item.answerTxt.length >= answerTxtTruncate.truncateThreshold;
          return ({
            ...item,
            truncated: toTruncate,
            truncationEnabled: toTruncate,
          });
        });
        tableOptions.value.pagination.totalRows = res.count;
        tableOptions.value.pagination.hideUnnecessary = res.totalCount <= tableOptions.value.pagination.pageSize;
        if (expanded.value) {
          expandAll()
        }
      })
      .finally(() => {
        tableOptions.value.busy = false;
      });
}

const pageChanged = (pagingInfo) => {
  tableOptions.value.pagination.pageSize = pagingInfo.rows;
  tableOptions.value.pagination.currentPage = pagingInfo.page + 1;
  loadData();
};

const sortField = (column) => {
  // set to the first page
  tableOptions.value.pagination.currentPage = 1;
  loadData();
};

const expandedRows = ref({});
const expanded = ref(false)
const expandAll = () => {
  expanded.value = true
  answerHistory.value.forEach((item) => {
    expandedRows.value[item.userQuizAttemptId] = true;
  })
}
const collapseAll = () => {
  expanded.value = false
  expandedRows.value = {}
}
</script>

<template>
  <div>
    <SkillsDataTable
        tableStoredStateId="answerHistory"
        :value="answerHistory"
        :loading="tableOptions.busy"
        stripedRows
        showGridlines
        lazy
        :paginator="true"
        dataKey="userQuizAttemptId"
        v-model:expandedRows="expandedRows"
        :rows="tableOptions.pagination.pageSize"
        :rowsPerPageOptions="tableOptions.pagination.possiblePageSizes"
        :total-records="tableOptions.pagination.totalRows"
        @page="pageChanged"
        @sort="sortField"
        v-model:sort-field="sortInfo.sortBy"
        v-model:sort-order="sortInfo.sortOrder"
        aria-label="Answer History"
        data-cy="quizAnswerHistoryTable">

      <template v-if="isTextInput" #header>
        <div class="text-right">
          <SkillsButton v-if="expanded" label="Collapse All"
                        icon="fas fa-compress-alt"
                        class="mr-2"
                        data-cy="collapseAll"
                        @click="collapseAll"
                        outlined
                        size="small"/>
          <SkillsButton v-if="!expanded" label="Expand All"
                        icon="fas fa-expand-alt"
                        data-cy="expandAll"
                        @click="expandAll"
                        outlined
                        size="small"/>
        </div>
      </template>

      <template #paginatorstart>
        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(tableOptions.pagination.totalRows) }}</span>
      </template>

      <template #empty>
        <div class="flex justify-content-center flex-wrap">
          <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle" aria-hidden="true"></i>
          <span class="flex align-items-center justify-content-center">There are no records to show</span>
        </div>
      </template>
      <template #expansion="slotProps">
        <MarkdownText
            :text="slotProps.data.answerTxt"
            :instance-id="`${answerDefId}-${slotProps.index}-answerTxt`"
            :data-cy="`row${slotProps.index}-colAnswerTxt`"/>
      </template>
      <Column v-if="isTextInput" expander style="width: 5rem" />
      <Column v-for="col of tableOptions.fields"
              :key="col.key"
              :field="col.key"
              :sortable="col.sortable"
              class="vertical-align-top"
              :class="{'flex': responsive.md.value }">
        <template #header>
          <span :data-cy="col.dataCy" ><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field === 'userIdForDisplay'" class="flex flex-row flex-wrap align-items-center"  :data-cy="`row${slotProps.index}-colUserId`">
            <div class="flex flex-grow-1 justify-content-start mb-2">
              {{ userInfo.getUserDisplay(slotProps.data, true) }}
            </div>
            <div class="flex justify-content-start mb-2">
              <router-link :data-cy="`managesQuizBtn_${slotProps.data.quizId}`"
                           :aria-label="`View quiz attempt for ${slotProps.data.userQuizAttemptId} id`"
                           :to="{ name: 'QuizSingleRunPage', params: { runId: slotProps.data.userQuizAttemptId } }" tabindex="-1">
                  <SkillsButton label="View Run"
                                icon="fas fa-eye"
                                data-cy="viewRunBtn"
                              class="flex-shrink-1"
                                outlined
                                size="small"/>
              </router-link>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'userTag'">
            <span :data-cy="`row${slotProps.index}-userTag`">{{ slotProps.data[col.key] }}</span>
          </div>
          <div v-else-if="slotProps.field === 'updated'" style="min-width: 13rem;">
            <DateCell :value="slotProps.data[col.key]" />
          </div>
          <div v-else>
            {{ slotProps.data[col.key] }}
          </div>
        </template>
      </Column>
    </SkillsDataTable>
  </div>
</template>

<style scoped>
pre {
  overflow-x: auto;
  white-space: pre-wrap;
  white-space: -moz-pre-wrap;
  white-space: -pre-wrap;
  white-space: -o-pre-wrap;
  word-wrap: break-word;
}
</style>