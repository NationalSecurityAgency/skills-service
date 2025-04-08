/*
Copyright 2025 SkillTree

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
import NoContent2 from "@/components/utils/NoContent2.vue";
import UserCommentsAdminService from "@/components/userComments/UserCommentsAdminService.js";
import {useRoute} from "vue-router";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import SkillsTable from "@/components/skills/SkillsTable.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import Column from "primevue/column";
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import UserComment from "@/skills-display/components/communication/UserComment.vue";
import InputText from "primevue/inputtext";

const route = useRoute()
const responsive = useResponsiveBreakpoints()
const colors = useColors()

const sortInfo = ref({sortOrder: -1, sortBy: 'created'})
const pagination = ref({
  currentPage: 1,
  totalRows: 0,
  pageSize: 10,
  possiblePageSizes: [10, 20, 50]
})
const isInitialLoading = ref(true)
const isTableReloading = ref(false)
const comments = ref([])
const hasComments = computed(() => comments.value.length > 0 && !isInitialLoading.value)
const loadComments = () => {
  isTableReloading.value = true
  const params = {
    limit: pagination.value.pageSize,
    ascending: sortInfo.value.sortOrder === 1,
    page: pagination.value.currentPage,
    orderBy: sortInfo.value.sortBy
  }

  return UserCommentsAdminService.getUserComments(route.params.projectId, params)
      .then(res => {
        comments.value = res.data
        // expand all the rows
        expandedRows.value = comments.value.reduce((acc, comment) => ({ ...acc, [comment.threadId]: true }), {})
      })
      .finally(() => isTableReloading.value = false)
}
onMounted(() => {
  loadComments().then(() => isInitialLoading.value = false)
})

const pageChanged = (pagingInfo) => {
  pagination.value.pageSize = pagingInfo.rows
  pagination.value.currentPage = pagingInfo.page + 1
  return loadComments()
}
const sortField = (column) => {
  // set to the first page
  pagination.value.currentPage = 1
  return loadComments()
}

const expandedRows = ref([]);

const usernameFilter = ref('')
const skillnameFilter = ref('')
const needsResponseOnlyFilter = ref(false)
const reloadTable = () => {

}
const reset = () => {}
</script>

<template>
  <div>
    <skills-spinner v-if="isInitialLoading" :is-loading="isInitialLoading" class="my-20"/>
    <no-content2
        v-if="!hasComments"
        title="No User Comments"
        message="When a user makes a comment, it will appear here." class="my-8"/>

    <div class="py-4 px-5 flex flex-col gap-4 mb-3">
      <div class="flex gap-3">
        <div class="flex flex-col gap-2 flex-1">
          <label for="skill-name-filter">Skill Name Filter:</label>
          <InputText class="w-full"
                     v-model="skillnameFilter"
                     id="skill-name-filter"
                     data-cy="skillnameFilter"
                     @keydown.enter="reloadTable"
                     aria-label="Skill name filter"/>
        </div>
        <div class="flex flex-col gap-2 flex-1">
          <label for="user-name-filter">User Name Filter:</label>
          <InputText class="w-full"
                     v-model="usernameFilter"
                     id="user-name-filter"
                     data-cy="usernameFilter"
                     @keydown.enter="reloadTable"
                     aria-label="User name filter"/>
        </div>
      </div>
      <div>
        <div class="flex items-center gap-2">
          <Checkbox v-model="needsResponseOnlyFilter" inputId="needsResponseInput" name="needsResponse" value="false" />
          <label for="needsResponseInput"> <Tag>Needs Response</Tag> Only </label>
        </div>
      </div>
      <div class="flex gap-1">
        <SkillsButton size="small" aria-label="Filter" @click="reloadTable" data-cy="filterBtn"
                      icon="fa fa-filter" label="Filter"/>
        <SkillsButton size="small" aria-label="Reset" @click="reset" class="ml-1"
                      data-cy="resetBtn" icon="fa fa-times" label="Reset"/>
      </div>
    </div>

    <SkillsDataTable
        :showHeaders="false"
        v-if="hasComments"
        table-stored-state-id="userCommentsTable"
        aria-label="User Comments Table"
        :value="comments"
        :loading="isTableReloading"
        striped-rows
        lazy
        paginator
        data-cy="quizRunsToGradeTable"
        @page="pageChanged"
        @sort="sortField"
        v-model:expandedRows="expandedRows"
        :rows="pagination.pageSize"
        :rowsPerPageOptions="pagination.possiblePageSizes"
        :total-records="pagination.totalRows"
        dataKey="threadId"
        v-model:sort-field="sortInfo.sortBy"
        v-model:sort-order="sortInfo.sortOrder"
    >
      <Column expander style="display: none;" :showFilterMenu="false" />
      <Column header="Skill" field="skillId" :sortable="true" :class="{'flex': responsive.md.value }">
        <template #header>
          <div class="mr-2"><i class="far fa-calendar-alt" :class="colors.getTextClass(2)"
                               aria-hidden="true"></i></div>
        </template>
        <template #body="slotProps">
          <span class="text-gray-600 italic">Skill: </span>{{ slotProps.data.skillName }}
          <Tag class="ml-2">Needs Response</Tag>
        </template>
      </Column>
      <Column header="Date" field="created" :sortable="true" :class="{'flex': responsive.md.value }">
        <template #header>
          <div class="mr-2"><i class="far fa-calendar-alt" :class="colors.getTextClass(2)"
                               aria-hidden="true"></i></div>
        </template>
        <template #body="slotProps">
          <DateCell :value="slotProps.data.completed"/>
        </template>
      </Column>

      <template #expansion="slotProps">
        <div class="px-8 pt-3">
          <user-comment :comment="slotProps.data" :can-edit="false" reply-label="Reply"/>
        </div>
      </template>


    </SkillsDataTable>

  </div>
</template>

<style scoped>

</style>