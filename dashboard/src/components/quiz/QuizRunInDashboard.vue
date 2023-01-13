<template>
  <quiz-run v-if="quizId" :quiz-id="quizId" />
</template>

<script>
  import QuizRun from '@/common-components/quiz/QuizRun';

  export default {
    name: 'QuizRunInDashboard',
    components: { QuizRun },
    computed: {
      quizId() {
        return this.$route?.params?.quizId;
      },
    },
  };
</script>

<style scoped>

</style>
