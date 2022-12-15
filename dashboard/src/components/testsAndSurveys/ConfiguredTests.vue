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
        <b-button variant="outline-info" @click="applyFilters" data-cy="users-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
        <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="users-resetBtn"><i class="fa fa-times"/> Reset</b-button>
      </div>
    </div>

    <skills-b-table :options="options" :items="quizzes"
                    data-cy="performedSkillsTable">
      <template v-slot:cell(name)="data">
        <div class="row">
          <div class="col">
            <div class="h5">{{ data.item.name }} <b-badge v-if="data.item.live" variant="success" style="font-size: 0.9rem;"><i class="fas fa-rocket"></i> Live</b-badge></div>
            <div class="text-muted" style="font-size: 0.9rem;">ID: {{ data.item.quizId }}</div>
          </div>
          <div class="col-auto text-right">
            <router-link :data-cy="`managesQuizBtn_${data.item.quizId}`" :to="{ name:'Questions', params: { testId: data.item.quizId }}"
                         :aria-label="`Manage Quiz ${data.item.name}`"
                         class="btn btn-outline-primary btn-sm">
              <span class="d-none d-sm-inline">Manage </span> <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
            </router-link>
            <b-button-group size="sm" class="ml-1">
              <b-button @click="editSkill(data.item)"
                        variant="outline-primary" :data-cy="`editSkillButton_${data.item.skillId}`"
                        :aria-label="'edit Skill '+data.item.name" :ref="'edit_'+data.item.skillId"
                        title="Edit Skill">
                <i class="fas fa-edit" aria-hidden="true"/>
              </b-button>
              <b-button @click="deleteSkill(data.item)" variant="outline-primary"
                        :data-cy="`deleteSkillButton_${data.item.skillId}`"
                        :aria-label="'delete Skill '+data.item.name"
                        title="Delete Skill">
                <i class="text-warning fas fa-trash" aria-hidden="true"/>
              </b-button>
            </b-button-group>
          </div>
        </div>
      </template>
      <template v-slot:cell(createdOn)="data">
        <date-cell :value="data.value" />
      </template>
      <template v-slot:cell(numQuestions)="data">
        {{ data.value | number }} <b-badge v-if="data.value === 0" variant="warning">Empty</b-badge>
      </template>
    </skills-b-table>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';

  export default {
    name: 'ConfiguredTests',
    components: { DateCell, SkillsBTable },
    data() {
      return {
        filter: {
          name: '',
        },
        quizzes: [
          {
            name: 'My First Cool Quiz',
            quizId: 'myFirstCoolQuiz',
            numQuestions: 12,
            createdOn: 1626892932373,
            live: true,
          },
          {
            name: 'Some other test',
            quizId: 'testone',
            numQuestions: 0,
            createdOn: 1626781931373,
            live: false,
          },
        ],
        options: {
          busy: false,
          bordered: true,
          outlined: true,
          stacked: 'md',
          sortBy: 'performedOn',
          sortDesc: true,
          fields: [
            {
              key: 'name',
              label: 'Quiz',
              sortable: true,
            },
            {
              key: 'numQuestions',
              label: 'Number of Questions',
              sortable: true,
            },
            {
              key: 'createdOn',
              label: 'Created On',
              sortable: true,
            },
          ],
          pagination: {
            server: true,
            currentPage: 1,
            totalRows: 1,
            pageSize: 5,
            possiblePageSizes: [5, 10, 15, 20],
          },
        },
      };
    },
    methods: {
      applyFilters() {
        this.quizzes = this.quizzes.map((q) => ({ ...q }));
      },
      reset() {
        this.quizzes = this.quizzes.map((q) => ({ ...q }));
      },
    },
  };

</script>

<style scoped>

</style>
