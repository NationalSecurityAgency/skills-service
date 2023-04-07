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
  <div style="min-height: 20rem;">
    <skills-spinner :is-loading="loading" class="my-5"/>
    <no-content2 v-if="!loading && !hasData" title="No Quiz or Survey Definitions"
                 class="mt-5"
                 message="Create a Survey or a Quiz to run independently or to associate to a skill in one of the existing SkillTree projects."
                 data-cy="noQuizzesYet"/>
    <div v-if="!loading && hasData">
      <div class="row px-3 py-3">
        <div class="col-12">
          <b-input v-model="filter.name" v-on:keyup.enter="applyFilters"
                   data-cy="quizNameFilter" aria-label="Quiz/Survey Name Filter"/>
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info"
                    aria-label="Filter surveys and quizzes table"
                    @click="applyFilters"
                    data-cy="quizFilterBtn"><i
            class="fa fa-filter" aria-hidden="true"/> Filter
          </b-button>
          <b-button variant="outline-info"
                    @click="reset"
                    aria-label="Reset surveys and quizzes filter"
                    class="ml-1"
                    data-cy="quizResetBtn"><i
            class="fa fa-times" aria-hidden="true"/> Reset
          </b-button>
        </div>
      </div>

      <skills-b-table :options="options" :items="quizzes"
                      tableStoredStateId="quizDeffinitionsTable"
                      data-cy="quizDeffinitionsTable">
        <template #head(name)="data">
          <span class="text-primary"><i class="fas fa-spell-check skills-color-subjects" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(type)="data">
          <span class="text-primary"><i class="fas fa-sliders-h text-success" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(created)="data">
          <span class="text-primary"><i class="fas fa-clock text-warning" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template v-slot:cell(name)="data">
          <div class="row">
            <div class="col">
              <div class="h5">
                <router-link :data-cy="`managesQuizLink_${data.item.quizId}`"
                             :to="{ name:'Questions', params: { quizId: data.item.quizId }}"
                             :aria-label="`Manage Quiz ${data.item.name}`"
                             tag="a">
                  <span v-html="data.item.nameHtml ? data.item.nameHtml : data.item.name" />
                </router-link>
              </div>
            </div>
            <div class="col-auto text-right">
              <router-link :data-cy="`managesQuizBtn_${data.item.quizId}`"
                           :to="{ name:'Questions', params: { quizId: data.item.quizId }}"
                           :aria-label="`Manage Quiz ${data.item.name}`"
                           class="btn btn-outline-primary btn-sm">
                <span class="d-none d-sm-inline">Manage </span> <i class="fas fa-arrow-circle-right"
                                                                   aria-hidden="true"/>
              </router-link>
              <b-button-group size="sm" class="ml-1">
                <b-button @click="showUpdateModal(data.item)"
                          variant="outline-primary" :data-cy="`editQuizButton_${data.item.quizId}`"
                          :aria-label="`Edit Quiz ${data.item.name}`" :ref="`edit_${data.item.quizId}`"
                          title="Edit Quiz">
                  <i class="fas fa-edit" aria-hidden="true"/>
                </b-button>
                <b-button @click="showDeleteWarningModal(data.item)" variant="outline-primary"
                          :data-cy="`deleteQuizButton_${data.item.quizId}`"
                          :aria-label="'delete Quiz '+data.item.name"
                          :ref="`delete_${data.item.quizId}`"
                          title="Delete Quiz">
                  <i class="text-warning fas fa-trash" aria-hidden="true"/>
                </b-button>
              </b-button-group>
            </div>
          </div>
        </template>
        <template v-slot:cell(created)="data">
          <date-cell :value="data.value"/>
        </template>
      </skills-b-table>
    </div>
      <edit-quiz v-if="editQuizInfo.showDialog" v-model="editQuizInfo.showDialog"
                 :quiz="editQuizInfo.quizDef"
                 :is-edit="editQuizInfo.isEdit"
                 @quiz-saved="updateQuizDef"
                 @hidden="handleEditQuizModalClose"/>
      <removal-validation v-if="deleteQuizInfo.showDialog" v-model="deleteQuizInfo.showDialog"
                          :removal-not-available="deleteQuizInfo.disableDelete"
                          @do-remove="deleteQuiz" @hidden="focusOnRefId(`delete_${deleteQuizInfo.quizDef.quizId}`)">
        <skills-spinner :is-loading="deleteQuizInfo.loadingDeleteCheck" class="my-4"/>
        <div v-if="!deleteQuizInfo.loadingDeleteCheck">
          <div v-if="deleteQuizInfo.disableDelete">
            Cannot remove the quiz since it is currently assigned to <b-badge>{{ deleteQuizInfo.numSkillsAssignedTo }}</b-badge> skill{{ deleteQuizInfo.numSkillsAssignedTo > 1 ? 's' : ''}}.
          </div>
          <div v-if="!deleteQuizInfo.disableDelete">
            <p>
              This will remove <span
              class="text-primary font-weight-bold">{{ deleteQuizInfo.quizDef.name }}</span> {{ deleteQuizInfo.quizDef.type }}.
            </p>
            <div>
              Deletion <b>cannot</b> be undone and permanently removes all of the underlying questions
              as well as users' achievements, stats and metrics. Proceed with caution!
            </div>
          </div>
        </div>
      </removal-validation>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import QuizService from '@/components/quiz/QuizService';
  import RemovalValidation from '@/components/utils/modal/RemovalValidation';
  import EditQuiz from '@/components/quiz/testCreation/EditQuiz';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import NoContent2 from '@/components/utils/NoContent2';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';

  export default {
    name: 'QuizDefinitions',
    components: {
      NoContent2,
      SkillsSpinner,
      RemovalValidation,
      DateCell,
      SkillsBTable,
      EditQuiz,
    },
    data() {
      return {
        loading: true,
        filter: {
          name: '',
        },
        quizzes: [],
        quizzesPreFilter: [],
        options: {
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
        },
        deleteQuizInfo: {
          showDialog: false,
          quizDef: {},
          disableDelete: true,
          numSkillsAssignedTo: 0,
          loadingDeleteCheck: true,
        },
        editQuizInfo: {
          showDialog: false,
          isEdit: false,
          quizDef: {},
        },
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      hasData() {
        return this.quizzesPreFilter && this.quizzesPreFilter.length > 0;
      },
    },
    methods: {
      applyFilters() {
        if (!this.filter.name || this.filter.name.trim() === '') {
          this.reset();
        } else {
          this.quizzes = this.quizzesPreFilter.filter((q) => q.name.toLowerCase()
            .indexOf(this.filter.name.trim().toLowerCase()) > 0)?.map((item) => {
            const nameHtml = StringHighlighter.highlight(item.name, this.filter.name);
            return {
              nameHtml,
              ...item,
            };
          });
        }
      },
      reset() {
        this.filter.name = '';
        this.quizzes = this.quizzesPreFilter.map((q) => ({ ...q }));
      },
      showUpdateModal(quizDef, isEdit = true) {
        this.editQuizInfo.quizDef = quizDef;
        this.editQuizInfo.isEdit = isEdit;
        this.editQuizInfo.showDialog = true;
      },
      updateQuizDef(quizDef) {
        if (!this.hasData) {
          this.loading = true;
        }
        this.options.busy = true;
        const isNewQuizDef = !quizDef.originalQuizId;
        QuizService.updateQuizDef(quizDef)
          .then((updatedQuizDef) => {
            // presence of the originalQuizId indicates edit operation
            if (isNewQuizDef) {
              this.quizzes.push(updatedQuizDef);
              this.quizzesPreFilter.push(updatedQuizDef);
              this.options.pagination.totalRows = this.quizzesPreFilter.length;
            } else {
              const replaceUpdated = (q) => {
                if (q.quizId === quizDef.originalQuizId) {
                  return updatedQuizDef;
                }
                return q;
              };
              this.quizzes = this.quizzes.map(replaceUpdated);
              this.quizzesPreFilter = this.quizzesPreFilter.map(replaceUpdated);
            }
          })
          .finally(() => {
            this.options.busy = false;
            this.loading = false;
            this.handleEditQuizModalClose(quizDef);
            this.$nextTick(() => {
              this.$announcer.polite(`${quizDef.type} named ${quizDef.name} was saved`);
            });
          });
      },
      handleEditQuizModalClose(quizDef) {
        const isNewQuizDef = !quizDef.originalQuizId && !quizDef.isEdit;
        if (isNewQuizDef) {
          this.$emit('focus-on-new-button');
        } else {
          this.focusOnRefId(`edit_${quizDef.quizId}`);
        }
      },
      loadData() {
        this.loading = true;
        QuizService.getQuizDefs()
          .then((res) => {
            this.quizzes = res.map((q) => ({ ...q }));
            this.quizzesPreFilter = res.map((q) => ({ ...q }));
            this.options.pagination.totalRows = this.quizzes.length;
          })
          .finally(() => {
            this.options.busy = false;
            this.loading = false;
          });
      },
      showDeleteWarningModal(quizDef) {
        this.deleteQuizInfo.quizDef = quizDef;
        this.deleteQuizInfo.showDialog = true;
        this.deleteQuizInfo.loadingDeleteCheck = true;
        this.deleteQuizInfo.disableDelete = true;
        QuizService.countNumSkillsQuizAssignedTo(quizDef.quizId)
          .then((res) => {
            this.deleteQuizInfo.numSkillsAssignedTo = res;
            this.deleteQuizInfo.disableDelete = res > 0;
            this.deleteQuizInfo.loadingDeleteCheck = false;
          });
      },
      deleteQuiz() {
        this.options.busy = true;
        const { quizDef } = this.deleteQuizInfo;
        this.deleteQuizInfo.quizDef = {};
        QuizService.deleteQuizId(quizDef.quizId)
          .then(() => {
            this.quizzes = this.quizzes.filter((q) => q.quizId !== quizDef.quizId);
            this.quizzesPreFilter = this.quizzesPreFilter.filter((q) => q.quizId !== quizDef.quizId);
          })
          .finally(() => {
            this.options.busy = false;
            this.$emit('focus-on-new-button');
            this.$nextTick(() => {
              this.$announcer.polite(`${quizDef.type} named ${quizDef.name} was removed.`);
            });
          });
      },
      focusOnRefId(refId) {
        this.$nextTick(() => {
          const ref = this.$refs[refId];
          if (ref) {
            ref.focus();
          }
        });
      },
    },
  };

</script>

<style scoped>

</style>
