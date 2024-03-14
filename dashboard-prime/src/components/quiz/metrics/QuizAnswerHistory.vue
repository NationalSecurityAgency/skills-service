<script setup>

import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router'
import { useStorage } from '@vueuse/core';
import { useTruncateFormatter } from '@/components/utils/UseTruncateFormatter.js';
import { useUserTagsUtils} from '@/components/utils/UseUserTagsUtils.js';
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import Column from 'primevue/column';
import DataTable from 'primevue/datatable';
import QuizService from '@/components/quiz/QuizService.js';
import DateCell from "@/components/utils/table/DateCell.vue";

const props = defineProps({
  answerDefId: Number,
  questionType: {
    type: String,
    default: '',
  },
})

const route = useRoute();
const truncateFormatter = useTruncateFormatter();
const userTagsUtils = useUserTagsUtils();
const userInfo = useUserInfo();
const quizId = ref(route.params.quizId);
const answerHistory = ref([]);
const sortInfo = useStorage('quizAnswerHistoryTable', {  sortOrder: 1, sortBy: 'updated' })
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
  if (isTextInput.value) {
    fields.push({
      key: 'answerTxt',
      label: 'Answer',
      sortable: false,
      imageClass: 'fas fa-check-double skills-color-projects',
    });
  }
  fields.push({
    key: 'userIdForDisplay',
    label: 'User',
    sortable: true,
    imageClass: 'fas fa-user skills-color-users',
    dataCy: 'usrColumnHeader',
  });
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
    ascending: sortInfo.value.sortOrder === 1 ? true : false,
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

const expandIcon = (truncated) => {
  return truncated ? 'fas fa-expand-arrows-alt' : 'fas fa-compress-arrows-alt';
};
const expandLabel = (truncated) => {
  return truncated ? 'Expand Text' : 'Collapse';
};

</script>

<template>
  <div>
    <DataTable :value="answerHistory"
               :loading="tableOptions.busy"
               stripedRows
               showGridlines
               lazy
               :paginator="true"
               :rows="tableOptions.pagination.pageSize"
               :rowsPerPageOptions="tableOptions.pagination.possiblePageSizes"
               :total-records="tableOptions.pagination.totalRows"
               @page="pageChanged"
               @sort="sortField"
               v-model:sort-field="sortInfo.sortBy"
               v-model:sort-order="sortInfo.sortOrder"
               data-cy="quizAnswerHistoryTable">

      <template #paginatorstart>
        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ tableOptions.pagination.totalRows }}</span>
      </template>

      <template #empty>
        <div class="flex justify-content-center flex-wrap">
          <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle"
             aria-hidden="true"></i>
          <span class="flex align-items-center justify-content-center">There are no records to show
              </span>
        </div>
      </template>

      <Column v-for="col of tableOptions.fields"
              :key="col.key"
              :field="col.key"
              :sortable="col.sortable"
              class="vertical-align-top"
      >
        <template #header>
          <span :data-cy="col.dataCy" ><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field == 'answerTxt'" :data-cy="`row${slotProps.index}-colAnswerTxt`">
            <pre v-if="slotProps.data.truncated" data-cy="textTruncated">{{ truncateFormatter.truncate(slotProps.data[col.key], answerTxtTruncate.truncateTo) }}</pre>
            <pre v-else data-cy="text">{{ slotProps.data[col.key] }}</pre>
            <div class="text-right">
              <SkillsButton v-if="slotProps.data.truncationEnabled"
                            :label="expandLabel(slotProps.data.truncated)"
                            :icon="expandIcon(slotProps.data.truncated)"
                            outlined
                            size="small"
                            data-cy="expandCollapseTextBtn"
                            @click="slotProps.data.truncated = !slotProps.data.truncated">
              </SkillsButton>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'userIdForDisplay'" class="flex flex-row flex-wrap align-items-center"  :data-cy="`row${slotProps.index}-colUserId`">
            <div class="flex flex-grow-1 justify-content-start mb-2">
              {{ userInfo.getUserDisplay(slotProps.data, true) }}
            </div>
            <div class="flex justify-content-start mb-2">
              <router-link :data-cy="`managesQuizBtn_${slotProps.data.quizId}`"
                           :aria-label="`View quiz attempt for ${slotProps.data.userQuizAttemptId} id`"
                           :to="{ name: 'QuizSingleRunPage', params: { runId: slotProps.data.userQuizAttemptId } }">
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
    </DataTable>
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