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
  <sub-page-header title="Results > Run">
    <b-button :to="{ name: 'QuizRunsHistoryPage' }" variant="outline-primary" size="sm">
      <i class="fas fa-arrow-alt-circle-left" aria-hidden="true"/> Back
    </b-button>
  </sub-page-header>

  <b-card body-class="p-0">
    <div>
      <div class="row px-3 py-3">
        <div class="col-12">
          <b-input v-model="filters.userId" v-on:keyup.enter="applyFilters"
                   data-cy="userNameFilter" aria-label="User Name Filter"/>
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="userFilterBtn"><i
            class="fa fa-filter" aria-hidden="true"/> Filter
          </b-button>
          <b-button ref="filterResetBtn" variant="outline-info" @click="resetFilter" class="ml-1" data-cy="userResetBtn"><i
            class="fa fa-times" aria-hidden="true"/> Reset
          </b-button>
        </div>
      </div>

      <skills-b-table :options="table.options"
                      :items="runsHistory"
                      @page-size-changed="pageSizeChanged"
                      @page-changed="pageChanged"
                      @sort-changed="sortTable"
                      tableStoredStateId="quizRunsHistoryTable"
                      data-cy="quizRunsHistoryTable">
        <template #head(userIdForDisplay)="data">
          <span class="text-primary"><i class="fas fa-user skills-color-users" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(status)="data">
          <span class="text-primary"><i class="fas fa-trophy skills-color-points" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(started)="data">
          <span class="text-primary"><i class="far fa-clock skills-color-events" aria-hidden="true"></i> {{ data.label }}</span>
        </template>

        <template v-slot:cell(userIdForDisplay)="data">
          <div class="row" :data-cy="`row${data.index}-userCell`">
            <div class="col">
              <span v-if="data.item.userIdForDisplayHtml" v-html="data.item.userIdForDisplayHtml"></span><span v-else>{{ data.item.userIdForDisplay }}</span>
            </div>
            <div class="col-auto">
              <b-button variant="outline-info" size="sm">
                <i class="fas fa-list-ul" aria-hidden="true"/><span class="sr-only">view run details</span>
              </b-button>
            </div>
          </div>
        </template>

        <template v-slot:cell(status)="data">
          <div v-if="data.value === 'FAILED'" class="text-danger">
            <i class="far fa-times-circle" aria-hidden="true"></i> Failed
          </div>
          <div v-if="data.value === 'PASSED'" class="text-success">
            <i class="fas fa-check-double" aria-hidden="true"></i> <span v-if="quizType === 'Survey'">Completed</span><span v-else>Passed</span>
          </div>
          <div v-if="data.value === 'INPROGRESS'" class="text-info">
            <i class="fas fa-running" aria-hidden="true"></i> In Progress
          </div>
        </template>

        <template v-slot:cell(runtime)="data">
          {{ quizRuntime(data.item) | duration }}
        </template>

        <template v-slot:cell(started)="data">
          <date-cell :value="data.value" />
        </template>

        <template v-slot:cell(controls)="data">
          <b-button :data-cy="`row${data.index}-deleteBtn`"
                    :ref="`deleteAttempt-${data.item.attemptId}`"
                    @click="initiateDelete(data.item)"
                    variant="outline-danger"
                    size="sm">
            <i class="fas fa-trash" aria-hidden="true"/><span class="sr-only">delete quiz result for {{ data.item.userIdForDisplay}}</span>
          </b-button>
        </template>

      </skills-b-table>
    </div>
  </b-card>

  <removal-validation v-if="deleteQuizRunInfo.showDialog" v-model="deleteQuizRunInfo.showDialog"
                      @do-remove="deleteRun" @hidden="focusOnRefId(`deleteAttempt-${deleteQuizRunInfo.quizRun.attemptId}`)">
    <p>
      This will remove the {{ quizType }} result for <span
      class="text-primary font-weight-bold">{{ deleteQuizRunInfo.quizRun.userIdForDisplay }}</span> user.
    </p>
    <div>
      Deletion <b>cannot</b> be undone and permanently removes all of the underlying user's answers.
    </div>
  </removal-validation>
