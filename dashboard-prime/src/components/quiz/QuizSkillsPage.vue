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
import { FilterMatchMode } from 'primevue/api'
import { useRoute } from 'vue-router'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import InputGroupAddon from 'primevue/inputgroupaddon'
import Column from 'primevue/column'
import QuizService from '@/components/quiz/QuizService.js'
import InputText from 'primevue/inputtext'
import NoContent2 from '@/components/utils/NoContent2.vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import InputGroup from 'primevue/inputgroup'
import LoadingContainer from '@/components/utils/LoadingContainer.vue'

const route = useRoute()
const userInfo = useUserInfo()
const appConfig = useAppConfig()
const responsive = useResponsiveBreakpoints()

const quizType = ref('')
const skills = ref([])
const totalRows = ref(0)
const filtering = ref(false)
const options = ref({
  busy: true,
  bordered: true,
  outlined: true,
  stacked: 'md',
  sortBy: 'projectId',
  sortDesc: true,
  tableDescription: 'Skills Associated with this Quiz/Survey',
  fields: [
    {
      key: 'projectId',
      label: 'Project Id',
      imageClass: 'fas fa-list-alt skills-color-users',
      sortable: true
    },
    {
      key: 'skillName',
      label: 'Skill',
      imageClass: 'fas fa-graduation-cap skills-color-points',
      sortable: true
    }
  ],
  pagination: {
    server: false,
    currentPage: 1,
    totalRows: 1,
    pageSize: 5,
    possiblePageSizes: [5, 10, 15, 20]
  }
})
const sortInfo = ref({ sortOrder: -1, sortBy: 'projectId' })

const docsUrl = computed(() => {
  return `${appConfig.docsHost}/dashboard/user-guide/quizzes-and-surveys.html#skill-association`
})

onMounted(() => {
  loadData()
})

const loadData = () => {
  options.value.busy = true
  QuizService.getSkillsForQuiz(route.params.quizId, userInfo.userInfo.value.userId)
    .then((result) => {
      if (result) {
        skills.value = result
        totalRows.value = skills.value.length
        options.value.busy = false
      }
    }).finally(() => {
    options.value.busy = false
  })
}

const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS }
})

const clearFilter = () => {
  filters.value.global.value = null
  filtering.value = false
}
const onFilter = (filterEvent) => {
  totalRows.value = filterEvent.filteredValue.length
  filtering.value = true
}

</script>

<template>
  <div>
    <SubPageHeader title="Associated Skills" />
    <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
      <template #content>
        <LoadingContainer v-bind:is-loading="options.busy">
          <div v-if="skills.length > 0 && !options.busy">
            <SkillsDataTable
              tableStoredStateId="quizSkills"
              :value="skills"
              :loading="options.busy"
              show-gridlines
              striped-rows
              paginator
              data-cy="quizSkills"
              v-model:filters="filters"
              :globalFilterFields="['skillName']"
              @filter="onFilter"
              :rows="options.pagination.pageSize"
              :rowsPerPageOptions="options.pagination.possiblePageSizes"
              v-model:sort-field="sortInfo.sortBy"
              v-model:sort-order="sortInfo.sortOrder">
              <template #header>
                <div class="flex gap-1">
                  <InputGroup>
                    <InputGroupAddon>
                      <i class="fas fa-search" aria-hidden="true" />
                    </InputGroupAddon>
                    <InputText class="flex flex-grow-1"
                               v-model="filters['global'].value"
                               data-cy="quiz-skillNameFilter"
                               placeholder="Skill Filter"
                               aria-label="Skill name filter" />
                    <InputGroupAddon class="p-0 m-0">
                      <SkillsButton
                        icon="fa fa-times"
                        text
                        outlined
                        @click="clearFilter"
                        aria-label="Reset surveys and quizzes filter"
                        data-cy="clearFilterBtn" />
                    </InputGroupAddon>
                  </InputGroup>
                </div>
              </template>

              <template #paginatorstart>
                <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows
                }}</span>
              </template>

              <template #empty>
                <div class="flex justify-content-center flex-wrap h-12rem">
                  <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle fa-3x"
                     aria-hidden="true"></i>
                  <span class="w-full">
                      <span class="flex align-items-center justify-content-center">There are no records to show</span>
                      <span v-if="filtering" class="flex align-items-center justify-content-center">  Click
                        <SkillsButton class="flex flex align-items-center justify-content-center px-1"
                                      label="Reset"
                                      link
                                      size="small"
                                      @click="clearFilter"
                                      :aria-label="`Reset filter for ${quizType} results`"
                                      data-cy="clearFilterBtn2" /> to clear the existing filter.
                      </span>
                    </span>
                </div>
              </template>
              <Column v-for="col of options.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
                      :class="{'flex': responsive.md.value }">
                <template #header>
                  <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
                </template>
                <template #body="slotProps">
                  <div v-if="slotProps.field === 'projectId'">
                    <div v-if="slotProps.data.canUserAccess">
                      <RouterLink :aria-label="`manage project ${slotProps.data.projectId }`"
                                  :to="{ name:'Subjects', params: { projectId: slotProps.data.projectId  }}"
                                  class="text-info mb-0 pb-0 preview-card-title" :title="`${slotProps.data.projectId}`"
                                  role="link"
                      >
                        {{ slotProps.data.projectId }}
                      </RouterLink>
                    </div>
                    <div v-else>
                      {{ slotProps.data.projectId }}
                    </div>
                  </div>
                  <div v-else-if="slotProps.field === 'skillName'">
                    <RouterLink v-if="slotProps.data.canUserAccess"
                                role="link"
                                tag="a" :to="{ name:'SkillOverview',
                              params: { projectId: slotProps.data.projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId }}"
                                :aria-label="`Manage skill ${slotProps.data.skillName} via link`">
                      <HighlightedValue :value="slotProps.data.skillName" :filter="filters.global.value" />
                    </RouterLink>
                    <div v-else>
                      <HighlightedValue :value="slotProps.data.skillName" :filter="filters.global.value" />
                    </div>
                    <div class="text-secondary" style="font-size: 0.9rem;">ID: {{ slotProps.data.skillId }}</div>
                  </div>
                  <div v-else>
                    <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ slotProps.data[col.key] }}</span>
                  </div>
                </template>
              </Column>
            </SkillsDataTable>
          </div>
          <NoContent2 v-else title="No Skills Associated Yet..." icon="fas fa-award" class="p-5">
            There are currently no skills associated with this quiz/survey.
            You can learn more about how to add skills to a quiz/survey in the documentation
            <a aria-label="SkillTree documentation of associating skills to quizzes"
               :href="docsUrl" target="_blank" style="display: inline-block">
              here.
            </a>
          </NoContent2>
        </LoadingContainer>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>