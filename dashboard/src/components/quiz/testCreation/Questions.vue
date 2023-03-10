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
                     :is-loading="isLoading"
                     aria-label="new question">
      <b-button v-if="!isReadOnlyQuiz"
                id="btn_Questions"
                ref="btn_Questions"
                @click="openNewAnswerModal()"
                variant="outline-primary"
                size="sm"
                data-cy="btn_Questions"
                aria-label="Create new Question" role="button">
        <span class="d-none d-sm-inline">Question</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
      </b-button>
    </sub-page-header>

    <b-overlay :show="operationInProgress" rounded="sm">
    <b-card body-class="p-0" footer-bg-variant="white">
      <skills-spinner :is-loading="isLoading" class="mb-5"/>
      <div v-if="!isLoading">
        <no-content2 v-if="!hasData" class="my-5"
                     data-cy="noQuestionsYet"
                     title="No Questions Yet..." message="Create a question to get started."/>
        <div v-if="hasData" id="questionsCard">
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
              <question-card
                @edit-question="initiatedEditQuizDef"
                @delete-question="deleteQuestion"
                @sort-change-requested="handleKeySortRequest"
                :question="q"
                :quiz-type="quizType"
                :show-drag-and-drop-controls="questions && questions.length > 1"
                :question-num="index+1"/>
            </b-overlay>
          </div>
        </div>
      </div>

      <template v-if="!isLoading && !isReadOnlyQuiz" #footer>
        <div class="text-right">
          <b-button ref="newQuestionOnBottomBtn"
                    data-cy="newQuestionOnBottomBtn"
                    variant="outline-primary"
                    size="sm"
                    aria-label="Create new question"
                    @click="openNewAnswerModal('newQuestionOnBottomBtn')">
            Question <i class="fas fa-plus-circle"/>
          </b-button>
        </div>
      </template>
    </b-card>
    </b-overlay>

    <edit-question v-if="editQuestionInfo.showDialog" v-model="editQuestionInfo.showDialog"
                   :is-edit="editQuestionInfo.isEdit"
                   @question-saved="questionDefSaved"
                   @hidden="handleEditQuestionModalClose"
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
  import QuizConfigMixin from '@/components/quiz/QuizConfigMixin';

  const { mapActions } = createNamespacedHelpers('quiz');

  export default {
    name: 'Questions',
    mixins: [QuizConfigMixin],
    components: {
      NoContent2,
      SkillsSpinner,
      EditQuestion,
      QuestionCard,
      SubPageHeader,
    },
    data() {
      return {
        isLoading: true,
        operationInProgress: false,
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
        this.operationInProgress = true;
        if (questionDef.id) {
          QuizService.updateQuizQuestionDef(this.quizId, questionDefWithQuizId)
            .then((res) => {
              this.questions = this.questions.map((q) => {
                if (q.id === res.id) {
                  return res;
                }
                return q;
              });
              if (this.questions && this.questions.length === 1) {
                this.enableDropAndDrop();
              }
              this.loadQuizSummary({ quizId: this.quizId })
                .then(() => this.handleEditQuestionBtnFocus(questionDef));
            }).finally(() => { this.operationInProgress = false; });
        } else {
          QuizService.saveQuizQuestionDef(this.quizId, questionDefWithQuizId)
            .then((res) => {
              this.questions.push(res);
              if (this.questions && this.questions.length === 1) {
                this.enableDropAndDrop();
              }
              this.loadQuizSummary({ quizId: this.quizId })
                .then(() => {
                  this.handleNewQuestionBtnFocus();
                });
            }).finally(() => { this.operationInProgress = false; });
        }
      },
      initiatedEditQuizDef(questionDef) {
        this.editQuestionInfo.questionDef = { ...questionDef, quizId: this.quizId, quizType: this.quizType };
        this.editQuestionInfo.isEdit = true;
        this.editQuestionInfo.showDialog = true;
        this.editQuestionInfo.initiatedByBtnRef = null;
      },
      deleteQuestion(questionDef) {
        this.operationInProgress = true;
        QuizService.deleteQuizQuestion(this.quizId, questionDef.id)
          .then(() => {
            this.questions = this.questions.filter((q) => q.id !== questionDef.id);
            this.loadQuizSummary({ quizId: this.quizId })
              .then(() => this.handleNewQuestionBtnFocus());
          }).finally(() => { this.operationInProgress = false; });
      },
      handleEditQuestionModalClose(questionDef) {
        if (questionDef.id) {
          this.handleEditQuestionBtnFocus(questionDef);
        } else {
          this.handleNewQuestionBtnFocus();
        }
      },
      handleNewQuestionBtnFocus() {
        const self = this;
        this.$nextTick(() => {
          this.$nextTick(() => {
            if (this.editQuestionInfo.initiatedByBtnRef) {
              self.$refs[self.editQuestionInfo.initiatedByBtnRef]?.focus();
            } else {
              self.$refs.btn_Questions?.focus();
            }
          });
        });
      },
      handleEditQuestionBtnFocus(questionDef) {
        this.$nextTick(() => {
          this.$nextTick(() => {
            const editBtn = document.getElementById(`editQuestion_${questionDef.id}`);
            if (editBtn) {
              editBtn.focus();
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
        this.editQuestionInfo.isEdit = false;
        this.editQuestionInfo.initiatedByBtnRef = initiatedBtnRef;
        this.editQuestionInfo.showDialog = true;
      },
      loadQuestions() {
        this.isLoading = true;
        QuizService.getQuizQuestionDefs(this.quizId)
          .then((res) => {
            this.quizType = res.quizType;
            this.questions = res.questions;
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
      handleKeySortRequest(sortRequestInfo) {
        const { question, newIndex } = sortRequestInfo;
        if (newIndex >= 0 && newIndex < this.questions.length) {
          this.operationInProgress = true;
          QuizService.updateQuizQuestionDisplaySortOrder(this.quizId, question.id, newIndex)
            .then(() => {
              this.questions = this.questions.filter((q) => q.id !== question.id);
              this.questions.splice(newIndex, 0, question);
            })
            .finally(() => {
              this.operationInProgress = false;
              this.$nextTick(() => {
                this.$nextTick(() => {
                  const editBtn = document.getElementById(`questionSortControl-${question.id}`);
                  if (editBtn) {
                    editBtn.focus();
                  }
                });
              });
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
