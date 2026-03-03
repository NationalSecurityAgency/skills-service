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

const colors = useColors()
const responsive = useResponsiveBreakpoints()
const userInfo = useUserInfo()

onMounted(() => {
  loadData()
})

const userProgress = ref({})
const hasData = computed(() => userProgress.value?.numTotalMetricItems > 0)
const isLoading = ref(true)
const isTableLoading = ref(false)
const totalRows = ref(0)
const currentPage = ref(1)
const pageSize = useStorage('usersOverallProgressTable-pageSize', 10)
const possiblePageSizes = [10, 20, 50, 100]
const sortInfo = ref({sortOrder: -1, sortBy: 'skillsAccomplished'})
const expandedRows = ref([])

const loadData = () => {
  return UsersService.getGlobalUserProgress().then((data) => {
    const metricItemsPage = data.metricItemsPage.map((item) => ({
      ...item,
      skillsEarnedPercent: Math.trunc((item.numSkillsEarned / data.numTotalSkills) * 100)
    }))
    userProgress.value = {...data, metricItemsPage}
    totalRows.value = data.numTotalMetricItems
  }).finally(() => {
    isLoading.value = false
  })
}

const filters = ref({
  user: '',
})
const applyFilters = () => {

}
const resetFilters = () => {

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

const totalQuizzesAndSurveys = computed(() => {
  return userProgress.value ? userProgress.value.numTotalQuizzes + userProgress.value.numTotalSurveys : 0
})
</script>

<template>
  <div>
    <SubPageHeader title="Users Progress" :title-level="1">
    </SubPageHeader>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-6"/>
    <div v-if="!isLoading">
      <OverallMetricsCards :data="userProgress" />
      <Card v-if="hasData"
            :pt="{ body: { class: 'p-0!' } }">
        <template #content>
          <div class="flex flex-col gap-2 p-5">
            <div class="flex flex-col gap-1">
              <div>
                <label for="userFilter">User Filter</label>
              </div>
              <InputText id="userFilter" v-model="filters.user" v-on:keydown.enter="applyFilters"
                         class="w-full"
                         data-cy="users-skillIdFilter" aria-label="user filter"/>
            </div>
            <div class="flex gap-2">
              <SkillsButton icon="fa fa-filter" label="Filter" outlined @click="applyFilters" data-cy="users-filterBtn"
                            size="small"/>
              <SkillsButton icon="fa fa-times" label="Reset" outlined @click="resetFilters" class="ml-1"
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
                tableStoredStateId="usersTable" data-cy="usersTable"
                aria-label="Users"
                :rowsPerPageOptions="possiblePageSizes"
                v-model:sort-field="sortInfo.sortBy"
                v-model:sort-order="sortInfo.sortOrder"
                @sort="sortField"
                data-key="userId"
            >
              <Column field="userId" header="User" :sortable="true" :class="{'flex': responsive.md.value }">
                <template #header>
                  <i class="fas fa-user skills-color-users mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  {{ userInfo.getUserDisplay(slotProps.data, true) }}
                </template>
              </Column>

              <Column field="numSkillsEarned" header="Skills & Projects" :sortable="true" :class="{'flex': responsive.md.value }">
                <template #header>
                  <i class="fas fa-user skills-color-users mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
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
                    <div>Started:
                      <Tag>{{ slotProps.data.numProjects }}</Tag>
                      / {{ userProgress.numTotalProjects }} Projects
                    </div>
                  </div>
                </template>
              </Column>

              <Column field="numQuizzes" header="# Quizzes/Surveys" :sortable="true" :class="{'flex': responsive.md.value }">
                <template #header>
                  <i class="fas fa-user skills-color-users mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  {{ slotProps.data.numQuizzes }} / {{ userProgress.numTotalQuizzes }}
                </template>
              </Column>

              <Column field="numBadgesEarned" header="# Badges" :sortable="true" :class="{'flex': responsive.md.value }">
                <template #header>
                  <i class="fas fa-user skills-color-users mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  {{ slotProps.data.numBadgesEarned }} / {{ userProgress.numTotalBadges}}
                </template>
              </Column>

              <template #expansion="slotProps">
                <pre>{{ slotProps.data }}</pre>
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