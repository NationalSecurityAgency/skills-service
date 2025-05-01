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

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router';
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import { useAdminGroupState } from '@/stores/UseAdminGroupState.js';
import { userErrorState } from '@/stores/UserErrorState.js';
import { useUpgradeInProgressErrorChecker } from '@/components/utils/errors/UseUpgradeInProgressErrorChecker.js';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import AdminGroupsService from '@/components/access/groups/AdminGroupsService.js';
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import Column from 'primevue/column';
import NoContent2 from '@/components/utils/NoContent2.vue';
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';

const route = useRoute()
const userInfo = useUserInfo();
const adminGroupState = useAdminGroupState()
const errorState = userErrorState()
const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()

const isLoading = ref(true)
const availableQuizzes = ref([])
const assignedQuizzes = ref([])

const removeQuizInfo = ref({
  showDialog: false,
  quiz: {}
})

const adminGroupId = computed(() => route.params.adminGroupId)

const areQuizzesAvailable = computed(() => {
  return availableQuizzes.value && availableQuizzes.value.length > 0;
})
const areQuizzesAssigned = computed(() => {
  return assignedQuizzes.value && assignedQuizzes.value.length > 0;
})
const emptyMessage = computed(() => {
  if (areQuizzesAvailable.value) {
    return 'No results. Please refine your search string.'
  } else {
    if (areQuizzesAssigned.value) {
      return 'All of your available quizzes and surveys have already been assigned to this admin group.'
    }
    return 'You currently do not administer any quizzes or surveys.'
  }
})
const errNotification = ref({
  enable: false,
  msg: '',
});

onMounted(() => {
  loadData()
})
const loadData = () => {
  isLoading.value = true
  AdminGroupsService.getAdminGroupQuizzes(adminGroupId.value)
      .then((res) => {
        availableQuizzes.value = res.availableQuizzes;
        assignedQuizzes.value = res.assignedQuizzes;
      }).finally(() => {
    isLoading.value = false
  });
}
const addQuizToAdminGroup = (quiz) => {
  isLoading.value = true
  AdminGroupsService.addQuizToAdminGroup(adminGroupId.value, quiz.quizId)
    .then((res) => {
      availableQuizzes.value = res.availableQuizzes;
      assignedQuizzes.value = res.assignedQuizzes;
      adminGroupState.adminGroup.numberOfQuizzesAndSurveys++;
    }).catch((e) => {
      handleError(e);
    }).finally(() => {
      isLoading.value = false
    });
}
const removeQuizFromAdminGroupConfirm = (quiz) => {
  removeQuizInfo.value.quiz = quiz
  removeQuizInfo.value.showDialog = true
}

const removeQuizFromAdminGroup = () => {
  isLoading.value = true
  const { quizId } = removeQuizInfo.value.quiz
  AdminGroupsService.removeQuizFromAdminGroup(adminGroupId.value, quizId)
      .then((res) => {
        availableQuizzes.value = res.availableQuizzes;
        assignedQuizzes.value = res.assignedQuizzes;
        adminGroupState.adminGroup.numberOfQuizzesAndSurveys--;
      }).finally(() => {
    isLoading.value = false
  });
}
const handleError = (e) => {
  if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'AccessDenied') {
    errNotification.value.msg = e.response.data.explanation;
    errNotification.value.enable = true;
  } else if (upgradeInProgressErrorChecker.isUpgrading(e)) {
    upgradeInProgressErrorChecker.navToUpgradeInProgressPage()
  } else {
    const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
    errorState.navToErrorPage('Failed to add Quiz to Admin Group', errorMessage)
  }
}
const clearErrorMessage = () => {
  errNotification.value.msg = '';
  errNotification.value.enable = false;
}
</script>

