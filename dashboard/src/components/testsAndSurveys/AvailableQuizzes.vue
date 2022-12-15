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
    <b-card body-class="p-0">
      <div class="row px-3 pt-3">
        <div class="col-12">
          <b-form-group label="Quiz Name Filter" label-class="text-muted">
            <b-input v-model="filter.name" v-on:keyup.enter="applyFilters"
                     data-cy="skillsTable-skillFilter" aria-label="skill name filter"/>
          </b-form-group>
        </div>
        <div class="col-md">
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="users-filterBtn"><i class="fa fa-filter"/>
            Filter
          </b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="users-resetBtn"><i class="fa fa-times"/>
            Reset
          </b-button>
        </div>
      </div>

      <skills-b-table :options="options" :items="quizzes"
                      data-cy="performedSkillsTable">
        <template v-slot:cell(name)="data">
          <div class="row">
            <div class="col">
              <div class="h5 font-weight-bold text-secondary">{{ data.item.name }}
                <b-badge v-if="data.item.completed" variant="success" style="font-size: 0.9rem;" class="text-uppercase">
                  <i class="fas fa-check-circle"></i> completed
                </b-badge>
              </div>
              <div class="text-muted" style="font-size: 0.9rem;">{{ data.item.description }}</div>
              <div v-if="data.item.skillInfo" class="mt-1 ml-2">
                <span style="font-size: 1.1rem;"><b-badge variant="success">+{{ data.item.skillInfo.skillPoints }} POINTS</b-badge></span>
                Completion will
                earn
                <b-badge variant="success">{{ data.item.skillInfo.skillPoints }}</b-badge>
                points for the
                <b-badge variant="info">{{ data.item.skillInfo.skillName }}</b-badge>
                in the
                <b-badge variant="info">{{ data.item.skillInfo.projectName }}</b-badge>
              </div>
            </div>
            <div v-if="!data.item.completed" class="col-auto text-right">
              <router-link :data-cy="`take_quiz-${data.item.quizId}`"
                           :to="{ name:'StartQuiz', params: { quizId: data.item.quizId }}"
                           :aria-label="`Take Quiz ${data.item.name}`"
                           class="btn btn-outline-primary btn-sm">
                <span class="d-none d-sm-inline">Take Quiz </span> <i class="fas fa-arrow-circle-right"
                                                                      aria-hidden="true"/>
              </router-link>
            </div>
          </div>
        </template>
        <template v-slot:cell(createdOn)="data">
          <date-cell :value="data.value"/>
        </template>
        <template v-slot:cell(numQuestions)="data">
          {{ data.value | number }}
          <b-badge v-if="!data.item.completed">~{{ data.value * 2 }} minutes</b-badge>
        </template>
      </skills-b-table>
    </b-card>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';

  export default {
    name: 'AvailableQuizzes',
    components: {
      DateCell,
      SkillsBTable,
    },
    data() {
      return {
        filter: {
          name: '',
        },
        quizzes: [
          {
            name: 'My First Cool Quiz',
            description: 'Fun trivia questions!',
            quizId: 'myFirstCoolQuiz',
            numQuestions: 12,
            createdOn: 1626892932373,
            completed: true,
          },
          {
            name: 'Some other test',
            description: 'This is a very important tests!',
            quizId: 'testone',
            numQuestions: 5,
            createdOn: 1626781931373,
            completed: false,
            skillInfo: {
              projectId: 'proj1',
              projectName: 'Cool Project',
              skillId: 'neatSkill',
              skillName: 'Neat Skill',
              skillPoints: 50,
            },
          },
          {
            name: 'Some other test',
            description: 'This is a very important tests!',
            quizId: 'testone',
            numQuestions: 18,
            createdOn: 1626781931373,
            completed: false,
            skillInfo: {
              projectId: 'proj1',
              projectName: 'Cool Project',
              skillId: 'neatSkill',
              skillName: 'Blah Skill',
              skillPoints: 25,
            },
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
            totalRows: 3,
            pageSize: 5,
            possiblePageSizes: [5, 10, 15, 20],
          },
        },
      };
    },
  };
</script>

<style scoped>

</style>
