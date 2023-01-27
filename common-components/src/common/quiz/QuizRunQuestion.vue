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
  <div class="row no-gutters mb-4" :data-cy="`question_${num}`">
    <div class="col-auto pt-2 pr-2">
      <b-badge class="d-inline-block" :variant="`${q.gradedInfo ? (q.gradedInfo.isCorrect ? 'success' : 'danger') : 'default'}`">{{ num }}</b-badge>
      <span v-if="enableMissingIndicator && (isMissingAnswer || textInputErrMsg)" class="text-danger" style="font-size: 1.1rem;" data-cy="issueWithAnAnswer"><i class="fas fa-times-circle" aria-hidden="true"></i></span>
      <span v-if="q.gradedInfo" class="ml-1 pt-1">
        <span v-if="q.gradedInfo.isCorrect" class="text-success" style="font-size: 1.1rem;" data-cy="questionAnsweredCorrectly"><i class="fas fa-check-double" aria-hidden="true"></i></span>
        <span v-if="!q.gradedInfo.isCorrect" class="text-danger" style="font-size: 1.1rem;" data-cy="questionAnsweredWrong"><i class="fas fa-times-circle" aria-hidden="true"></i></span>
      </span>
    </div>

    <div class="col">
      <markdown-text :text="q.question" data-cy="questionsText" />

      <div v-if="isTextInput">
        <b-form-textarea
            id="textarea"
            data-cy="textInputAnswer"
            v-model="answerText"
            debounce="400"
            placeholder="Please enter your response here..."
            rows="2"
            max-rows="20"/>
        <div v-if="textInputErrMsg" class="text-danger" data-cy="textInputErrMsg"> {{ textInputErrMsg }}</div>
      </div>
      <div v-else>
        <div v-if="isMultipleChoice" class="text-secondary font-italic small" data-cy="multipleChoiceMsg">(Select <b>all</b> that apply)</div>
        <div class="mt-1 pl-1">
            <div v-for="(a, aIndex) in answerOptions" :key="a.id">
              <quiz-run-answer
                  :data-cy="`answer_${aIndex+1}`"
                  :a="a"
                  :can-select-more-than-one="isMultipleChoice"
                  @selection-changed="selectionChanged"/>
            </div>
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
      enableMissingIndicator: {
        type: Boolean,
        default: false,
      },
      textInputErrMsg: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        answerOptions: [],
        answerText: '',
      };
    },
    mounted() {
      this.answerOptions = this.q.answerOptions.map((a) => ({ ...a, selected: a.selected ? a.selected : false }));
      if (this.isTextInput) {
        const existingAnswerText = this.q.answerOptions[0].answerText;
        this.answerText = existingAnswerText || '';
      }
    },
    watch: {
      answerText() {
        this.textAnswerChanged();
      },
    },
    computed: {
      isMultipleChoice() {
        return this.q.questionType === 'MultipleChoice';
      },
      isSingleChoice() {
        return this.q.questionType === 'SingleChoice';
      },
      isTextInput() {
        return this.q.questionType === 'TextInput';
      },
      isMissingAnswer() {
        if (this.isTextInput) {
          return !this.answerText || this.answerText.trimEnd() === '';
        }
        return this.answerOptions.findIndex((a) => a.selected === true) < 0;
      },
    },
    methods: {
      textAnswerChanged() {
        // only 1 answer in case of TextInput
        const selectedAnswerIds = this.answerOptions.map((a) => a.id);
        const isAnswerBlank = !this.answerText || this.answerText.trimEnd() === '';
        const currentAnswer = {
          questionId: this.q.id,
          questionType: this.q.questionType,
          selectedAnswerIds,
          changedAnswerId: this.answerOptions[0].id,
          changedAnswerIdSelected: !isAnswerBlank,
          answerText: this.answerText,
        };
        this.$emit('answer-text-changed', currentAnswer);
      },
      selectionChanged(selectedStatus) {
        this.answerOptions = this.answerOptions.map((a) => {
          const isThisId = a.id === selectedStatus.id;
          const isSelected = isThisId && selectedStatus.selected;
          const selectRes = isSelected || (this.q.questionType === 'MultipleChoice' && a.selected && !isThisId);
          return {
            ...a,
            selected: selectRes,
          };
        });
        const selectedAnswerIds = this.answerOptions.filter((a) => a.selected).map((a) => a.id);
        const currentAnswer = {
          questionId: this.q.id,
          questionType: this.q.questionType,
          selectedAnswerIds,
          changedAnswerId: selectedStatus.id,
          changedAnswerIdSelected: selectedStatus.selected,
        };
        this.$emit('selected-answer', currentAnswer);
      },
    },
  };
</script>

<style scoped>

</style>
