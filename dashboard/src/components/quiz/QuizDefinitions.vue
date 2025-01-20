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
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js';
import { FilterMatchMode } from 'primevue/api'
import QuizService from '@/components/quiz/QuizService.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import Column from 'primevue/column'
import DateCell from '@/components/utils/table/DateCell.vue'
import EditQuiz from '@/components/quiz/testCreation/EditQuiz.vue'
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'
import Avatar from 'primevue/avatar';
import { useQuizSummaryState } from '@/stores/UseQuizSummaryState.js';

const announcer = useSkillsAnnouncer()
const responsive = useResponsiveBreakpoints()
const communityLabels = useCommunityLabels()
const quizSummaryState = useQuizSummaryState()

const loading = ref(false);
const quizzes = ref([]);
const sortInfo = ref({ sortOrder: 1, sortBy: 'created' })
const options = ref({
  emptyText: 'Click Test+ on the top-right to create a test!',
  busy: false,
  bordered: true,
  outlined: true,
  stacked: 'md',
  fields: [
    {
      key: 'name',
      label: 'Name',
      sortable: true,
      imageClass: 'fas fa-spell-check skills-color-subjects',
    },
    {
      key: 'type',
      label: 'Type',
      sortable: true,
      imageClass: 'fas fa-sliders-h text-success',
    },
    {
      key: 'created',
      label: 'Created On',
      sortable: true,
      imageClass: 'fas fa-clock text-warning',
    },
  ],
  pagination: {
    server: false,
    currentPage: 1,
    totalRows: 0,
    pageSize: 5,
    possiblePageSizes: [5, 10, 15, 20],
  },
});
const deleteQuizInfo = ref( {
  showDialog: false,
      quizDef: {},
  disableDelete: true,
      numSkillsAssignedTo: 0,
      loadingDeleteCheck: true,
});
const editQuizInfo = ref({
  showDialog: false,
      isEdit: false,
      quizDef: {},
});

const totalRows = ref(0)

const hasData = computed(() => {
  return quizzes.value && quizzes.value.length > 0;
});

onMounted(() => {
  quizSummaryState.quizSummary = null
  loadData()
})

function loadData() {
  loading.value = true;
  QuizService.getQuizDefs()
      .then((res) => {
        quizzes.value = res.map((q) => ({ ...q }));
        options.value.pagination.totalRows = quizzes.value.length;
        totalRows.value = quizzes.value.length;
      })
      .finally(() => {
        options.value.busy = false;
        loading.value = false;
      });
}

const filters = ref({
  global: {value: null, matchMode: FilterMatchMode.CONTAINS},
})

const clearFilter = () => {
  filters.value.global.value = null
}
const onFilter = (filterEvent) => {
  totalRows.value = filterEvent.filteredValue.length
}
function updateQuizDef(quizDef) {
  const existingIndex = quizzes.value.findIndex((item) => item.quizId === quizDef.originalQuizId)
  if (existingIndex >= 0) {
    quizzes.value.splice(existingIndex, 1, quizDef)
  } else {
    quizzes.value.push(quizDef)
  }
  announcer.polite(`${quizDef.type} named ${quizDef.name} was saved`);
}
function showDeleteWarningModal(quizDef) {
  deleteQuizInfo.value.quizDef = quizDef;
  deleteQuizInfo.value.showDialog = true;
  deleteQuizInfo.value.loadingDeleteCheck = true;
  deleteQuizInfo.value.disableDelete = true;
  QuizService.countNumSkillsQuizAssignedTo(quizDef.quizId)
      .then((res) => {
        deleteQuizInfo.value.numSkillsAssignedTo = res;
        deleteQuizInfo.value.disableDelete = res > 0;
        deleteQuizInfo.value.loadingDeleteCheck = false;
      });
}
function deleteQuiz() {
  options.value.busy = true;
  const { quizDef } = deleteQuizInfo.value;
  deleteQuizInfo.value.quizDef = {};
  QuizService.deleteQuizId(quizDef.quizId)
      .then(() => {
        quizzes.value = quizzes.value.filter((q) => q.quizId !== quizDef.quizId);
      })
      .finally(() => {
        options.value.busy = false;
        announcer.polite(`${quizDef.type} named ${quizDef.name} was removed.`);
      });
}
const showUpdateModal = (quizDef, isEdit = true) => {
  editQuizInfo.value.quizDef = quizDef;
  editQuizInfo.value.isEdit = isEdit;
  editQuizInfo.value.isCopy = false;
  editQuizInfo.value.showDialog = true;
};
const showCopyModal = (quizDef, isCopy = true) => {
  editQuizInfo.value.quizDef = quizDef;
  editQuizInfo.value.isCopy = isCopy;
  editQuizInfo.value.isEdit = false;
  editQuizInfo.value.showDialog = true;
};