</div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';
  import QuizService from '@/components/quiz/QuizService';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import dayjs from '@/common-components/DayJsCustomizer';
  import RemovalValidation from '@/components/utils/modal/RemovalValidation';

  const { mapActions } = createNamespacedHelpers('quiz');

  export default {
    name: 'QuizRunsHistoryPage',
    components: {
      DateCell,
      SkillsBTable,
      SubPageHeader,
      RemovalValidation,
    },
    data() {
      return {
        quizId: this.$route.params.quizId,
        quizType: '',
        runsHistory: [],
        filters: {
          userId: '',
        },
        table: {
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'started',
            sortDesc: true,
            fields: [
              {
                key: 'userIdForDisplay',
                label: 'User',
                sortable: true,
              },
              {
                key: 'status',
                label: 'Status',
                sortable: true,
              },
              {
                key: 'runtime',
                label: 'Runtime',
                sortable: false,
              },
              {
                key: 'started',
                label: 'Started',
                sortable: true,
              },
              {
                key: 'controls',
                label: '',
                sortable: false,
                class: 'controls-column',
              },
            ],
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 0,
              pageSize: 10,
              possiblePageSizes: [10, 20, 50],
            },
          },
        },
        deleteQuizRunInfo: {
          showDialog: false,
          quizRun: {},
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      ...mapActions([
        'afterQuizSummaryLoaded',
      ]),
      loadData() {
        this.table.options.busy = true;
        const params = {
          query: this.filters.userId.trim(),
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        return this.afterQuizSummaryLoaded()
          .then((quizSummary) => {
            this.quizType = quizSummary.type;
            return QuizService.getQuizRunsHistory(this.quizId, params)
              .then((res) => {
                let items = res.data;
                if (this.filters.userId && this.filters.userId.trim().length > 0) {
                  items = items.map((item) => {
                    const userIdForDisplayHtml = StringHighlighter.highlight(item.userIdForDisplay, this.filters.userId);
                    return { userIdForDisplayHtml, ...item };
                  });
                }
                this.runsHistory = items;
                this.table.options.pagination.totalRows = res.count;
              })
              .finally(() => {
                this.table.options.busy = false;
              });
          });
      },
      applyFilters() {
        this.table.options.pagination.currentPage = 1;
        this.loadData().then(() => {
          this.$nextTick(() => this.$announcer.polite(`Quiz Run table has been filtered by ${this.filters.userId}`));
        });
      },
      resetFilter() {
        this.filters.userId = '';
        this.table.options.pagination.currentPage = 1;
        this.loadData().then(() => {
          this.$nextTick(() => this.$announcer.polite('Quiz Run table filters have been removed'));
        });
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadData();
      },
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadData();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      quizRuntime(quiz) {
        const { started, completed } = quiz;
        const startDate = dayjs(started);
        const endDate = completed ? dayjs(completed) : dayjs();
        const diff = endDate.diff(startDate);
        return diff;
      },
      focusOnRefId(refId) {
        this.$nextTick(() => {
          const ref = this.$refs[refId];
          if (ref) {
            ref.focus();
          }
        });
      },
      initiateDelete(quizRun) {
        this.deleteQuizRunInfo.quizRun = quizRun;
        this.deleteQuizRunInfo.showDialog = true;
      },
      deleteRun() {
        this.table.options.busy = true;
        QuizService.deleteQuizRunHistoryItem(this.quizId, this.deleteQuizRunInfo.quizRun.attemptId)
          .then(() => {
            this.loadData().then(() => this.focusOnRefId('filterResetBtn'));
          });
      },
    },
  };
</script>

<style>
.controls-column {
  max-width: 2.5rem !important;
}

</style>
