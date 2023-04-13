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
    <sub-page-header title="Associated Skills"/>

    <b-card body-class="p-0">
      <div v-if="skills.length === 0" class="alert alert-info">
        <i class="fas fa-exclamation-circle"/> There are currently no skills associated with this quiz/survey.
        You can learn more about how to add skills to a quiz/survey in the documentation
        <a aria-label="SkillTree documentation of associating skills to quizzes"
           :href="docsUrl" target="_blank" style="display: inline-block">
          here.
        </a>
      </div>
      <loading-container v-bind:is-loading="table.options.busy">

        <div class="row px-3 pt-3">
          <div class="col-12">
            <b-form-group label="Skill Filter" label-class="text-muted">
              <b-input v-model="filter.skillName" v-on:keydown.enter="applyFilters" data-cy="quiz-skillNameFilter" aria-label="Skill name filter"/>
            </b-form-group>
          </div>
          <div class="col-md">
          </div>
        </div>

        <div class="row pl-3 mb-3">
          <div class="col">
            <b-button variant="outline-info" @click="applyFilters" data-cy="quiz-filterBtn"><i class="fa fa-filter" aria-hidden="true" /> Filter</b-button>
            <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="quiz-resetBtn"><i class="fa fa-times" aria-hidden="true" /> Reset</b-button>
          </div>
        </div>

        <skills-b-table :options="table.options" :items="table.items"
                        tableStoredStateId="quizSkillsTable"
                        data-cy="quizSkills">
          <template #head(projectId)="data">
            <span class="text-primary"><i class="fas fa-list-alt skills-color-users" aria-hidden="true"></i> {{ data.label }}</span>
          </template>
          <template #head(name)="data">
            <span class="text-primary"><i class="fas fa-graduation-cap skills-color-points" aria-hidden="true"></i> {{ data.label }}</span>
          </template>

          <template v-slot:cell(projectId)="data">
            <router-link v-if="data.item.canUserAccess"
              :to="{ name:'Subjects', params: { projectId: data.item.projectId  }}"
              class="text-info mb-0 pb-0 preview-card-title" :title="`${data.item.projectId }`"
              :aria-label="`manage project ${data.item.projectId }`"
              role="link">
              {{ data.item.projectId }}
            </router-link>
            <div v-else>{{ data.item.projectId }}</div>
          </template>

          <template v-slot:cell(name)="data">
            <router-link v-if="data.item.canUserAccess"
                         tag="a" :to="{ name:'SkillOverview',
                         params: { projectId: data.item.projectId, subjectId: data.item.subjectId, skillId: data.item.skillId }}"
                         :aria-label="`Manage skill ${data.item.skillName}  via link`">
              <span v-html="data.item.nameHtml ? data.item.nameHtml : data.item.skillName" />
            </router-link>
            <div v-else>
              <span v-html="data.item.nameHtml ? data.item.nameHtml : data.item.skillName" />
            </div>
            <div class="text-secondary" style="font-size: 0.9rem;">ID: {{data.item.skillId}}</div>
          </template>
        </skills-b-table>

      </loading-container>
    </b-card>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import QuizService from '@/components/quiz/QuizService';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import LoadingContainer from '@/components/utils/LoadingContainer';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';

  export default {
    name: 'QuizSettings',
    components: {
      SubPageHeader,
      SkillsBTable,
      LoadingContainer,
    },
    data() {
      return {
        quizId: this.$route.params.quizId,
        skills: [],
        filter: {
          skillName: '',
        },
        table: {
          options: {
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'projectId',
            sortDesc: true,
            tableDescription: 'Skills Associated with this Quiz/Survey',
            fields: [
              {
                key: 'projectId',
                label: 'Project Id',
                sortable: true,
              },
              {
                key: 'name',
                label: 'Skill',
                sortable: true,
              },
            ],
            pagination: {
              server: false,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
          },
          items: [],
        },
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      docsUrl() {
        return `${this.$store.getters.config.docsHost}/dashboard/user-guide/quizzes-and-surveys.html#skill-association`;
      },
    },
    methods: {
      loadData() {
        QuizService.getSkillsForQuiz(this.quizId, this.$store.getters.userInfo.userId).then((result) => {
          if (result) {
            this.skills = result;
            this.table.items = result;
            this.table.options.busy = false;
          }
        });
      },
      applyFilters() {
        this.table.options.pagination.currentPage = 1;
        if (!this.filter.skillName || this.filter.skillName.trim() === '') {
          this.reset();
        } else {
          this.table.items = this.table.items.filter((q) => q.skillName.toLowerCase()
            .includes(this.filter.skillName.trim().toLowerCase()))?.map((item) => {
            const nameHtml = StringHighlighter.highlight(item.skillName, this.filter.skillName);
            return {
              nameHtml,
              ...item,
            };
          });
        }
        this.$nextTick(() => this.$announcer.polite(`Associated skills table has been filtered by ${this.filter.skillName}`));
      },
      reset() {
        this.filter.skillName = '';
        this.table.options.pagination.currentPage = 1;
        this.table.items = this.skills;
        this.$nextTick(() => this.$announcer.polite('Associated skills table filters have been removed'));
      },
    },
  };
</script>

<style scoped>

</style>
