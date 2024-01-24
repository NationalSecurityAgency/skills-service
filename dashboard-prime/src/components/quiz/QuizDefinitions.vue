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


import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import ColumnGroup from 'primevue/columngroup';   // optional
import Row from 'primevue/row';                   // optional


const emit = defineEmits(['focus-on-new-button'])
const loading = ref(false);
const filter = ref({
  name: '',
});
const quizzes = ref([]);
const quizzesPreFilter = ref([]);
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
const refs = ref({});  // keep track of dynamic refs

const hasData = computed(() => {
  return quizzesPreFilter.value && quizzesPreFilter.value.length > 0;
});

onMounted(() => {
  loadData()
})

function loadData() {
  loading.value = true;
  QuizService.getQuizDefs()
      .then((res) => {
        quizzes.value = res.map((q) => ({ ...q }));
        quizzesPreFilter.value = res.map((q) => ({ ...q }));
        options.value.pagination.totalRows = quizzes.value.length;
      })
      .finally(() => {
        options.value.busy = false;
        loading.value = false;
      });
}
function applyFilters() {
  if (!filter.value.name || filter.value.name.trim() === '') {
    this.reset();
  } else {
    quizzes.value = quizzesPreFilter.value.filter((q) => q.name.toLowerCase()
        .includes(filter.value.name.trim().toLowerCase()))?.map((item) => {
      const nameHtml = StringHighlighter.highlight(item.name, filter.value.name);
      return {
        nameHtml,
        ...item,
      };
    });
  }
}
function reset() {
  filter.value.name = '';
  quizzes.value = quizzesPreFilter.value.map((q) => ({ ...q }));
}
function updateQuizDef(quizDef) {
  if (!this.hasData) {
    loading.value = true;
  }
  options.value.busy = true;
  const isNewQuizDef = !quizDef.originalQuizId;
  QuizService.updateQuizDef(quizDef)
      .then((updatedQuizDef) => {
        // presence of the originalQuizId indicates edit operation
        if (isNewQuizDef) {
          quizzes.value.push(updatedQuizDef);
          quizzesPreFilter.value.push(updatedQuizDef);
          options.value.pagination.totalRows = this.quizzesPreFilter.length;
        } else {
          const replaceUpdated = (q) => {
            if (q.quizId === quizDef.originalQuizId) {
              return updatedQuizDef;
            }
            return q;
          };
          quizzes.value = quizzes.value.map(replaceUpdated);
          quizzesPreFilter.value = quizzesPreFilter.value.map(replaceUpdated);
        }
      })
      .finally(() => {
        options.value.busy = false;
        loading.value = false;
        handleEditQuizModalClose(quizDef);
        nextTick(() => {
          console.log('announce quiz was saved!')
          // this.$announcer.polite(`${quizDef.type} named ${quizDef.name} was saved`);
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
        quizzesPreFilter.value = quizzesPreFilter.value.filter((q) => q.quizId !== quizDef.quizId);
      })
      .finally(() => {
        options.value.busy = false;
        emit('focus-on-new-button');
        nextTick(() => {
          console.log('announce quiz was removed');
          // this.$announcer.polite(`${quizDef.type} named ${quizDef.name} was removed.`);
        });
      });
}
function handleEditQuizModalClose(quizDef) {
  const isNewQuizDef = !quizDef.originalQuizId && !quizDef.isEdit;
  if (isNewQuizDef) {
    emit('focus-on-new-button');
  } else {
    focusOnRefId(`edit_${quizDef.quizId}`);
  }
}
function focusOnRefId(refId) {
  nextTick(() => {
    const ref = this.$refs[refId];
    if (ref) {
      ref.focus();
    }
  });
}
const showUpdateModal = (quizDef, isEdit = true) => {
  console.log('showUpdateModal!', quizDef, isEdit);
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
      <div class="flex mx-3">
        <InputText class="flex flex-grow-1" type="text" v-model="filter.name" v-on:keyup.enter="applyFilters"
                   data-cy="quizNameFilter" aria-label="Quiz/Survey Name Filter"/>
      </div>
      <div class="flex gap-1 m-3">
        <Button outlined
                aria-label="Filter surveys and quizzes table"
                @click="applyFilters"
                data-cy="quizFilterBtn">
          <i class="fa fa-filter" aria-hidden="true"/><span class="ml-1">Filter</span>
        </Button>
        <Button outlined
                @click="reset"
                aria-label="Reset surveys and quizzes filter"
                data-cy="quizResetBtn">
          <i class="fa fa-times" aria-hidden="true"/><span class="ml-1">Reset</span>
        </Button>
      </div>
      <DataTable :value="quizzes" tableStyle="min-width: 50rem"
                 tableStoredStateId="quizDeffinitionsTable"
                 data-cy="quizDeffinitionsTable"
                 show-gridlines
                 striped-rows>
<!--        <Column field="name" sortable>-->
<!--          <template #header>-->
<!--            <span><i class="fas fa-spell-check skills-color-subjects" aria-hidden="true"></i> Custom Name</span>-->
<!--          </template>-->
<!--        </Column>-->
<!--        <Column field="type" sortable>-->
<!--          <template #header>-->
<!--            <span><i class="fas fa-sliders-h text-success" aria-hidden="true"></i> Type</span>-->
<!--          </template>-->
<!--        </Column>-->
<!--        <Column field="created" sortable>-->
<!--          <template #header>-->
<!--            <span><i class="fas fa-clock text-warning" aria-hidden="true"></i> Created On</span>-->
<!--          </template>-->
<!--        </Column>-->

        <Column v-for="col of options.fields" :key="col.key" :field="col.key" :sortable="col.sortable">
          <template #header>
            <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
          </template>
        </Column>
      </DataTable>
    </div>

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