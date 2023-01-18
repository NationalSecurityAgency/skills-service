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
    <div class="row px-3 py-3">
      <div class="col-12">
        <b-input v-model="filter.name" v-on:keyup.enter="applyFilters"
                 data-cy="skillsTable-skillFilter" aria-label="skill name filter"/>
      </div>
    </div>

    <div class="row pl-3 mb-3">
      <div class="col">
        <b-button variant="outline-info" @click="applyFilters" data-cy="users-filterBtn"><i
          class="fa fa-filter"/> Filter
        </b-button>
        <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="users-resetBtn"><i
          class="fa fa-times"/> Reset
        </b-button>
      </div>
    </div>

    <skills-b-table :options="options" :items="quizzes"
                    data-cy="performedSkillsTable">
      <template v-slot:cell(name)="data">
        <div class="row">
          <div class="col">
            <div class="h5">
              <router-link :data-cy="`managesQuizLink_${data.item.quizId}`"
                           :to="{ name:'Questions', params: { quizId: data.item.quizId }}"
                           :aria-label="`Manage Quiz ${data.item.name}`"
                           tag="a">
                {{ data.item.name }}
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
                        variant="outline-primary" :data-cy="`editSkillButton_${data.item.quizId}`"
                        :aria-label="'edit Quiz '+data.item.name" :ref="'edit_'+data.item.quizId"
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

    <edit-quiz v-if="editQuizInfo.showDialog" v-model="editQuizInfo.showDialog"
               :quiz="editQuizInfo.quizDef"
               :is-edit="editQuizInfo.isEdit"
               @quiz-saved="updateQuizDef"
               @hidden="focusOnRefId(`edit_${$event.quizId}`)"/>
    <removal-validation v-if="deleteQuizInfo.showDialog" v-model="deleteQuizInfo.showDialog"
                        @do-remove="deleteQuiz" @hidden="focusOnRefId(`delete_${deleteQuizInfo.quizDef.quizId}`)">
      <p>
        This will remove <span
        class="text-primary font-weight-bold">{{ deleteQuizInfo.quizDef.name }}</span> test.
      </p>
      <div>
        Deletion can not be undone and permanently removes all of the test's underlying configuration
        as well as users' test achievements, stats and metrics.
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

  export default {
    name: 'QuizDefinitions',
    components: {
      RemovalValidation,
      DateCell,
      SkillsBTable,
      EditQuiz,
    },
    data() {
      return {
        filter: {
          name: '',
        },
        quizzes: [],
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
              label: 'Test Name',
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
    methods: {
      applyFilters() {
        this.quizzes = this.quizzes.map((q) => ({ ...q }));
      },
      reset() {
        this.quizzes = this.quizzes.map((q) => ({ ...q }));
      },
      showUpdateModal(quizDef, isEdit = true) {
        this.editQuizInfo.quizDef = quizDef;
        this.editQuizInfo.isEdit = isEdit;
        this.editQuizInfo.showDialog = true;
      },
      updateQuizDef(quizDef) {
        this.options.busy = true;
        const isNewQuizDef = !quizDef.originalQuizId;
        QuizService.updateQuizDef(quizDef)
          .then((updatedQuizDef) => {
            // presence of the originalQuizId indicates edit operation
            if (isNewQuizDef) {
              this.quizzes.push(updatedQuizDef);
            } else {
              this.quizzes = this.quizzes.map((q) => {
                if (q.quizId === quizDef.originalQuizId) {
                  return updatedQuizDef;
                }
                return q;
              });
            }
          })
          .finally(() => {
            this.options.busy = false;
            if (isNewQuizDef) {
              this.$emit('focus-on-new-button');
            } else {
              this.focusOnRefId(`edit_${quizDef.quizId}`);
            }
          });
      },
      loadData() {
        this.options.busy = true;
        QuizService.getQuizDefs()
          .then((res) => {
            this.quizzes = res;
          })
          .finally(() => {
            this.options.busy = false;
          });
      },
      showDeleteWarningModal(quizDef) {
        this.deleteQuizInfo.quizDef = quizDef;
        this.deleteQuizInfo.showDialog = true;
      },
      deleteQuiz() {
        this.options.busy = true;
        const { quizDef } = this.deleteQuizInfo;
        this.deleteQuizInfo.quizDef = {};
        QuizService.deleteQuizId(quizDef.quizId)
          .then(() => {
            this.quizzes = this.quizzes.filter((q) => q.quizId !== quizDef.quizId);
          })
          .finally(() => {
            this.options.busy = false;
            this.$emit('focus-on-new-button');
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
