/*
Copyright 2026 SkillTree

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

import {computed, onMounted, ref} from "vue";
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import UsersService from "@/components/users/UsersService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import InputText from "primevue/inputtext";
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import {useStorage} from "@vueuse/core";
import Column from "primevue/column";
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useUserInfo} from "@/components/utils/UseUserInfo.js";
import ProgressBar from "primevue/progressbar";
import OverallMetricsCards from "@/components/utils/cards/OverallMetricsCards.vue";
import SingleUserOverallProgress from "@/components/users/SingleUserOverallProgress.vue";
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import HighlightedValue from "@/components/utils/table/HighlightedValue.vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const colors = useColors()
const responsive = useResponsiveBreakpoints()
const userInfo = useUserInfo()
const appConfig = useAppConfig()

onMounted(() => {
  loadData().then(() => {
    hasData.value = userProgress.value?.numTotalMetricItems > 0
  })
})

const userProgress = ref({})
const hasData = ref(false)
const isLoading = ref(true)
const isTableLoading = ref(false)
const totalRows = ref(0)
const currentPage = ref(1)
const pageSize = useStorage('usersOverallProgressTable-pageSize', 10)
const possiblePageSizes = [10, 20, 50, 100]
const sortInfo = ref({sortOrder: -1, sortBy: 'skillsAccomplished'})
const expandedRows = ref([])

const loadData = () => {
  isTableLoading.value = true
  const orderBy = sortInfo.value.sortBy === appConfig.usersTableAdditionalUserTagKey ? 'userTag' : sortInfo.value.sortBy
  const params = {
    limit: pageSize.value,
    ascending: sortInfo.value.sortOrder === 1,
    page: currentPage.value,
    orderBy,
    userQuery: filters.value.user,
    userTagFilter: filters.value.userTag,
  }
  return UsersService.getGlobalUserProgress(params).then((data) => {
    const metricItemsPage = data.metricItemsPage.map((item) => ({
      ...item,
      skillsEarnedPercent: Math.trunc((item.numSkillsEarned / data.numTotalSkills) * 100),
      userToShow: userInfo.getUserDisplay(item, true)
    }))
    userProgress.value = {...data, metricItemsPage}
    totalRows.value = data.numTotalMetricItems
  }).finally(() => {
    isLoading.value = false
    isTableLoading.value = false
  })
}

const filters = ref({
  user: '',
  userTag: '',
})
const applyFilters = () => {
  loadData()
}
const resetFilters = () => {
  filters.value.user = ''
  filters.value.userTag = ''
  loadData()
}
const sortField = () => {
  // set to the first page
  currentPage.value = 1
  loadData()
}
const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
  currentPage.value = pagingInfo.page + 1
  loadData()
}

const showUserTagColumn = computed(() => {
  return !!(appConfig.usersTableAdditionalUserTagKey && appConfig.usersTableAdditionalUserTagLabel);
})
</script>

<template>
  <div>
    <SubPageHeader title="Users Progress" :title-level="1" />
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-6"/>
    <div v-if="!isLoading">
      <OverallMetricsCards :data="userProgress" />
      <Card v-if="hasData"
            :pt="{ body: { class: 'p-0!' } }">
        <template #content>
          <div class="flex flex-col gap-2 p-5">
            <div class="flex gap-3">
              <div class="flex-1 flex flex-col gap-1">
                <div>
                  <label for="userFilter">User Filter</label>
                </div>
                <InputText id="userFilter"
                           v-model="filters.user"
                           v-on:keydown.enter="applyFilters"
                           :disabled="isTableLoading"
                           class="w-full"
                           data-cy="users-skillIdFilter"
                           aria-label="user filter"/>
              </div>
              <div v-if="showUserTagColumn" class="flex-1 flex flex-col gap-1">
                <div>
                  <label for="userTagFilter">{{ appConfig.usersTableAdditionalUserTagLabel }} Filter</label>
                </div>
                <InputText id="userTagFilter" v-model="filters.userTag" v-on:keydown.enter="applyFilters"
                           class="w-full"
                           data-cy="users-userTagFilter" aria-label="user tag filter"/>
              </div>
            </div>
            <div class="flex gap-2">
              <SkillsButton icon="fa fa-filter"
                            label="Filter"
                            :disabled="isTableLoading"
                            outlined @click="applyFilters"
                            data-cy="users-filterBtn"
                            size="small"/>
              <SkillsButton icon="fa fa-times"
                            label="Reset"
                            :disabled="isTableLoading"
                            outlined
                            @click="resetFilters"
                            data-cy="users-resetBtn" size="small"/>
            </div>
          </div>

          <div class="mt-3">
            <SkillsDataTable
                :value="userProgress.metricItemsPage"
                :loading="isTableLoading"
                size="small"
                stripedRows showGridlines paginator lazy
                :totalRecords="totalRows"
                :rows="pageSize"
                @page="pageChanged"
                :expander="true"
                v-model:expandedRows="expandedRows"
                tableStoredStateId="userOverallProgressTable"
                data-cy="userOverallProgressTable"
                aria-label="Users"
                :rowsPerPageOptions="possiblePageSizes"
                v-model:sort-field="sortInfo.sortBy"
                v-model:sort-order="sortInfo.sortOrder"
                @sort="sortField"
                data-key="userId"
                :auto-max-width="false"
            >
              <Column field="userIdForDisplay" header="User" :sortable="true" :class="{'flex': responsive.lg.value }">
                <template #header>
                  <i class="fa-solid fa-user mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  <highlighted-value :value="slotProps.data.userToShow" :filter="filters.user"/>
                </template>
              </Column>
              <Column v-if="showUserTagColumn"
                      :field="appConfig.usersTableAdditionalUserTagKey"
                      :header="appConfig.usersTableAdditionalUserTagLabel"
                      :sortable="true"
                      :class="{'flex': responsive.lg.value }">
                <template #header>
                  <i class="fas fa-tag mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  <highlighted-value :value="slotProps.data.userTag" :filter="filters.userTag"/>
                </template>
              </Column>

              <Column field="numSkillsEarned" header="Skills & Projects" :sortable="true" :class="{'flex': responsive.lg.value }">
                <template #header>
                  <i class="fa-solid fa-tasks mr-1" :class="colors.getTextClass(3)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  <div class="flex flex-col">
                    <div class="flex gap-2">
                      <div class="text-primary flex-1"
                           :aria-label="`${slotProps.data.skillsEarnedPercent} percent completed`"
                           data-cy="progressPercent">{{ slotProps.data.skillsEarnedPercent }}%
                      </div>
                      <div
                          :aria-label="`${slotProps.data.numSkillsEarned} out of ${userProgress.numTotalSkills} total skills`"
                      ><span class="text-primary">{{ slotProps.data.numSkillsEarned }}</span> / {{ userProgress.numTotalSkills }} Skills</div>
                    </div>
                    <ProgressBar style="height: 5px;" :value="slotProps.data.skillsEarnedPercent" :showValue="false"
                                 class="lg:min-w-[12rem] xl:min-w-[20rem]"
                                 :aria-label="`Progress for ${slotProps.data.userId} user`" />
                  </div>
                  <div class="flex flex-col gap-1 mt-2">
                    <div class="flex gap-1 flex-wrap items-center"><div>Started:</div>
                      <div>
                        <Tag>{{ slotProps.data.numProjects }}</Tag>/ {{ userProgress.numTotalProjects }} Projects
                      </div>
                    </div>
                  </div>
                </template>
              </Column>

              <Column field="numQuizAttempts" header="# Quiz Runs" :sortable="true" :class="{'flex': responsive.lg.value }">
                <template #header>
                  <i class="fa-solid fa-spell-check mr-1" :class="colors.getTextClass(4)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  <div class="flex pr-2 flex-wrap">
                    <div class="flex flex-col gap-2 flex-1">
                      <div class="flex-1"><Tag severity="secondary">{{ slotProps.data.numQuizAttempts }}</Tag></div>
                    </div>
                    <div v-if="slotProps.data.numQuizAttempts > 0">
                      <div class="flex gap-2">
                        <div class="flex-1">Passed:</div><div>{{ slotProps.data.numQuizzesPassed}}</div>
                      </div>
                      <div class="flex gap-2">
                        <div class="flex-1">Failed:</div><div>{{ slotProps.data.numQuizzesFailed}}</div>
                      </div>
                      <div class="flex gap-1" v-if="slotProps.data.numQuizzesInProgress > 0">
                        <div class="flex-2">In Progress:</div><div>{{ slotProps.data.numQuizzesInProgress}}</div>
                      </div>
                    </div>
                  </div>
                </template>
              </Column>

              <Column field="numSurveys" header="# Survey Runs" :sortable="true" :class="{'flex': responsive.lg.value }">
                <template #header>
                  <i class="fa-solid fa-clipboard-question mr-1" :class="colors.getTextClass(5)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  <div class="flex flex-col gap-2 mb-2">
                    <div class="flex-1"><Tag>{{ slotProps.data.numSurveys }}</Tag> / {{ userProgress.numTotalSurveys }}</div>
                  </div>
                </template>
              </Column>

              <Column field="numBadgesEarned" header="# Badges" :sortable="true" :class="{'flex': responsive.lg.value }">
                <template #header>
                  <i class="fa-solid fa-award mr-1" :class="colors.getTextClass(6)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  <Tag>{{ slotProps.data.numBadgesEarned }}</Tag> / {{ userProgress.numTotalBadges}}
                  <div>Global Badges: {{ slotProps.data.numGlobalBadgesEarned }}</div>
                </template>
              </Column>

              <template #expansion="slotProps">
                <single-user-overall-progress :user-progress-meta="slotProps.data" />
              </template>
              <template #empty>
                <table-no-res :showResetFilter="true" @resetFilter="resetFilters"/>
              </template>

            </SkillsDataTable>
          </div>
        </template>
      </Card>
      <no-content2 v-if="!hasData" class="mt-6"
                   title="No User Data Available"
                   message="Cross-project user activity and progress data will display here once users start completing skills and taking quizzes across your administered projects"></no-content2>
    </div>
  </div>
</template>

<style scoped>

</style>