defineExpose({
  showUpdateModal,
})
</script>

<template>
  <div style="min-height: 20rem;">
    <SkillsSpinner :is-loading="loading" class="my-5" />
    <NoContent2 v-if="!loading && !hasData"
                title="No Quiz or Survey Definitions"
                class="py-8 px-4"
                message="Create a Survey or a Quiz to run independently or to associate to a skill in one of the existing SkillTree projects."
                data-cy="noQuizzesYet"/>
    <div v-if="!loading && hasData">
      <SkillsDataTable
        tableStoredStateId="quizDefinitionsTable"
        aria-label="Quizzes and Surveys"
        :value="quizzes"
        data-cy="quizDefinitionsTable"
        v-model:filters="filters"
        :globalFilterFields="['name']"
        @filter="onFilter"
        v-model:sort-field="sortInfo.sortBy"
        v-model:sort-order="sortInfo.sortOrder"
        paginator :rows="5" :rowsPerPageOptions="[5, 10, 15, 20]"
        show-gridlines
        striped-rows>
        <template #header>
          <div class="flex gap-1">
            <InputGroup>
              <InputGroupAddon>
                <i class="fas fa-search" aria-hidden="true"/>
              </InputGroupAddon>
              <InputText class="flex flex-grow-1"
                         v-model="filters['global'].value"
                         data-cy="quizNameFilter"
                         placeholder="Quiz/Survey Search"
                         aria-label="Quiz/Survey Name Filter"/>
              <InputGroupAddon class="p-0 m-0">
                <SkillsButton
                              icon="fa fa-times"
                              text
                              outlined
                              @click="clearFilter"
                              aria-label="Reset surveys and quizzes filter"
                              data-cy="quizResetBtn"/>
              </InputGroupAddon>
            </InputGroup>
          </div>
        </template>

        <template #paginatorstart>
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
        </template>

        <template #empty>
          <div class="flex justify-content-center flex-wrap">
            <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle" aria-hidden="true"></i>
            <span class="flex align-items-center justify-content-center">No Quiz or Survey Definitions.  Click
            <SkillsButton class="flex flex align-items-center justify-content-center px-1"
                          label="Reset"
                          link
                          size="small"
                          @click="clearFilter"
                          aria-label="Reset surveys and quizzes filter"
                          data-cy="quizResetBtn"/> to clear the existing filter.
              </span>
          </div>
        </template>
        <Column v-for="col of options.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
                :class="{'flex': responsive.md.value }">
          <template #header>
            <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
          </template>
          <template #body="slotProps">
            <div v-if="slotProps.field === 'name'" class="flex w-full flex-wrap flex-column sm:flex-row gap-2">
              <div class="flex align-items-start justify-content-start w-min-10rem">
                <div>
                  <router-link :data-cy="`managesQuizLink_${slotProps.data.quizId}`"
                               :to="{ name:'Questions', params: { quizId: slotProps.data.quizId }}"
                               :aria-label="`Manage Quiz ${slotProps.data.name}`">
                    <highlighted-value :value="slotProps.data.name" :filter="filters.global.value" />
                  </router-link>
                  <div v-if="slotProps.data.userCommunity" class="my-2" data-cy="userCommunity">
                    <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
                    <span class="text-color-secondary font-italic ml-1">{{ communityLabels.beforeCommunityLabel.value }}</span> <span
                      class="font-bold text-primary">{{ slotProps.data.userCommunity }}</span> <span
                      class="text-color-secondary font-italic">{{ communityLabels.afterCommunityLabel.value }}</span>
                  </div>
                </div>
              </div>
              <div class="flex flex-1 flex-wrap align-items-start justify-content-end gap-2">
                <router-link :data-cy="`managesQuizBtn_${slotProps.data.quizId}`"
                             :to="{ name:'Questions', params: { quizId: slotProps.data.quizId }}"
                             :aria-label="`Manage Quiz ${slotProps.data.name}`" tabindex="-1">
                  <SkillsButton label="Manage"
                                icon="fas fa-arrow-circle-right"
                                  class="flex-shrink-1"
                                outlined
                                size="small"/>
                </router-link>
                <ButtonGroup class="flex flex-nowrap">
                  <SkillsButton @click="showUpdateModal(slotProps.data)"
                                icon="fas fa-edit"
                                outlined
                                :data-cy="`editQuizButton_${slotProps.data.quizId}`"
                                :aria-label="`Edit Quiz ${slotProps.data.name}`"
                                :ref="`edit_${slotProps.data.quizId}`"
                                :id="`edit_${slotProps.data.quizId}`"
                                :track-for-focus="true"
                                title="Edit Quiz">
                  </SkillsButton>
                  <SkillsButton @click="showCopyModal(slotProps.data)"
                                icon="fas fa-copy"
                                outlined
                                :data-cy="`copyQuizButton_${slotProps.data.quizId}`"
                                :aria-label="`Copy Quiz ${slotProps.data.name}`"
                                :ref="`copy_${slotProps.data.quizId}`"
                                :id="`copy_${slotProps.data.quizId}`"
                                :track-for-focus="true"
                                title="Copy Quiz">
                  </SkillsButton>
                  <SkillsButton @click="showDeleteWarningModal(slotProps.data)"
                                icon="text-warning fas fa-trash"
                                outlined
                                :data-cy="`deleteQuizButton_${slotProps.data.quizId}`"
                                :aria-label="'delete Quiz '+slotProps.data.name"
                                :ref="`delete_${slotProps.data.quizId}`"
                                :id="`delete_${slotProps.data.quizId}`"
                                :track-for-focus="true"
                                title="Delete Quiz">
                  </SkillsButton>
                </ButtonGroup>
              </div>
            </div>
            <div v-else-if="slotProps.field === 'type'">
              <highlighted-value :value="slotProps.data[col.key]" :filter="filters.global.value" />
            </div>
            <div v-else-if="slotProps.field === 'created'">
              <DateCell :value="slotProps.data[col.key]" />
            </div>
            <div v-else>
              {{ slotProps.data[col.key] }}
            </div>
          </template>
        </Column>
      </SkillsDataTable>
    </div>

    <edit-quiz
        v-if="editQuizInfo.showDialog"
        v-model="editQuizInfo.showDialog"
        :quiz="editQuizInfo.quizDef"
        :is-edit="editQuizInfo.isEdit"
        :is-copy="editQuizInfo.isCopy"
        @quiz-saved="updateQuizDef"
        :enable-return-focus="true"/>

    <removal-validation
      v-if="deleteQuizInfo.showDialog"
      :item-name="deleteQuizInfo.quizDef.name"
      :item-type="deleteQuizInfo.quizDef.type"
      v-model="deleteQuizInfo.showDialog"
      :loading="deleteQuizInfo.loadingDeleteCheck"
      :removal-not-available="deleteQuizInfo.disableDelete"
      :enable-return-focus="true"
      @do-remove="deleteQuiz">
        <div v-if="deleteQuizInfo.disableDelete">
          Cannot remove the quiz since it is currently assigned to <Tag>{{ deleteQuizInfo.numSkillsAssignedTo }}</Tag> skill{{ deleteQuizInfo.numSkillsAssignedTo > 1 ? 's' : ''}}.
        </div>
        <div v-if="!deleteQuizInfo.disableDelete">
            Deletion <b>cannot</b> be undone and permanently removes all of the underlying questions
            as well as users' achievements, stats and metrics. Proceed with caution!
        </div>
    </removal-validation>

  </div>
</template>

<style scoped>

.skills-color-subjects {
  color: #2a9d8fff;
}
.text-success {
  color: #007c49;
}
.text-warning {
  color: #ffc42b;
}

</style>