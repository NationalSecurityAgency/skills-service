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
    <skills-spinner :is-loading="loadingQuizInfo" />
    <div v-if="!loadingQuizInfo">
      <sub-page-header :title="quizInfo.quizType" class="pt-4 pl-3">
      </sub-page-header>

      <quiz-run v-if="quizId"
                :quiz-id="quizId"
                :quiz="quizInfo"
                class="mb-5"
                @testWasTaken="navToProgressAndRanking"
                @cancelled="navToProgressAndRanking"/>
    </div>
  </div>
</template>

<script>
  import QuizRun from '@/common-components/quiz/QuizRun';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import QuizRunService from '@/common-components/quiz/QuizRunService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  export default {
    name: 'QuizRunInDashboard',
    components: {
      SkillsSpinner,
      SubPageHeader,
      QuizRun,
    },
    data() {
      return {
        quizInfo: {},
        loadingQuizInfo: true,
      };
    },
    mounted() {
      this.loadQuizInfo();
    },
    computed: {
      quizId() {
        return this.$route?.params?.quizId;
      },
    },
    methods: {
      navToProgressAndRanking() {
        this.$router.push({ name: 'MyProgressPage' });
      },
      loadQuizInfo() {
        this.loadingQuizInfo = true;
        QuizRunService.getQuizInfo(this.quizId)
          .then((quizInfo) => {
            this.quizInfo = quizInfo;
          })
          .finally(() => {
            this.loadingQuizInfo = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
