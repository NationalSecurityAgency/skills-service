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
import { useRoute } from 'vue-router'
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import { useFocusState } from '@/stores/UseFocusState.js'
import QuizService from '@/components/quiz/QuizService.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import Column from 'primevue/column'
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue'
import ExistingUserInput from '@/components/utils/ExistingUserInput.vue'
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue'
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'

const announcer = useSkillsAnnouncer()
const route = useRoute()
const userInfo = useUserInfo()
const appConfig = useAppConfig()
const focusState = useFocusState()
const responsive = useResponsiveBreakpoints()

const initialLoad = ref(true)
const userRoles = ref([])
const userIds = ref([])
const removeRoleInfo = ref({
  showDialog: false,
  userInfo: {}
})
const selectedUser = ref(null)

const options = ref({
  busy: true,
  bordered: true,
  outlined: true,
  stacked: 'md',
  sortBy: 'userIdForDisplay',
  sortDesc: true,
  fields: [
    {
      key: 'userIdForDisplay',
      label: 'Quiz Admin',
      sortable: true
    }
  ],
  pagination: {
    server: false,
    currentPage: 1,
    totalRows: 0,
    pageSize: 5,
    possiblePageSizes: [5, 10, 20, 50]
  }
})
const sortInfo = ref({ sortOrder: -1, sortBy: 'userIdForDisplay' })

const userSelected = computed(() => {
  return selectedUser.value && selectedUser.value.userId
})
onMounted(() => {
  loadData()
})

const loadData = () => {
  return QuizService.getQuizUserRoles(route.params.quizId)
    .then((res) => {
      userRoles.value = res
      options.value.pagination.totalRows = userRoles.value.length
      userIds.value = userRoles.value.map((u) => [u.userId, u.userIdForDisplay]).flat()
      options.value.busy = false
    })
    .finally(() => {
      initialLoad.value = false
    })
}
const deleteUserRoleConfirm = (user) => {
  removeRoleInfo.value.userInfo = user
  removeRoleInfo.value.showDialog = true
}
const doDeleteUserRole = () => {
  options.value.busy = true
  const { userIdForDisplay, userId, dn } = removeRoleInfo.value.userInfo
  const pkiAuthenticated = appConfig.isPkiAuthenticated
  const userIdParam = pkiAuthenticated ? dn : userId
  QuizService.deleteQuizAdmin(route.params.quizId, userIdParam)
    .then(() => {
      loadData()
        .finally(() => {
          document.getElementById('existingUserInput').firstElementChild.focus()
          announcer.polite(`Admin ${userIdForDisplay} was removed`)
        })
    })
}
const notCurrentUser = (userId) => {
  return userInfo.userInfo.value && userId !== userInfo.userInfo.value.userId
}
const addUserRole = () => {
  options.value.busy = true
  const { userIdForDisplay, userId, dn } = selectedUser.value
  const pkiAuthenticated = appConfig.isPkiAuthenticated
  const userIdParam = pkiAuthenticated ? dn : userId
  QuizService.addQuizAdmin(route.params.quizId, userIdParam)
    .then(() => {
      selectedUser.value = null
      loadData()
        .then(() => {
          // focusOnTable();
          announcer.polite(`New admin ${userIdForDisplay} was added`)
        })
    })
}
</script>

<template>
  <div>
    <SubPageHeader title="Access" />

    <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
      <template #content>

        <SkillsSpinner :is-loading="initialLoad" />
        <div v-if="!initialLoad">
          <div class="flex py-4 px-2">
            <div class="flex flex-1 px-3">
              <SkillsOverlay :show="options.busy" opacity="0" class="w-full">
                <ExistingUserInput :suggest="true"
                                   :validate="true"
                                   user-type="DASHBOARD"
                                   :excluded-suggestions="userIds"
                                   v-model="selectedUser"
                                   data-cy="existingUserInput" />
              </SkillsOverlay>
            </div>
            <div class="flex flex-0 px-3">
              <SkillsButton @click="addUserRole"
                            icon="fas fa-arrow-circle-right"
                            outlined
                            size="small"
                            data-cy="addUserBtn"
                            id="addUserBtn"
                            aria-label="Add selected user as an admin of this quiz or survey"
                            :disabled="!userSelected || options.busy"
                            :track-for-focus="true"
                            label="Add User">
              </SkillsButton>
            </div>
          </div>
          <SkillsDataTable
            tableStoredStateId="quizAccess"
            :value="userRoles"
            :loading="options.busy"
            show-gridlines
            striped-rows
            paginator
            id="quizUserRoleTable"
            data-cy="quizUserRoleTable"
            :rows="options.pagination.pageSize"
            :rowsPerPageOptions="options.pagination.possiblePageSizes"
            v-model:sort-field="sortInfo.sortBy"
            v-model:sort-order="sortInfo.sortOrder">

            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ userRoles.length
              }}</span>
            </template>

            <Column v-for="col of options.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
                    :class="{'flex': responsive.md.value }">
              <template #header>
                <span class="text-primary"><i class="fas fa-user skills-color-users"
                                              aria-hidden="true"></i> {{ col.label }}</span>
              </template>
              <template #body="slotProps">
                <div v-if="slotProps.field === 'userIdForDisplay'" class="flex flex-row flex-wrap"
                     :data-cy="`quizAdmin_${slotProps.data.userId}`">
                  <div class="flex align-items-start justify-content-start">
                    {{ userInfo.getUserDisplay(slotProps.data, true) }}
                  </div>
                  <div v-if="notCurrentUser(slotProps.data.userId)"
                       class="flex flex-grow-1 align-items-start justify-content-end">
                    <SkillsButton data-cy="removeUserBtn"
                                  :id="`deleteAdmin-${slotProps.data.userId}`"
                                  @click="deleteUserRoleConfirm(slotProps.data)"
                                  icon="fa fa-trash"
                                  size="small"
                                  outlined
                                  :track-for-focus="true"
                                  :aria-label="`remove access role from user ${slotProps.data.userId}`" />
                  </div>
                </div>
              </template>
            </Column>
          </SkillsDataTable>
        </div>
      </template>
    </Card>

    <RemovalValidation
      v-if="removeRoleInfo.showDialog"
      v-model="removeRoleInfo.showDialog"
      @do-remove="doDeleteUserRole"
      :item-name="removeRoleInfo.userInfo.userIdForDisplay"
      item-type="from having admin privileges"
      :enable-return-focus="true">
    </RemovalValidation>
  </div>
</template>

<style scoped>

</style>