<template>
  <sub-page-header title="Group Quizzes and Surveys" />

  <Card :pt="{ body: { class: 'p-0!' } }">
    <template #content>
      <loading-container :is-loading="isLoading" class="">
        <div class="w-full px-4 py-6">
          <SkillsDropDown
              name="associatedQuiz"
              data-cy="quizSelector"
              aria-label="Select Quiz/Survey to add to Admin Group"
              showClear
              filter
              optionLabel="name"
              @update:modelValue="addQuizToAdminGroup"
              :emptyMessage=emptyMessage
              :isRequired="true"
              :options="availableQuizzes">
            <template #value="slotProps">
              <div v-if="slotProps.value" class="p-1" :data-cy="`quizSelected-${slotProps.value.quizId}`">
                <span class="text-secondary">{{ slotProps.value.type }}:</span><span class="ml-1">{{ slotProps.value.name }}</span>
              </div>
              <span v-else> Search available quizzes and surveys...</span>
            </template>
            <template #option="slotProps">
              <div :data-cy="`availableQuizSelection-${slotProps.option.quizId}`">
                <span class="text-secondary">{{ slotProps.option.type }}:</span><span class="h6 ml-2">{{ slotProps.option.name }}</span>
              </div>
            </template>
          </SkillsDropDown>
        </div>
        <Message v-if="errNotification.enable" @close="clearErrorMessage" severity="error" data-cy="error-msg">
          <strong>Error!</strong> Request could not be completed! {{ errNotification.msg }}
        </Message>
        <div v-if="assignedQuizzes && assignedQuizzes.length > 0">
          <SkillsDataTable
              :loading="isLoading"
              tableStoredStateId="adminGroupQuizzesTable"
              aria-label="Admin Group Quizzes and Surveys"
              :value="assignedQuizzes"
              paginator
              :rows="5"
              :totalRecords="assignedQuizzes.length"
              :rowsPerPageOptions="[5, 10, 15, 20]"
              data-cy="adminGroupQuizzesTable">
            <Column header="Name" field="name" style="width: 40%;" :sortable="true">
              <template #body="slotProps">
                <router-link :id="slotProps.data.quizId" :to="{ name:'QuizOverview',
                    params: { quizId: slotProps.data.quizId }}"
                             class="btn btn-sm btn-outline-hc ml-2" :data-cy="`manage_${slotProps.data.quizId}`">
                  {{ slotProps.data.name }}
                </router-link>
              </template>
            </Column>
            <Column header="Delete">
              <template #body="slotProps">
                <SkillsButton v-on:click="removeQuizFromAdminGroupConfirm(slotProps.data)" size="small"
                              :id="`removeQuiz_${slotProps.data.quizId}`"
                              :track-for-focus="true"
                              :data-cy="`removeQuiz_${slotProps.data.quizId}`" icon="fas fa-trash" label="Delete"
                              :aria-label="`remove quiz on ${slotProps.data.quizId} from admin group`">
                </SkillsButton>
              </template>
            </Column>

            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ assignedQuizzes.length }}</span>
            </template>
          </SkillsDataTable>
        </div>

        <no-content2 v-else title="No Quizzes or Surveys Added Yet..." icon="fas fa-spell-check" class="py-8">
          <div>
            <p>
              Please use the drop-down above to start adding quizzes and surveys to this admin group!
            </p>
            <p>
              When a quiz or survey is assigned to a group, group's members automatically gain administrative privileges of that project, streamlining management.
            </p>
          </div>
        </no-content2>
      </loading-container>
    </template>
  </Card>

  <RemovalValidation
      v-if="removeQuizInfo.showDialog"
      v-model="removeQuizInfo.showDialog"
      @do-remove="removeQuizFromAdminGroup"
      :item-name="removeQuizInfo.quiz.name"
      removalTextPrefix="This will remove the "
      :item-type="`quiz from this admin group.  All members of this admin group other than ${userInfo.userInfo.value.userIdForDisplay} will lose admin access to this quiz`"
      :enable-return-focus="true">
  </RemovalValidation>
</template>

<style scoped>

</style>