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
import {ref, onMounted, computed} from 'vue'
import QuizService from "@/components/quiz/QuizService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";

const loading = ref(false);
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
    },
    {
      key: 'type',
      label: 'Type',
      sortable: true,
    },
    {
      key: 'created',
      label: 'Created On',
      sortable: true,
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

onMounted(() => {
  loadData()
})
const hasData = computed(() => {
  return quizzesPreFilter.value && quizzesPreFilter.value.length > 0;
});

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
const showUpdateModal = (quizDef, isEdit = true) => {
  // this.editQuizInfo.quizDef = quizDef;
  // this.editQuizInfo.isEdit = isEdit;
  // this.editQuizInfo.showDialog = true;
  console.log('showUpdateModal!', quizDef, isEdit);
}

defineExpose({
  showUpdateModal,
})
</script>

<template>
  <div>
    <SkillsSpinner :is-loading="loading" class="my-5" />
    <NoContent2 v-if="!loading && !hasData"
                title="No Quiz or Survey Definitions"
                class="mt-5"
                message="Create a Survey or a Quiz to run independently or to associate to a skill in one of the existing SkillTree projects."
                data-cy="noQuizzesYet"/>
  </div>
</template>

<style scoped>

</style>