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
    <page-header :loading="isLoading" :options="headerOptions">
      <div slot="subSubTitle" v-if="quiz">
        <b-button-group class="mt-1" size="sm">
          <b-button ref="editQuizButton"
                    class="btn btn-outline-primary"
                    size="sm"
                    variant="outline-primary"
                    data-cy="btn_edit-quiz"
                    :aria-label="'edit Quiz '+ quiz.quizId">
            <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
          </b-button>
          <b-button target="_blank" :to="{ name:'MyProjectSkills', params: { quiz: quiz.quizId } }"
                    data-cy="quizPreview"
                    variant="outline-primary" :aria-label="'preview client display for project'+quiz.name">
            <span>Preview</span> <i class="fas fa-eye" style="font-size:1rem;" aria-hidden="true"/>
          </b-button>
        </b-button-group>
      </div>
    </page-header>

    <navigation v-if="!isLoading" :nav-items="[
          {name: 'Questions', iconClass: 'fa-graduation-cap skills-color-skills', page: 'Questions'},
          {name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'QuizMetrics'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import Navigation from '@/components/utils/Navigation';
  import PageHeader from '@/components/utils/pages/PageHeader';

  export default {
    name: 'QuizPage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: false,
        quiz: {
          name: 'My First Cool Quiz',
          quizId: 'myFirstCoolQuiz',
          numQuestions: 12,
          createdOn: 1626892932373,
          live: true,
        },
      };
    },
    computed: {
      headerOptions() {
        if (!this.quiz) {
          return {};
        }
        return {
          icon: 'fas fa-cubes skills-color-subjects',
          title: `QUIZ: ${this.quiz.name}`,
          subTitle: `ID: ${this.quiz.quizId}`,
          stats: [{
            label: 'Questions',
            count: this.quiz.numQuestions,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>
