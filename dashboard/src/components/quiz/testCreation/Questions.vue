/*
Copyright 2024 SkillTree

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
<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useQuizSummaryState } from '@/stores/UseQuizSummaryState.js';
import { useQuizConfig } from '@/stores/UseQuizConfig.js';
import { useFocusState } from '@/stores/UseFocusState.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useElementHelper } from '@/components/utils/inputForm/UseElementHelper.js';
import Sortable from 'sortablejs';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import NoContent2 from '@/components/utils/NoContent2.vue';
import QuestionCard from '@/components/quiz/testCreation/QuestionCard.vue';
import EditQuestion from '@/components/quiz/testCreation/EditQuestion.vue';
import QuizService from '@/components/quiz/QuizService.js';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';

const announcer = useSkillsAnnouncer()
const route = useRoute()
const quizSummaryState = useQuizSummaryState()
const quizConfig = useQuizConfig()
const focusState = useFocusState()
const elementHelper = useElementHelper()

const loadingQuestions = ref(false);
const operationInProgress = ref(false);
const questions = ref([]);
const quizType = ref(null);
const sortOrder = ref({
  loading: false,
  loadingQuestionId: -1,
});
const editQuestionInfo = ref({
  showDialog: false,
  isEdit: true,
  isCopy: false,
  questionDef: {},
})
const isLoading = computed(() => quizConfig.loadingQuizConfig || loadingQuestions.value);
const hasData = computed(() => questions.value && questions.value.length > 0)

onMounted(() => {
  loadQuestions(route.params.quizId)
})

function loadQuestions(quizId) {
  loadingQuestions.value = true;
  QuizService.getQuizQuestionDefs(quizId)
      .then((res) => {
        quizType.value = res.quizType;
        questions.value = res.questions;
      })
      .finally(() => {
        loadingQuestions.value = false;
        enableDropAndDrop();
      });
}

function enableDropAndDrop() {
  if (hasData.value) {
    nextTick(() => {
      elementHelper.getElementById('questionsCard').then((cards) => {
        // need to check for null because this logic is within the nextTick method
        // and may actually run after the user moved onto another page
        if (cards) {
          Sortable.create(cards, {
            handle: '.sort-control',
            animation: 150,
            ghostClass: 'skills-sort-order-ghost-class',
            onUpdate(event) {
              sortOrderUpdate(event);
            },
          });
        }
      });
    });
  }
}

function sortOrderUpdate(updateEvent) {
  const {id} = updateEvent.item;
  sortOrder.value.loadingQuestionId = id;
  sortOrder.value.loading = true;
  QuizService.updateQuizQuestionDisplaySortOrder(route.params.quizId, id, updateEvent.newIndex)
      .finally(() => {
        sortOrder.value.loading = false;
        announcer.polite(`Sort order changed. This is now a question number ${updateEvent.newIndex + 1}`);
      });
}

function openNewQuestionModal() {
  editQuestionInfo.value.questionDef = {
    id: null,
    question: '',
    type: QuestionType.MultipleChoice,
    quizType: quizType.value,
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
  editQuestionInfo.value.isEdit = false;
  editQuestionInfo.value.isCopy = false;
  editQuestionInfo.value.showDialog = true;
}
function questionDefSaved(questionDef) {
  try {
    operationInProgress.value = true;
    if (questionDef.isEdit || questionDef.isCopy) {
      // is edit
      questions.value = questions.value.map((q) => {
        if (q.id === questionDef.id) {
          return questionDef;
        }
        return q;
      });
    } else {
      // is new
      questions.value.push(questionDef)
    }
    if (questions.value && questions.value.length === 1) {
      enableDropAndDrop();
    }
    quizSummaryState.loadQuizSummary(route.params.quizId)
  } finally {
    announcer.polite('Question was successfully updated.');
    operationInProgress.value = false;
  }
}
function initiatedEditQuestionDef(questionDef) {
  editQuestionInfo.value.questionDef = { ...questionDef, quizId: route.params.quizId, quizType: quizType.value };
  editQuestionInfo.value.isEdit = true;
  editQuestionInfo.value.isCopy = false;
  editQuestionInfo.value.showDialog = true;
}
function copyQuestion(questionDef) {
  editQuestionInfo.value.questionDef = { ...questionDef, quizId: route.params.quizId, quizType: quizType.value };
  editQuestionInfo.value.isCopy = true;
  editQuestionInfo.value.isEdit = false;
  editQuestionInfo.value.showDialog = true;
}

function deleteQuestion(questionDef) {
  operationInProgress.value = true;
  QuizService.deleteQuizQuestion(route.params.quizId, questionDef.id)
      .then(() => {
        questions.value = questions.value.filter((q) => q.id !== questionDef.id);
        quizSummaryState.loadQuizSummary(route.params.quizId)
            .then(() => handleNewQuestionBtnFocus());
      }).finally(() => {
    operationInProgress.value = false;
    announcer.polite('Question was successfully deleted.');
  });
}

function handleKeySortRequest(sortRequestInfo) {
  const {question, newIndex} = sortRequestInfo;
  if (newIndex >= 0 && newIndex < questions.value.length) {
    operationInProgress.value = true;
    QuizService.updateQuizQuestionDisplaySortOrder(route.params.quizId, question.id, newIndex)
        .then(() => {
          questions.value = questions.value.filter((q) => q.id !== question.id);
          questions.value.splice(newIndex, 0, question);
        })
        .finally(() => {
          operationInProgress.value = false;
          focusState.setElementId(`questionSortControl-${question.id}`);
          focusState.focusOnLastElement()
          announcer.polite(`Sort order changed. This is now a question number ${newIndex + 1}`);
          nextTick(() => {
            nextTick(() => {
              const editBtn = document.getElementById(`questionSortControl-${question.id}`);
              if (editBtn) {
                editBtn.focus();
              }
              announcer.polite(`Sort order changed. This is now a question number ${newIndex + 1}`);
            });
          });
        });
  }
}
function handleNewQuestionBtnFocus() {
  focusState.setElementId('btn_Questions');
  focusState.focusOnLastElement()
}
</script>

<template>
  <div>
    <SubPageHeader ref="subPageHeader"
                   title="Questions"
                   :is-loading="quizConfig.loadingQuizConfig"
                   aria-label="new question">

      <SkillsButton v-if="!quizConfig.isReadOnlyQuiz"
                    @click="openNewQuestionModal()"
                    icon="fas fa-plus-circle"
                    outlined
                    size="small"
                    data-cy="btn_Questions"
                    id="btn_Questions"
                    aria-label="Create new Question"
                    :track-for-focus="true"
                    label="Question">
      </SkillsButton>
    </SubPageHeader>

    <BlockUI :blocked="operationInProgress">
      <Card :pt="{ body: { class: 'p-0!' } }">
        <template #content>
          <div>
            <SkillsSpinner :is-loading="isLoading" class="py-20"/>
            <div v-if="!isLoading">
              <NoContent2 v-if="!hasData"
                          title="No Questions Yet..."
                          class="mt-8 pt-8"
                          message="Create a question to get started."
                          data-cy="noQuestionsYet"/>
              <div v-if="hasData" id="questionsCard">
                <div v-for="(q, index) in questions" :key="q.id" :id="q.id">
                  <BlockUI :blocked="sortOrder.loading">
                    <div class="absolute top-1/2 z-50 w-full text-center" :data-cy="`${q.id}_overlayShown`">
                      <div v-if="sortOrder.loading && q.id.toString()===sortOrder.loadingQuestionId" data-cy="updatingSortMsg" >
                        <div class="text-primary uppercase mb-1">Updating sort order!</div>
                        <div class="flex justify-center">
                          <SkillsSpinner label="Loading..." extra-class="m-0" :is-loading="true"
                                         style="width: 3rem; height: 3rem;" variant="info"/>
                        </div>
                      </div>
                    </div>
                    <QuestionCard
                        @edit-question="initiatedEditQuestionDef"
                        @copy-question="copyQuestion"
                        @delete-question="deleteQuestion"
                        @sort-change-requested="handleKeySortRequest"
                        :question="q"
                        :quiz-type="quizType"
                        :show-drag-and-drop-controls="questions && questions.length > 1"
                        :question-num="index+1"/>
                  </BlockUI>
                </div>
              </div>
            </div>
          </div>
        </template>

        <template #footer v-if="!quizConfig.isReadOnlyQuiz && !quizConfig.loadingQuizConfig">
          <div class="flex justify-end flex-wrap p-4">
            <SkillsButton @click="openNewQuestionModal()"
                          icon="fas fa-plus-circle"
                          outlined
                          size="small"
                          data-cy="newQuestionOnBottomBtn"
                          aria-label="Create new Question"
                          ref="newQuestionOnBottomBtn"
                          id="newQuestionOnBottomBtn"
                          :track-for-focus="true"
                          label="Question">
            </SkillsButton>
          </div>
        </template>
      </Card>
    </BlockUI>

    <!-- Edit Question Modal -->
    <edit-question
        data-cy="editQuestionModal"
        v-if="editQuestionInfo.showDialog"
        v-model="editQuestionInfo.showDialog"
        :question-def="editQuestionInfo.questionDef"
        :is-edit="editQuestionInfo.isEdit"
        :is-copy="editQuestionInfo.isCopy"
        :enable-return-focus="true"
        @question-saved="questionDefSaved" />
  </div>
</template>

<style scoped>

</style>