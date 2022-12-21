<template>
<div class="container-fluid">
  <skills-spinner :is-loading="isLoading" class="mt-3"/>
  <div v-if="!isLoading">
    <div class="row bg-white border-bottom py-2 mb-3 pt-4 text-center" data-cy="subPageHeader">
      <div class="col-sm-6 col-md-7 text-sm-left">
        <h1 class="h4">{{ quizInfo.name }}</h1>
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
            this.quizInfo = res;
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
