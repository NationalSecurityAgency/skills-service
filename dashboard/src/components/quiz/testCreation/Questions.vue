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
    <sub-page-header ref="subPageHeader"
                     title="Questions"
                     action="Question"
                     @add-action="openNewAnswerModal"
                     aria-label="new question"/>

    <b-card body-class="p-0" footer-bg-variant="white">
      <skills-spinner :is-loading="isLoading" />
      <div v-if="!isLoading">
        <no-content2 v-if="!hasData" class="my-5"
                     data-cy="noQuestionsYet"
                     title="No Questions Yet..." message="Create a question to get started."/>
        <div v-if="hasData" id="questionsCard">
<!--          <div class="p-2 text-right">-->
<!--            <b-button variant="outline-info"><i class="far fa-minus-square"></i> Collapse All</b-button>-->
<!--          </div>-->
          <div v-for="(q, index) in questions" :key="q.id" :id="q.id">
            <b-overlay :show="sortOrder.loading" rounded="sm" opacity="0.4">
              <template #overlay>
                <div class="text-center" :data-cy="`${q.id}_overlayShown`">
                  <div v-if="q.id.toString()===sortOrder.loadingQuestionId"
                       data-cy="updatingSortMsg">
                    <div class="text-info text-uppercase mb-1">Updating sort order!</div>
                    <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                  </div>
                </div>
              </template>
              <question-card :question="q" :quiz-type="quizType" :question-num="index+1"></question-card>
            </b-overlay>
            <!--        <hr v-if="index + 1 < questions.length"/>-->
          </div>
        </div>
      </div>

      <template #footer>
        <div class="text-right">
          <b-button ref="newQuestionOnBottomBtn"
                    data-cy="newQuestionOnBottomBtn"
                    variant="outline-primary"
                    size="sm"
                    @click="openNewAnswerModal('newQuestionOnBottomBtn')">
            Question <i class="fas fa-plus-circle"/>
          </b-button>
        </div>
      </template>
    </b-card>

    <edit-question v-if="editQuestionInfo.showDialog" v-model="editQuestionInfo.showDialog"
                   :is-edit="editQuestionInfo.isEdit"
                   @question-saved="questionDefSaved"
                   @hidden="handleNewQuestionBtnFocus"
                   :question-def="editQuestionInfo.questionDef"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import Sortable from 'sortablejs';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import QuestionCard from '@/components/quiz/testCreation/QuestionCard';
  import EditQuestion from '@/components/quiz/testCreation/EditQuestion';
  import QuizService from '@/components/quiz/QuizService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import NoContent2 from '@/components/utils/NoContent2';
  import QuestionType from '@/common-components/quiz/QuestionType';

  const { mapActions } = createNamespacedHelpers('quiz');

  export default {
    name: 'Questions',
    components: {
      NoContent2,
      SkillsSpinner,
      EditQuestion,
      QuestionCard,
      SubPageHeader,
    },
    data() {
      return {
        isLoading: false,
        quizId: this.$route.params.quizId,
        quizType: null,
        questions: [],
        editQuestionInfo: {
          showDialog: false,
          isEdit: false,
          questionDef: {},
          initiatedByBtnRef: null,
        },
        sortOrder: {
          loading: false,
          loadingQuestionId: -1,
        },
      };
    },
    mounted() {
      this.loadQuestions();
    },
    computed: {
      hasData() {
        return this.questions && this.questions.length > 0;
      },
    },
    methods: {
      ...mapActions([
        'loadQuizSummary',
      ]),
      questionDefSaved(questionDef) {
        const questionDefWithQuizId = ({ ...questionDef, quizId: this.quizId });
        QuizService.saveQuizQuestionDef(this.quizId, questionDefWithQuizId)
          .then((res) => {
            this.loadQuizSummary({ quizId: this.quizId }).then(() => this.handleNewQuestionBtnFocus());
            this.questions.push(res);
          });
      },
      handleNewQuestionBtnFocus() {
        const self = this;
        this.$nextTick(() => {
          this.$nextTick(() => {
            if (this.editQuestionInfo.initiatedByBtnRef) {
              self.$refs[self.editQuestionInfo.initiatedByBtnRef]?.focus();
            } else {
              self.$refs?.subPageHeader?.$refs?.actionButton?.focus();
            }
          });
        });
      },
      openNewAnswerModal(initiatedBtnRef = null) {
        this.editQuestionInfo.questionDef = {
          id: null,
          question: '',
          type: QuestionType.MultipleChoice,
          quizType: this.quizType,
          answers: [{
            id: null,
            answer: '',
            isCorrect: false,
          }, {
            id: null,
            answer: '',
            isCorrect: false,
          }],
        };
        this.editQuestionInfo.showDialog = true;
        this.editQuestionInfo.initiatedByBtnRef = initiatedBtnRef;
      },
      loadQuestions() {
        this.isLoading = true;
        QuizService.getQuizQuestionDefs(this.quizId)
          .then((res) => {
            this.questions = res.questions;
            this.quizType = res.quizType;
          })
          .finally(() => {
            this.isLoading = false;
            this.enableDropAndDrop();
          });
      },
      enableDropAndDrop() {
        if (this.hasData && this.questions.length > 0) {
          const self = this;
          this.$nextTick(() => {
            const cards = document.getElementById('questionsCard');
            // need to check for null because this logic is within nextTick method
            // an may actually run after the user moved onto another page
            if (cards) {
              Sortable.create(cards, {
                handle: '.sort-control',
                animation: 150,
                ghostClass: 'skills-sort-order-ghost-class',
                onUpdate(event) {
                  self.sortOrderUpdate(event);
                },
              });
            }
          });
        }
      },
      sortOrderUpdate(updateEvent) {
        const { id } = updateEvent.item;
        this.sortOrder.loadingQuestionId = id;
        this.sortOrder.loading = true;
        QuizService.updateQuizQuestionDisplaySortOrder(this.quizId, id, updateEvent.newIndex)
          .finally(() => {
            this.sortOrder.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
