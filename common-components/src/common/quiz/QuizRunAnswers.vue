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
    <div v-for="(a, aIndex) in answerOptionsInternal" :key="a.id">
      <quiz-run-answer
          :data-cy="`answer_${aIndex+1}`"
          :a="a"
          :answer-num="aIndex+1"
          :q-num="qNum"
          :can-select-more-than-one="canSelectMoreThanOne"
          @selection-changed="selectionChanged"/>
    </div>
  </div>
</template>

<script>
  import QuizRunAnswer from '@/common-components/quiz/QuizRunAnswer';
  import QuestionType from '@/common-components/quiz/QuestionType';

  export default {
    name: 'QuizRunAnswers',
    components: { QuizRunAnswer },
    props: {
      value: Array,
      q: Object,
      qNum: Number,
      canSelectMoreThanOne: Boolean,
    },
    data() {
      return {
        answerOptionsInternal: [],
      };
    },
    mounted() {
      this.answerOptionsInternal = this.value.map((a) => ({ ...a }));
    },
    watch: {
      value(newVal) {
        this.answerOptionsInternal = newVal ? newVal.map((a) => ({ ...a })) : [];
      },
      'q.gradedInfo': function handleGraded(gradedInfo) {
        this.answerOptionsInternal = this.answerOptionsInternal.map((answer) => ({ ...answer, isGraded: true, isCorrect: gradedInfo.correctAnswerIds.indexOf(answer.id) >= 0 }));
      },
    },
    methods: {
      selectionChanged(selectedStatus) {
        this.answerOptionsInternal = this.value.map((a) => {
          const isThisId = a.id === selectedStatus.id;
          const isSelected = isThisId && selectedStatus.selected;
          const selectRes = isSelected || (this.q.questionType === QuestionType.MultipleChoice && a.selected && !isThisId);
          return {
            ...a,
            selected: selectRes,
          };
        });
        const selectedAnswerIds = this.answerOptionsInternal.filter((a) => a.selected).map((a) => a.id);
        const currentAnswer = {
          questionId: this.q.id,
          questionType: this.q.questionType,
          selectedAnswerIds,
          changedAnswerId: selectedStatus.id,
          changedAnswerIdSelected: selectedStatus.selected,
        };
        this.$emit('input', this.answerOptionsInternal);
        this.$emit('selected-answer', currentAnswer);
      },
    },
  };
</script>

<style scoped>
.answer-row {
  padding: 0.2rem 1rem 0rem 1rem  !important;
  margin-bottom: 0.1rem;
  border: 1px dotted transparent;
}

.answer-row-editable:hover {
  border: 1px dotted #007c49;
  border-radius: 5px;
}

.point-cursor {
  cursor: pointer;
}

i {
  color: #b6b5b5;
}
.checkmark {
  font-size: 1.2rem;
}

.selected-answer {
  background-color: lightgray;
  border: 1px dotted #007c49;
  border-radius: 5px;
  font-weight: bold;
}

.answerText {
  font-size: 0.8rem;
}
</style>
