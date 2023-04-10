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
<template>
  <div>
    <div class="text-center">
      <b-spinner v-if="isLoading" label="Loading..." style="width: 2rem; height: 2rem;" variant="info"/>
    </div>
    <div v-if="!isLoading">
      <skills-b-table :options="tableOptions"
                      :items="answerHistory"
                      @page-size-changed="pageSizeChanged"
                      @page-changed="pageChanged"
                      @sort-changed="sortTable"
                      tableStoredStateId="quizAnswerHistoryTable"
                      data-cy="quizAnswerHistoryTable">
        <template #head(answerTxt)="data">
          <span class="text-primary"><i class="fas fa-check-double skills-color-projects" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(userIdForDisplay)="data">
          <span class="text-primary"><i class="fas fa-user skills-color-users" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(updated)="data">
          <span class="text-primary"><i class="far fa-clock skills-color-events" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template v-slot:cell(userIdForDisplay)="data">
          <div class="row">
            <div class="col">
              {{ data.value }}
            </div>
            <div class="col-auto">
              <div class="col-auto">
                <b-button size="sm" variant="outline-info"
                          target="_blank"  :to="{ name: 'QuizSingleRunPage', params: { runId: data.item.userQuizAttemptId } }"><i class="fas fa-tasks"></i> View Full Run</b-button>
              </div>
            </div>
          </div>
        </template>
        <template v-slot:cell(updated)="data">
          <date-cell :value="data.value"/>
        </template>
      </skills-b-table>
    </div>
  </div>
</template>

<script>
  import QuizService from '@/components/quiz/QuizService';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';

  export default {
    name: 'QuizAnswerHistory',
    components: { DateCell, SkillsBTable },
    props: {
      answerDefId: Number,
      questionType: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        quizId: this.$route.params.quizId,
        isLoading: true,
        answerHistory: [],
        tableOptions: {
          busy: false,
          bordered: true,
          outlined: true,
          stacked: 'md',
          sortBy: 'updated',
          sortDesc: true,
          fields: [],
          pagination: {
            server: true,
            currentPage: 1,
            totalRows: 0,
            pageSize: 5,
            possiblePageSizes: [5, 10, 15, 20],
          },
        },
      };
    },
    computed: {
      isTextInput() {
        return this.questionType === 'TextInput';
      },
    },
    mounted() {
      const fields = [];
      if (this.isTextInput) {
        fields.push({
          key: 'answerTxt',
          label: 'Answer',
          sortable: false,
        });
      }
      fields.push({
        key: 'userIdForDisplay',
        label: 'User',
        sortable: true,
      });
      fields.push({
        key: 'updated',
        label: 'Date',
        sortable: true,
      });
      this.tableOptions.fields = fields;
      this.loadData().then(() => {
        this.isLoading = false;
      });
    },
    methods: {
      loadData() {
        this.tableOptions.busy = true;
        const params = {
          limit: this.tableOptions.pagination.pageSize,
          ascending: !this.tableOptions.sortDesc,
          page: this.tableOptions.pagination.currentPage,
          orderBy: this.tableOptions.sortBy,
        };
        return QuizService.getQuizAnswerSelectionHistory(this.quizId, this.answerDefId, params)
          .then((res) => {
            this.answerHistory = res.data;
            this.tableOptions.pagination.totalRows = res.count;
            this.tableOptions.pagination.hideUnnecessary = res.totalCount <= this.tableOptions.pagination.pageSize;
          })
          .finally(() => {
            this.tableOptions.busy = false;
          });
      },
      pageSizeChanged(newSize) {
        this.tableOptions.pagination.pageSize = newSize;
        this.loadData();
      },
      pageChanged(pageNum) {
        this.tableOptions.pagination.currentPage = pageNum;
        this.loadData();
      },
      sortTable(sortContext) {
        this.tableOptions.sortBy = sortContext.sortBy;
        this.tableOptions.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.tableOptions.pagination.currentPage = 1;
        this.loadData();
      },
    },
  };
</script>

<style scoped>

</style>
