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
import {useRoute} from "vue-router";
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import SkillsService from "@/components/skills/SkillsService.js";
import Column from 'primevue/column';
import InputText from 'primevue/inputtext';
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue';
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import {useStorage} from "@vueuse/core";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

const route = useRoute()
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const announcer = useSkillsAnnouncer()

const tags = ref([])
const isLoading = ref(false)
const sortInfo = ref({ sortOrder: -1, sortBy: 'tagValue' })
const possiblePageSizes = [10, 20, 50, 100]
const tableId = 'skillsTagsTable'
const pageSize = useStorage(`${tableId}-pageSize`, 10)
const filter = ref('')

onMounted(() => loadTags())

const loadTags = () => {
  isLoading.value = true
  SkillsService.getTagsForProject(route.params.projectId)
      .then((data) => {
        tags.value = data;
      })
      .finally(() => {
        isLoading.value = false;
      })
}

const filteredTags = computed(() => {
  if (filter.value && filter.value.toString().trim().length > 0) {
    return tags.value.filter((tag) => tag?.tagValue?.toString().toLowerCase().includes(filter.value.toString().toLowerCase()) )
  }

  return tags.value
})

const numRows = computed(() => filteredTags.value.length)

const clearFilter = () => {
  filter.value = ''
  announcer.polite('Skills filter was reset. Showing all results')
}
const onFilter = (val) => {
  filter.value = val
  announcer.polite(`Filtered by ${val}`)
}

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
}
</script>

<template>
  <div class="w-full">
    <sub-page-header title="Skill Tags" action="Tag"/>
    <Card :pt="{ 'body': 'p-0! m-0!'}">
      <template #content>
        <div class="p-4">
        <InputGroup>
          <InputGroupAddon>
            <i class="fas fa-search" aria-hidden="true"/>
          </InputGroupAddon>
          <InputText
              class="flex grow"
              v-model="filter"
              data-cy="tagsTable-skillFilter"
              aria-label="Tag Filter"
              placeholder="Tag Filter"/>
          <InputGroupAddon class="p-0 m-0">
            <SkillsButton
                id="tagFilterClearBtn"
                icon="fa fa-times"
                text
                outlined
                @click="clearFilter"
                aria-label="Reset filter"
                data-cy="filterResetBtn"/>
          </InputGroupAddon>
        </InputGroup>
        </div>
        <SkillsDataTable
            :tableStoredStateId="tableId"
            aria-label="Skill Tags"
            :value="filteredTags"
            v-model:sort-field="sortInfo.sortBy"
            v-model:sort-order="sortInfo.sortOrder"
            paginator
            :rows="pageSize"
            :rowsPerPageOptions="possiblePageSizes"
            @page="pageChanged"
        >
          <Column field="tagValue" header="Tag" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fa-solid fa-tag mr-1" :class="colors.getTextClass(0)" aria-hidden="true"></i>
            </template>
          </Column>
          <Column field="numSkills" header="# Skills" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fa-solid fa-graduation-cap mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i>
            </template>
          </Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numRows }}</span>
          </template>
        </SkillsDataTable>
      </template>
    </Card>
  </div>
</template>
