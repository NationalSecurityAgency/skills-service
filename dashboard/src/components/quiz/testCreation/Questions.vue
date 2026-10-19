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
import CopyQuestionModal from '@/components/quiz/testCreation/CopyQuestion.vue';
import QuizService from '@/components/quiz/QuizService.js';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import {useStorage} from "@vueuse/core";

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
const copyQuestionInfo = ref({
  showDialog: false,
  questionDef: {},
})
const questionExpandedStates = useStorage(`questionStates-${route.params.quizId}`, {});
const isLoading = computed(() => quizConfig.loadingQuizConfig || loadingQuestions.value);
const hasData = computed(() => questions.value && questions.value.length > 0)
const selectionModeEnabled = ref(false);

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

function copySelectedQuestions() {
  copyQuestionInfo.value.questionDef = {
    quizId: route.params.quizId,
    quizType: quizType.value
  };
  copyQuestionInfo.value.showDialog = true;
}

function toggleSelectionMode(value) {
  if(value) {
    collapseAllQuestions()
  } else {
    clearSelectedQuestions()
    expandAllQuestions()
  }
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
      multiPartAnswer: {
        term: '', value: ''
      },
    }, {
      id: null,
      answer: '',
      isCorrect: false,
      multiPartAnswer: {
        term: '', value: ''
      },
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
      if(!questionDef.isCopyToAnotherQuiz) {
        questions.value.push(questionDef)
      }
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
        delete questionExpandedStates.value[questionDef.id]
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

function collapseQuestion(questionId) {
  questionExpandedStates.value[questionId] = { collapsed: true };
}

function expandQuestion(questionId) {
  questionExpandedStates.value[questionId] = { collapsed: false };
}

function collapseAllQuestions() {
  const ids = questions.value.map(question => question.id)
  ids.forEach(id => {
    questionExpandedStates.value[id] = { collapsed: true };
  })
}

function expandAllQuestions() {
  const ids = questions.value.map(question => question.id)
  ids.forEach(id => {
    questionExpandedStates.value[id] = { collapsed: false };
  })
}

const areAllCollapsed = computed(() => {
  if(Object.keys(questionExpandedStates.value).length === 0) {
    return false;
  }

  const collapsed = Object.fromEntries(
      Object.entries(questionExpandedStates.value).filter(([key, value]) => value.collapsed === false)
  )

  return Object.keys(collapsed).length === 0
})

const areAllExpanded = computed(() => {
  if(Object.keys(questionExpandedStates.value).length === 0) {
    return true;
  }

  const collapsed = Object.fromEntries(
      Object.entries(questionExpandedStates.value).filter(([key, value]) => value.collapsed === false)
  )

  return Object.keys(collapsed).length === Object.keys(questionExpandedStates.value).length
})

function toggleQuestions() {
  if(areAllCollapsed.value) {
    expandAllQuestions()
  } else if(areAllExpanded.value) {
    collapseAllQuestions()
  } else {
    collapseAllQuestions();
  }
}

const selectedQuestions = ref({});
function questionSelected({ questionId, selected }) {
  if (selected) {
    selectedQuestions.value[questionId] = true
  } else {
    delete selectedQuestions.value[questionId]
  }
}

function clearSelectedQuestions() {
  const questionIds = Object.keys(selectedQuestions.value);
  questionIds.forEach((id) => {
    delete selectedQuestions.value[id]
  })
}

function selectAllQuestions() {
  const ids = questions.value.map(question => question.id)
  ids.forEach(id => {
    selectedQuestions.value[id] = true;
  })
}

const questionsActionsMenu = ref(false)
const toggleActionsMenu = (event) => {
  questionsActionsMenu.value.toggle(event)
}
const actionsMenu = ref([
  {
    label: 'Copy to Another Quiz',
    icon: 'fas fa-user-plus',
    command: () => {
      copySelectedQuestions()
    }
  },
  // {
  //   label: 'Delete',
  //   icon: 'fas fa-user-plus',
  //   command: () => {
  //     console.log('delete');
  //   }
  // },
])
function handleKeyDown(event) {
  // tab=9
  if (event.keyCode === 9) {
    event.preventDefault();
    if (actionMenuFocused.value) {
      focusState.focusOnLastElement()
    }
  }
}
const actionMenuFocused = ref(false)
const actionMenuOnFocus = () => {
  actionMenuFocused.value = true
  document.addEventListener('keydown', handleKeyDown);
}
const actionMenuOnBlur = () => {
  actionMenuFocused.value = false
  document.removeEventListener('keydown', handleKeyDown);

}
</script>

<template>
  <div>
    <SubPageHeader ref="subPageHeader"
                   title="Questions"
                   :is-loading="quizConfig.loadingQuizConfig"
                   aria-label="new question">

      <template #nextToTitle>
        <SkillsButton @click="toggleQuestions"
                      v-if="!quizConfig.isReadOnlyQuiz && questions && questions.length > 0 && !selectionModeEnabled"
                      :icon="areAllCollapsed ? 'fas fa-plus' : 'fas fa-minus'"
                      size="small"
                      data-cy="expandAllQuestions"
                      outlined
                      class="ml-2"
                      aria-label="Expand/Collapse All Questions" />
      </template>

      <div class="flex-1 flex items-center content-center">
        <ToggleSwitch
            data-cy="toggleSelectionMode"
            aria-label="Toggle select questions"
            class="mr-2"
            v-on:update:modelValue="toggleSelectionMode"
            v-model="selectionModeEnabled" />
        <label id="toggleSelectMessage">Select Questions</label>

        <SkillsButton
            id="questionActionsBtn"
            size="small"
            class="ml-3"
            v-if="selectionModeEnabled"
            @click="toggleActionsMenu"
            aria-label="Question actions button"
            aria-haspopup="true"
            :disabled="Object.keys(selectedQuestions).length === 0"
            :track-for-focus="true"
            data-cy="questionActionsBtn">
          <i class="fas fa-tools" aria-hidden="true"></i>
          <span>Action</span>
          <i class="fas fa-caret-down"></i>
        </SkillsButton>
        <Menu ref="questionsActionsMenu"
              v-if="selectionModeEnabled"
              id="questions-actions-menu"
              data-cy="questionsActionsMenu"
              :model="actionsMenu"
              @focus="actionMenuOnFocus"
              @blur="actionMenuOnBlur"
              aria-label="Menu to perform actions on selected questions"
              :popup="true">
          <template #item="{ item, props }">
            <a :href="item.url" target="_blank" v-bind="props.action">
              <span class="w-7 border text-center rounded-sm text-green-800 bg-green-50 dark:bg-gray-900 dark:text-green-500 dark:border-green-700"><i :class="item.icon"/></span>
              <span class="">{{ item.label }}</span>
            </a>
          </template>
        </Menu>

        <SkillsButton v-if="!quizConfig.isReadOnlyQuiz && !selectionModeEnabled"
                      @click="openNewQuestionModal()"
                      icon="fas fa-plus-circle"
                      outlined
                      class="ml-3"
                      size="small"
                      data-cy="btn_Questions"
                      id="btn_Questions"
                      aria-label="Create new Question"
                      :track-for-focus="true"
                      label="Question">
        </SkillsButton>
      </div>
    </SubPageHeader>

    <BlockUI :blocked="operationInProgress">
      <Card :pt="{ body: { class: 'p-0!' } }">
        <template #content>
          <div>
            <SkillsSpinner :is-loading="isLoading" class="py-20"/>
            <div v-if="!isLoading">
              <div v-if="selectionModeEnabled" class="mb-2 mt-1">
                <SkillsButton
                    label="Select All"
                    icon="fas fa-check"
                    size="small"
                    class="ml-1"
                    @click="selectAllQuestions"
                    data-cy="selectAllQuestionsBtn"
                />
                <SkillsButton
                    label="Deselect All"
                    icon="fas fa-times"
                    size="small"
                    class="ml-2"
                    @click="clearSelectedQuestions"
                    data-cy="deselectAllQuestionsBtn"
                />
              </div>
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
                        @collapse-question="collapseQuestion"
                        @expand-question="expandQuestion"
                        @question-select-toggled="questionSelected"
                        :question="q"
                        :selection-mode="selectionModeEnabled"
                        :collapsed="questionExpandedStates[q.id]?.collapsed"
                        :selected="selectedQuestions[q.id] === true"
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
                          v-if="!selectionModeEnabled"
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

    <copy-question-modal
        data-cy="copyQuestionModal"
        :question-def="copyQuestionInfo.questionDef"
        :questions-to-save="selectedQuestions"
        v-if="copyQuestionInfo.showDialog"
        v-model="copyQuestionInfo.showDialog"
        @copy-modal-closed="clearSelectedQuestions"
        :enable-return-focus="true" />

  </div>
</template>

<style scoped>

</style>