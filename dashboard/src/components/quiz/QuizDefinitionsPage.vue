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
  <sub-page-header title="Tests And Surveys" action="Test" @add-action="openNewTestModal"/>

  <b-card body-class="p-0">
    <quiz-definitions ref="configuredTests"/>
  </b-card>

  <edit-test v-if="newQuiz.show" v-model="newTest.show"
             :test="newQuiz.quiz"
             @quiz-saved="saveQuiz" />
</div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import QuizDefinitions from '@/components/quiz/QuizDefinitions';
  import EditTest from '@/components/quiz/testCreation/EditQuiz';

  export default {
    name: 'TestsAndSurveys',
    components: { EditTest, QuizDefinitions, SubPageHeader },
    data() {
      return {
        showNewTestModal: false,
        newQuiz: {
          show: false,
          isEdit: false,
          quiz: {},
        },
      };
    },
    methods: {
      openNewTestModal() {
        this.newQuiz = {
          show: true,
          isEdit: false,
          quiz: {
            name: '',
            testId: '',
            description: '',
          },
        };
      },
      saveQuiz(quizDef) {
        this.$refs.configuredTests.saveQuiz(quizDef);
      },
    },
  };
</script>

<style scoped>

</style>
