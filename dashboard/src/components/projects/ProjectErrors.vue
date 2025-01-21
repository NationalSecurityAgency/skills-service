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
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router';
import ProjectService from '@/components/projects/ProjectService';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import {useProjDetailsState} from "@/stores/UseProjDetailsState.js";
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import Column from 'primevue/column'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const dialogMessages = useDialogMessages()
const route = useRoute();
const numberFormat = useNumberFormat()

onMounted(loadErrors);

const projectDetailsState = useProjDetailsState();
const loading = ref(true);
const errors = ref([]);
const totalRows = ref(null);
const pageSize = ref(5);
const currentPage = ref(1);
const sortOrder = ref(-1);
const sortBy = ref('lastSeen');
const possiblePageSizes = ref([5, 10, 25]);

const formatErrorMsg = (errorType, error) => {
  if (errorType === 'SkillNotFound') {
    return `Reported Skill Id [${error}] does not exist in this Project`;
  }
  return error;
};

const pageChanged = (pagingInfo) => {
  currentPage.value = pagingInfo.page + 1;
  pageSize.value = pagingInfo.rows;
  loadErrors();
};

const sortTable = (sortContext) => {
  sortBy.value = sortContext.sortField;
  sortOrder.value = sortContext.sortOrder;

  // set to the first page
  currentPage.value = 1;
  loadErrors();
};

const removeAllErrors = () => {
  const msg = 'Are you absolutely sure you want to remove all Project issues?';
  dialogMessages.msgConfirm({
    message: msg,
    header: 'Please Confirm!',
    acceptLabel: 'YES, Delete It!',
    rejectLabel: 'Cancel',
    accept: () => {
      loading.value = true;
      ProjectService.deleteAllProjectErrors(route.params.projectId).then(() => {
        loadErrors();
        projectDetailsState.loadProjectDetailsState();
      });
    }
  })
};

const removeError = (projectError) => {
  const msg = `Are you absolutely sure you want to remove issue related to ${projectError.error}?`;
  dialogMessages.msgConfirm({
    message: msg,
    header: 'Please Confirm!',
    acceptLabel: 'YES, Delete It!',
    rejectLabel: 'Cancel',
    accept: () => {
      loading.value = true;
      ProjectService.deleteProjectError(projectError.projectId, projectError.errorId).then(() => {
        loadErrors();
        projectDetailsState.loadProjectDetailsState();
      });
    }
  })
};

function loadErrors() {
  loading.value = true;
  const pageParams = {
    limit: pageSize.value,
    ascending: sortOrder.value === 1,
    page: currentPage.value,
    orderBy: sortBy.value,
  };
  ProjectService.getProjectErrors(route.params.projectId, pageParams).then((res) => {
    errors.value = res.data;
    totalRows.value = res.totalCount;
    projectDetailsState.loadProjectDetailsState();
  }).finally(() => {
    loading.value = false;
  });
}

const responsive = useResponsiveBreakpoints()
const isFlex = computed(() => responsive.md.value)
</script>

<template>
  <div id="projectErrorsPanel">
    <sub-page-header title="Project Issues">
      <div class="row">
        <div class="col">
          <span id="remove-button" class="mr-2">
            <SkillsButton @click="removeAllErrors" :disabled="errors.length < 1" size="small" data-cy="removeAllErrors" id="removeAllErrorsButton" :track-for-focus="true" label="Remove All" icon="fas fa-trash-alt">
            </SkillsButton>
          </span>
        </div>
      </div>
    </sub-page-header>

    <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
      <template #content>
        <SkillsDataTable :busy="loading"
                         :value="errors"
                         tableStoredStateId="projectErrorsTable"
                         aria-label="Project Errors"
                         data-cy="projectErrorsTable" paginator lazy
                         :totalRecords="totalRows"
                         :rows="pageSize"
                         @page="pageChanged"
                         @sort="sortTable"
                         v-model:sort-field="sortBy"
                         v-model:sort-order="sortOrder"
                         :rowsPerPageOptions="possiblePageSizes">
          <Column header="Error" field="errorType" sortable  :class="{'flex': isFlex }">
            <template #body="slotProps">
              <div class="pl-3">
                <div class="mb-2">
                  {{ slotProps.data.errorType }}
                </div>
                <div class="text-sm">
                  {{ formatErrorMsg(slotProps.data.errorType, slotProps.data.error) }}
                </div>
              </div>
            </template>
          </Column>
          <Column header="First Seen" field="created" sortable :class="{'flex': isFlex }">
            <template #body="slotProps">
              <date-cell :value="slotProps.data.created" />
            </template>
          </Column>
          <Column header="Last Seen" field="lastSeen" sortable :class="{'flex': isFlex }">
            <template #body="slotProps">
              <date-cell :value="slotProps.data.lastSeen" />
            </template>
          </Column>
          <Column header="Times Seen" field="count" sortable :class="{'flex': isFlex }"></Column>
          <Column header="Delete" :class="{'flex': isFlex }">
            <template #body="slotProps">
              <SkillsButton :ref="`delete_${slotProps.data.error}`" @click="removeError(slotProps.data)" variant="outline-info" size="small"
                        :data-cy="`deleteErrorButton_${encodeURI(slotProps.data.error)}`"
                        :track-for-focus="true"
                        :id="`deleteErrorButton_${encodeURI(slotProps.data.error)}`"
                        :aria-label="`delete error for reported skill ${slotProps.data.error}`"
                        icon="fas fa-trash-alt" label="Delete">
              </SkillsButton>
            </template>
          </Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy="skillsBTableTotalRows">{{ numberFormat.pretty(totalRows) }}</span>
          </template>

          <template #empty>
            <div class="flex justify-content-center flex-wrap" data-cy="emptyTable">
              <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle"
                 aria-hidden="true"></i>
              <span class="flex align-items-center justify-content-center">There are no records to show
              </span>
            </div>
          </template>
        </SkillsDataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
