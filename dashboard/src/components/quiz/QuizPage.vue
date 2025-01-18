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

import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router'
import { useQuizSummaryState } from '@/stores/UseQuizSummaryState.js';
import { useQuizConfig } from '@/stores/UseQuizConfig.js';
import { useFocusState } from '@/stores/UseFocusState.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import PageHeader from '@/components/utils/pages/PageHeader.vue';
import Navigation from '@/components/utils/Navigation.vue';
import UserRolesUtil from '@/components/utils/UserRolesUtil.js';
import EditQuiz from '@/components/quiz/testCreation/EditQuiz.vue';
import QuizType from "@/skills-display/components/quiz/QuizType.js";
import Avatar from 'primevue/avatar';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';

const announcer = useSkillsAnnouncer()
const router = useRouter()
const route = useRoute()
const quizSummaryState = useQuizSummaryState()
const quizConfig = useQuizConfig()
const focusState = useFocusState()
const appConfig = useAppConfig()

onMounted(() => {
  if (!quizSummaryState.quizSummary || quizSummaryState.quizSummary.quizId !== route.params.quizId) {
    quizSummaryState.loadQuizSummary(route.params.quizId).then((quizSummary) => {
      updateEditQuizInfo(quizSummary)
    })
  } else {
    updateEditQuizInfo(quizSummaryState.quizSummary)
  }
})

const isQuiz = computed(() => QuizType.isQuiz(quizSummaryState.quizSummary?.type))
const isSurvey = computed(() => QuizType.isSurvey(quizSummaryState.quizSummary?.type))
const isLoading = computed(() => quizSummaryState.loadingQuizSummary || quizConfig.loadingQuizConfig)
const navItems = computed(() => {
  const res = [
    { name: 'Questions', iconClass: 'fa-graduation-cap', page: 'Questions' },
    { name: 'Results', iconClass: 'fa-chart-bar', page: 'QuizMetrics' },
    { name: 'Runs', iconClass: 'fa-users', page: 'QuizRunsHistoryPage' },
    { name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'QuizSkillsPage' },
  ];

  if (!quizConfig.isReadOnlyQuiz) {
    if (isQuiz.value) {
      res.push({ name: 'Grading', iconClass: 'fas fa-user-check', page: 'GradeQuizzesPage' });
    }
    res.push({ name: 'Access', iconClass: 'fas fa-shield-alt', page: 'QuizAccessPage' });
    res.push({ name: 'Settings', iconClass: 'fa-cogs', page: 'QuizSettings' });
    res.push({ name: 'Activity History', iconClass: 'fa-users-cog', page: 'QuizActivityHistory' });
  }

  return res;
})
const headerOptions = computed(() => {
  const quizSummary = quizSummaryState.quizSummary
  if (!quizSummary) {
    return {};
  }
  const typeDesc = isSurvey.value ? 'Collect Info' : 'Graded Questions';
  const typeIcon = isSurvey.value ? 'fas fa-chart-pie' : 'fas fa-tasks';
  return {
    icon: 'fas fa-spell-check skills-color-subjects',
    title: `${quizSummary.name}`,
    stats: [{
      label: 'Type',
      preformatted: `<div class="h5 font-weight-bold mb-0">${quizSummary.type}</div>`,
      secondaryPreformatted: `<div class="text-secondary text-uppercase text-truncate" style="font-size:0.8rem;margin-top:0.1em;">${typeDesc}</div>`,
      icon: `${typeIcon} skills-color-points`,
    }, {
      label: 'Questions',
      count: quizSummary.numQuestions,
      icon: 'fas fa-graduation-cap skills-color-skills',
    }],
  };
})
const userRoleForDisplay = computed(() => {
  return UserRolesUtil.userRoleFormatter(quizConfig.userQuizRole);
})

// const quizId = ref(route.params.quizId)
const editQuizInfo = ref({
  showDialog: false,
  isEdit: true,
  quizDef: {},
})
function updateEditQuizInfo(quizSummary) {
  editQuizInfo.value.quizDef.quizId = quizSummary.quizId
  editQuizInfo.value.quizDef.name = quizSummary.name
  editQuizInfo.value.quizDef.type = quizSummary.type
  editQuizInfo.value.quizDef.userCommunity = quizSummary.userCommunity
}

function updateQuizDef(quizDef) {
  const origId = route.params.quizId
  if (quizDef.quizId !== origId) {
    editQuizInfo.value.quizDef.quizId = quizDef.quizId
    router.replace({ name: route.name, params: { ...route.params, quizId: quizDef.quizId } })
      .then(() =>{
        focusState.focusOnLastElement()
      })
  } else {
    focusState.focusOnLastElement()
  }
  updateEditQuizInfo(quizDef)
  quizSummaryState.quizSummary.name = quizDef.name
  quizSummaryState.quizSummary.quizId = quizDef.quizId
  quizSummaryState.quizSummary.userCommunity = quizDef.userCommunity
  announcer.polite(`${quizDef.type} named ${quizDef.name} was saved`)
}
</script>

<template>
  <div>
    <PageHeader :loading="isLoading" :options="headerOptions">
      <template #subSubTitle v-if="quizSummaryState.quizSummary">
        <div>
          <div>
            <div v-if="quizSummaryState.quizSummary.userCommunity" class="my-1" data-cy="userCommunity">
              <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
              <span
                class="text-secondary font-italic ml-1">{{ appConfig.userCommunityBeforeLabel }}</span> <span
                class="font-weight-bold text-primary">{{ quizSummaryState.quizSummary.userCommunity }}</span> <span
                class="text-secondary font-italic">{{ appConfig.userCommunityAfterLabel }}</span>
            </div>
          </div>
          <div v-if="!quizConfig.isReadOnlyQuiz" class="mt-2">
            <SkillsButton
                id="editQuizButton"
                @click="editQuizInfo.showDialog = true"
                ref="editQuizButton"
                size="small"
                outlined
                severity="info"
                :track-for-focus="true"
                data-cy="editQuizButton"
                label="Edit"
                icon="fas fa-edit"
                :aria-label="`edit Quiz ${quizSummaryState.quizSummary.name}`">
            </SkillsButton>
            <router-link
                class="ml-1"
                data-cy="quizPreview"
                :to="{ name:'QuizRun', params: { quizId: quizSummaryState.quizSummary.quizId } }"
                target="_blank" rel="noopener" tabindex="-1">
              <SkillsButton
                  target="_blank"
                  v-if="quizSummaryState.quizSummary"
                  outlined
                  severity="info"
                  size="small"
                  label="Preview"
                  icon="fas fa-eye"
                  :aria-label="`Preview Quiz ${quizSummaryState.quizSummary.name}`">
              </SkillsButton>
            </router-link>
          </div>
          <div class="mt-3" v-if="!isLoading">
            <i class="fas fa-user-shield header-status-icon text-primary" aria-hidden="true"/>
            <span class="text-secondary font-italic small mx-1">Role:</span>
            <span class="small text-primary" data-cy="userRole">{{ userRoleForDisplay }}</span>
          </div>
        </div>
      </template>
    </PageHeader>

    <EditQuiz
        v-if="editQuizInfo.showDialog"
        v-model="editQuizInfo.showDialog"
        :quiz="editQuizInfo.quizDef"
        :is-edit="editQuizInfo.isEdit"
        @quiz-saved="updateQuizDef" />

    <Navigation :nav-items="navItems">
    </Navigation>

  </div>
</template>

<style scoped>

</style>