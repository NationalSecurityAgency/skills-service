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
<div class="container-fluid">
  <skills-spinner :is-loading="isLoading" class="mt-3"/>
  <div v-if="!isLoading">
    <div class="row bg-white border-bottom py-2 mb-3 pt-4" data-cy="subPageHeader">
      <div class="col">
        <div class="h4 text-success">{{ quizInfo.name }}</div>
      </div>
      <div class="col-auto text-right text-muted">
        <b-badge variant="success">{{quizInfo.questions.length}}</b-badge> <span class="text-uppercase">questions</span>
      </div>
    </div>

    <b-card body-class="pl-5">
      <div v-for="(q, index) in quizInfo.questions" :key="q.id">
        <quiz-run-question :q="q" :num="index" />
      </div>

      <div class="text-center">
        <b-button variant="success">Done</b-button>
      </div>
    </b-card>

  </div>
</div>
</template>

<script>
  import QuizRunService from '@/common-components/quiz/QuizRunService';
  import SkillsSpinner from '@/common-components/utilities/SkillsSpinner';
  import QuizRunQuestion from '@/common-components/quiz/QuizRunQuestion';

  export default {
    name: 'QuizRun',
    components: { SkillsSpinner, QuizRunQuestion },
    data() {
      return {
        isLoading: true,
        quizId: this.$route.params.quizId,
        quizInfo: null,
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        QuizRunService.getQuizInfo(this.quizId)
          .then((res) => {
            const copy = ({ ...res });
            copy.questions = res.questions.map((q) => {
              const answerOptions = q.answerOptions.map((a) => ({
                ...a,
                selected: false,
              }));
              return ({ ...q, answerOptions });
            });
            this.quizInfo = copy;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
