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
  <div class="border pb-2" data-cy="questionDisplayCard">
    <div class="row" :data-cy="`questionDisplayCard-${questionNum}`">
      <div class="col">

        <b-row :no-gutters="true" class="mb-3">
          <b-col cols="auto">
            <div :id="`questionSortControl-${question.id}`"
                 class="sort-control mr-3"
                 @click.prevent.self
                 tabindex="0"
                 aria-label="Questions Sort Control. Press up or down to change the order of this question."
                 role="button"
                 @keyup.down="move(1)"
                 @keyup.up="move(-1)"
                 data-cy="sortControlHandle"><i class="fas fa-arrows-alt"/></div>
          </b-col>
          <b-col class="">
            <div class="px-2 py-1">
              <markdown-text :text="question.question" data-cy="questionDisplayText"/>

              <div v-if="!isTextInputType" class="mt-1 pl-1">
                <div v-for="(a, index) in question.answers" :key="a.id" class="row no-gutters">
                  <div class="col-auto pb-1" :data-cy="`answerDisplay-${index}`">
                    <select-correct-answer :value="a.isCorrect" :read-only="true" :is-radio-icon="isSingleChoiceType"
                                           font-size="1.3rem"/>
                  </div>
                  <div class="col ml-2 pb-1"><div class="answerText align-middle" :data-cy="`answer-${index}_displayText`">{{ a.answer }}</div>
                  </div>
                </div>
              </div>
              <div v-if="isTextInputType">
                <b-form-textarea
                  id="textarea"
                  placeholder="Users will be required to enter text."
                  :disabled="true"
                  rows="2"
                  max-rows="4"/>
              </div>
            </div>
          </b-col>
        </b-row>

      </div>
      <div class="col-auto">
        <b-button-group size="sm" class="ml-1 mt-2 mr-3">
          <b-button variant="outline-primary"
                    :data-cy="`editQuestionButton_${questionNum}`"
                    :aria-label="`Edit Question Number ${questionNum}`"
                    :id="`editQuestion_${question.id}`"
                    ref="editQuestionBtn"
                    @click="editQuestion"
                    title="Edit Question">
            Edit <i class="fas fa-edit" aria-hidden="true"/>
          </b-button>
          <b-button @click="showDeleteDialog = true"
                    variant="outline-primary"
                    ref="deleteQuestionBtn"
                    :data-cy="`deleteQuestionButton_${questionNum}`"
                    :aria-label="`delete question number ${questionNum}`"
                    title="Delete Question">
            Delete <i class="text-warning fas fa-trash" aria-hidden="true"/>
          </b-button>
        </b-button-group>
      </div>
    </div>

    <removal-validation v-if="showDeleteDialog" v-model="showDeleteDialog" @do-remove="deletedQuestion" @hidden="handleDeleteCancelled">
      <p>
        This will remove <span class="text-primary font-weight-bold">Question #{{ questionNum }}.</span>
      </p>
      <div>
        Any any associated answers and metrics for this questions will also be removed. Please proceed with caution.
      </div>
    </removal-validation>
  </div>
</template>

<script>
  import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer';
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import QuestionType from '@/common-components/quiz/QuestionType';
  import RemovalValidation from '@/components/utils/modal/RemovalValidation';

  export default {
    name: 'QuestionCard',
    components: { RemovalValidation, MarkdownText, SelectCorrectAnswer },
    props: {
      quizType: String,
      question: Object,
      questionNum: Number,
    },
    data() {
      return {
        showDeleteDialog: false,
      };
    },
    computed: {
      isSingleChoiceType() {
        return this.question.questionType === QuestionType.SingleChoice;
      },
      isTextInputType() {
        return this.question.questionType === QuestionType.TextInput;
      },
    },
    methods: {
      editQuestion() {
        this.$emit('edit-question', this.question);
      },
      deletedQuestion() {
        this.$emit('delete-question', this.question);
      },
      handleDeleteCancelled() {
        this.$refs.deleteQuestionBtn.focus();
      },
      move(changeIndexBy) {
        console.log(`move by ${changeIndexBy}`);
        this.$emit('sort-change-requested', { question: this.question, newIndex: this.questionNum + changeIndexBy - 1 });
      },
    },
  };
</script>

<style lang="scss" scoped>
@import "@/assets/custom";

.sort-control i {
  padding: 0.4rem;
  font-size: 1.2rem;
  color: #b3b3b3 !important;
  top: 0rem;
  left: 0rem;
  border-bottom: 1px solid #e8e8e8;
  border-right: 1px solid #e8e8e8;
  background-color: #fbfbfb !important;
  border-bottom-right-radius: .25rem !important
}

.sort-control:hover, .sort-control i:hover {
  cursor: grab !important;
  color: $info !important;
  font-size: 1.5rem;
}

.answerText {
  font-size: 0.9rem;
}

</style>
