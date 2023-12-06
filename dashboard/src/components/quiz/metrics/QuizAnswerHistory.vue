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
          <span class="text-primary" data-cy="usrColumnHeader"><i class="fas fa-user skills-color-users" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(updated)="data">
          <span class="text-primary"><i class="far fa-clock skills-color-events" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template v-slot:cell(answerTxt)="data">
          <div :data-cy="`row${data.index}-colAnswerTxt`">
            <pre v-if="data.item.truncated" data-cy="textTruncated">{{ data.value | truncate(answerTxtTruncate.truncateTo) }}</pre>
            <pre v-else data-cy="text">{{ data.value }}</pre>
            <div class="text-right">
              <b-button v-if="data.item.truncationEnabled"
                      size="sm"
                      variant="outline-info"
                      data-cy="expandCollapseTextBtn"
                      @click="data.item.truncated = !data.item.truncated">
                <span v-if="!data.item.truncated"><i class="fas fa-compress-arrows-alt" aria-hidden="true"></i> Collapse</span>
                <span v-else><i class="fas fa-expand-arrows-alt" aria-hidden="true"></i> Expand Text</span>
              </b-button>
            </div>
          </div>
        </template>
        <template v-slot:cell(userIdForDisplay)="data">
          <div class="row" style="min-width: 15rem;" :data-cy="`row${data.index}-colUserId`">
            <div class="col mb-2">
              {{ getUserDisplay(data.item, true) }}
            </div>
            <div class="col-auto mb-2">
              <b-button size="sm" variant="outline-info"
                        target="_blank"
                        :aria-label="`View quiz attempt for ${data.item.userQuizAttemptId} id`"
                        :to="{ name: 'QuizSingleRunPage', params: { runId: data.item.userQuizAttemptId } }"
                        data-cy="viewRunBtn">
                <i class="fas fa-eye" aria-hidden="true"></i> View Run
              </b-button>
            </div>
          </div>
        </template>
        <template v-slot:cell(userTag)="data">
          <span :data-cy="`row${data.index}-userTag`">{{ data.value }}</span>
        </template>
        <template v-slot:cell(updated)="data">
          <div style="min-width: 13rem;">
            <date-cell :value="data.value"/>
          </div>
        </template>
      </skills-b-table>
  </div>
</template>

<script>
  import QuizService from '@/components/quiz/QuizService';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import UserTagsConfigMixin from '@/components/users/UserTagsConfigMixin';
  import UserIdForDisplayMixin from '../../users/UserIdForDisplayMixin';

  export default {
    name: 'QuizAnswerHistory',
    mixins: [UserTagsConfigMixin, UserIdForDisplayMixin],
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
        answerHistory: [],
        answerTxtTruncate: {
          truncateThreshold: 600,
          truncateTo: 550,
        },
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
      if (this.showUserTagColumn) {
        fields.push({
          key: 'userTag',
          label: this.userTagLabel,
          sortable: true,
        });
      }
      fields.push({
        key: 'updated',
        label: 'Date',
        sortable: true,
      });
      this.tableOptions.fields = fields;
      this.loadData();
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
            this.answerHistory = res.data.map((item) => {
              const toTruncate = item.answerTxt && item.answerTxt.length >= this.answerTxtTruncate.truncateThreshold;
              return ({
                ...item,
                truncated: toTruncate,
                truncationEnabled: toTruncate,
              });
            });
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
pre {
  overflow-x: auto;
  white-space: pre-wrap;
  white-space: -moz-pre-wrap;
  white-space: -pre-wrap;
  white-space: -o-pre-wrap;
  word-wrap: break-word;
}
</style>
