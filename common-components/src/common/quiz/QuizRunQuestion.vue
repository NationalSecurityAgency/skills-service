<template>
  <div class="row no-gutters mb-4">
    <div class="col-auto pt-2 pr-2">
      <b-badge class="d-inline-block">{{num + 1}}</b-badge>
    </div>

    <div class="col">
      <markdown-text :text="q.question"/>

      <div class="mt-1 pl-1">
        <div v-for="a in q.answerOptions" :key="a.id">
          <quiz-run-answer :a="a" :square-selector="a.canSelectMoreThanOne"/>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import QuizRunAnswer from '@/common-components/quiz/QuizRunAnswer';

  export default {
    name: 'QuizQuestion',
    components: { QuizRunAnswer, MarkdownText },
    props: {
      q: Object,
      num: Number,
    },
  };
</script>

<style scoped>

</style>
