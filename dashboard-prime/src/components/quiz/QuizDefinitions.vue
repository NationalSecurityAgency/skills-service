/*
Copyright 2020 SkillTree

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
import {ref, onMounted, computed, nextTick} from 'vue'
import QuizService from "@/components/quiz/QuizService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import Column from 'primevue/column';
import { FilterMatchMode } from 'primevue/api';
import DateCell from "@/components/utils/table/DateCell.vue";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import EditQuiz from "@/components/quiz/testCreation/EditQuiz.vue";
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'

const announcer = useSkillsAnnouncer()
const loading = ref(false);
const quizzes = ref([]);
const options = ref({
  emptyText: 'Click Test+ on the top-right to create a test!',
  busy: false,
  bordered: true,
  outlined: true,
  stacked: 'md',
  sortBy: 'created',
  sortDesc: false,
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
  if (!hasData) {
    loading.value = true;
  }
  options.value.busy = true;
  const isNewQuizDef = !quizDef.originalQuizId;
  QuizService.updateQuizDef(quizDef)
      .then((updatedQuizDef) => {
        // presence of the originalQuizId indicates edit operation
        if (isNewQuizDef) {
          quizzes.value.push(updatedQuizDef);
        } else {
          const replaceUpdated = (q) => {
            if (q.quizId === quizDef.originalQuizId) {
              return updatedQuizDef;
            }
            return q;
          };
          quizzes.value = quizzes.value.map(replaceUpdated);
        }
      })
      .finally(() => {
        options.value.busy = false;
        loading.value = false;
        nextTick(() => {
          announcer.polite(`${quizDef.type} named ${quizDef.name} was saved`);
        });
      });
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
        nextTick(() => {
          announcer.polite(`${quizDef.type} named ${quizDef.name} was removed.`);
        });
      });
}
const showUpdateModal = (quizDef, isEdit = true) => {
  editQuizInfo.value.quizDef = quizDef;
  editQuizInfo.value.isEdit = isEdit;
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
                class="mt-5"
                message="Create a Survey or a Quiz to run independently or to associate to a skill in one of the existing SkillTree projects."
                data-cy="noQuizzesYet"/>
    <div v-if="!loading && hasData">
      <DataTable :value="quizzes" tableStyle="min-width: 50rem"
                 data-cy="quizDefinitionsTable"
                 stateStorage="local" stateKey="quizDefinitionsTable"
                 v-model:filters="filters"
                 :globalFilterFields="['name']"
                 @filter="onFilter"
                 :sort-field="options.sortBy"
                 :sort-order="options.sortDesc ? -1 : 1"
                 paginator :rows="5" :rowsPerPageOptions="[5, 10, 15, 20]"
                 show-gridlines
                 striped-rows>
        <template #header>
          <div class="flex gap-1">
            <span class="p-input-icon-left flex flex-grow-1">
              <i class="pi pi-search"/>
              <InputText class="flex flex-grow-1"
                         v-model="filters['global'].value"
                         data-cy="quizNameFilter"
                         placeholder="Quiz/Survey Search"/>
            </span>
            <SkillsButton class="flex flex-grow-none"
                          label="Reset"
                          icon="fa fa-times"
                          outlined
                          @click="clearFilter"
                          aria-label="Reset surveys and quizzes filter"
                          data-cy="quizResetBtn"/>
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
        <Column v-for="col of options.fields" :key="col.key" :field="col.key" :sortable="col.sortable">
          <template #header>
            <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
          </template>
          <template #body="slotProps">
            <div v-if="slotProps.field == 'name'" class="flex flex-row flex-wrap">
                <div class="flex align-items-start justify-content-start">
                  <router-link :data-cy="`managesQuizLink_${slotProps.data.quizId}`"
                               :to="{ name:'Questions', params: { quizId: slotProps.data.quizId }}"
                               :aria-label="`Manage Quiz ${slotProps.data.name}`"
                               tag="a">
                    <highlighted-value :value="slotProps.data.name" :filter="filters.global.value" />
                  </router-link>
                </div>
                <div class="flex flex-grow-1 align-items-start justify-content-end">
                  <router-link :data-cy="`managesQuizBtn_${slotProps.data.quizId}`"
                               :to="{ name:'Questions', params: { quizId: slotProps.data.quizId }}"
                               :aria-label="`Manage Quiz ${slotProps.data.name}`">
                    <SkillsButton label="Manage"
                                  icon="fas fa-arrow-circle-right"
                                  class="flex-shrink-1"
                                  outlined
                                  size="small"/>
                  </router-link>
                  <span class="p-buttonset ml-1">
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
                  </span>
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
      </DataTable>
    </div>

    <edit-quiz
        v-if="editQuizInfo.showDialog"
        v-model="editQuizInfo.showDialog"
        :quiz="editQuizInfo.quizDef"
        :is-edit="editQuizInfo.isEdit"
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