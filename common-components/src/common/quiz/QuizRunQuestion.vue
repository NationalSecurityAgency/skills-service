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
  <div class="row no-gutters mb-4">
    <div class="col-auto pt-2 pr-2">
      <b-badge class="d-inline-block" :variant="`${q.gradedInfo ? (q.gradedInfo.isCorrect ? 'success' : 'danger') : 'default'}`">{{num + 1}}</b-badge>
      <span v-if="q.gradedInfo" class="ml-1 pt-1">
        <span v-if="q.gradedInfo.isCorrect" class="text-success" style="font-size: 1.1rem;"><i class="fas fa-check-double"></i></span>
        <span v-if="!q.gradedInfo.isCorrect" class="text-danger" style="font-size: 1.1rem;"><i class="fas fa-times-circle"></i></span>
      </span>
    </div>

    <div class="col">
      <markdown-text :text="q.question"/>

      <div v-if="q.canSelectMoreThanOne" class="text-secondary font-italic small">(Select <b>all</b> that apply)</div>
      <div class="mt-1 pl-1">
        <div v-for="a in answerOptions" :key="a.id">
          <quiz-run-answer
              :a="a"
              :can-select-more-than-one="q.canSelectMoreThanOne"
              @selection-changed="selectionChanged"/>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import QuizRunAnswer from '@/common-components/quiz/QuizRunAnswer';

  export default {
    name: 'QuizRunQuestion',
    components: { QuizRunAnswer, MarkdownText },
    props: {
      q: Object,
      num: Number,
    },
    data() {
      return {
        answerOptions: [],
      };
    },
    mounted() {
      this.answerOptions = this.q.answerOptions.map((a) => ({ ...a, selected: a.selected ? a.selected : false }));
    },
    methods: {
      selectionChanged(selectedStatus) {
        this.answerOptions = this.answerOptions.map((a) => {
          const isThisId = a.id === selectedStatus.id;
          const isSelected = isThisId && selectedStatus.selected;
          const selectRes = isSelected || (this.q.canSelectMoreThanOne && a.selected && !isThisId);
          return {
            ...a,
            selected: selectRes,
          };
        });
        const selectedAnswerIds = this.answerOptions.filter((a) => a.selected).map((a) => a.id);
        const currentAnswer = { questionId: this.q.id, selectedAnswerIds };
        this.$emit('selected-answer', currentAnswer);
      },
    },
  };
</script>

<style scoped>

</style>
