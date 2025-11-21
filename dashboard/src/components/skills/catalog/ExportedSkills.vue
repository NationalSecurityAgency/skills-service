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
import { onMounted, ref, watch } from 'vue'
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import { useRoute } from 'vue-router'
import DateCell from '@/components/utils/table/DateCell.vue'
import Column from 'primevue/column'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import ImportedSkillInfo from '@/components/skills/catalog/ImportedSkillInfo.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'
import ExportedSkillRemovalValidation from '@/components/skills/catalog/ExportedSkillRemovalValidation.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue';
import {useStorage} from "@vueuse/core";

const route = useRoute()
const responsive = useResponsiveBreakpoints()
const announcer = useSkillsAnnouncer()

const initialLoad = ref(true)
const reloadData = ref(false)
const data = ref([])
const totalRows = ref(1)
const pageSize = useStorage('exportedSkills-pageSize', 5)
const possiblePageSizes = [5, 10, 15, 25, 50]
const currentPage = ref(1)
const sortInfo = ref({ sortOrder: -1, sortBy: 'exportedOn' })
const initialLoadHadData = ref(false)
const selectedRows = ref([])
const expandedRows = ref([])

onMounted(() => {
  loadData()
})

const loadData = () => {
  reloadData.value = true
  const pageParams = {
    limit: pageSize.value,
    page: currentPage.value,
    orderBy: sortInfo.value.sortBy,
    ascending: sortInfo.value.sortOrder === 1
  }
  return CatalogService.getSkillsExportedToCatalog(route.params.projectId, pageParams).then((res) => {
    if (res.data) {
      data.value = res.data.map((skill) => ({ projectId: route.params.projectId, ...skill }))
      totalRows.value = res.totalCount
      if (totalRows.value > 0) {
        initialLoadHadData.value = true
      }
    } else {
      totalRows.value = 0
      initialLoadHadData.value = false
    }
  }).finally(() => {
    initialLoad.value = false
    reloadData.value = false
  })
}
const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
  currentPage.value = pagingInfo.page + 1
  loadData()
}
watch(sortInfo.value,
  () => {
    currentPage.value = 1
    loadData()
  })

const removalValidation = ref({
  show: false,
  skillToRemove: {}
})
const initiateRemoveCheck = (skillToRemove) => {
  removalValidation.value.skillToRemove = skillToRemove
  removalValidation.value.show = true
}
const doRemoveSkill = () => {
  reloadData.value = true;
  CatalogService.removeExportedSkill(removalValidation.value.skillToRemove.projectId, removalValidation.value.skillToRemove.skillId)
    .then(() => {
      loadData().then(() => {
        announcer.polite(`exported skill ${removalValidation.value.skillToRemove.skillName} has been removed from the skill catalog`)
      });
    });
}
</script>

<template>
  <div>
    <Card :pt="{ 'body': 'p-0 m-0', 'content': 'pt-0'}">
      <template #content>
        <skills-spinner :is-loading="initialLoad" />
        <div v-if="!initialLoad">
          <no-content2
            v-if="!initialLoadHadData"
            class="py-12"
            message="To export to the Skills Catalog please navigate to the Skills page, then select skills to export and click on the Action button located on the top-right above the skills' table."
            title="No skills exported to Skills Catalog"
            data-cy="noExportedSkills"
          />
          <SkillsDataTable
            tableStoredStateId="exportedSkillsTable"
            aria-label="Exported Skills"
            v-if="initialLoadHadData"
            :value="data"
            :loading="reloadData"
            :expander="true"
            v-model:selection="selectedRows"
            v-model:expandedRows="expandedRows"
            v-model:sort-field="sortInfo.sortBy"
            v-model:sort-order="sortInfo.sortOrder"
            stripedRows
            paginator
            lazy
            :totalRecords="totalRows"
            :rows="pageSize"
            @page="pageChanged"
            data-cy="exportedSkillsTable"
            :rowsPerPageOptions="possiblePageSizes">
            <template #header>Exported to Catalog</template>

            <Column field="skillName" header="Skill" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-graduation-cap mr-1" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <div :data-cy="`nameCell_${slotProps.data.skillId}`">
                  <router-link
                    :data-cy="`viewSkillLink_${slotProps.data.skillId}`"
                    :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId }}"
                    :aria-label="`View skill ${slotProps.data.skillName} via link`">
                    <div class="h5 d-inline-block">{{ slotProps.data.skillName }}</div>
                  </router-link>

                  <div class="mt-1">
                    <span>Subject:</span><span class="ml-2">{{ slotProps.data.subjectName }}</span>
                  </div>
                  <div v-if="slotProps.data.groupName" class="mt-1">
                    <span>Group:</span><span class="ml-2">{{ slotProps.data.groupName }}</span>
                  </div>
                </div>
              </template>
            </Column>
            <Column field="importedProjectCount" header="Projects Imported" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-tasks mr-1" aria-hidden="true"></i>
              </template>
            </Column>
            <Column field="exportedOn" header="Exported On" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-clock mr-1" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <date-cell :value="slotProps.data.exportedOn" />
              </template>
            </Column>
            <Column class="md:w-8" :class="{'flex': responsive.md.value }">
              <template #header>
                <span class="sr-only">Controls Heading - Not sortable</span>
              </template>
              <template #body="slotProps">
                <SkillsButton
                  :id="`deleteSkillButton_${slotProps.data.skillId}`"
                  icon="fas fa-trash"
                  @click="initiateRemoveCheck(slotProps.data)"
                  severity="primary"
                  outlined
                  :data-cy="`deleteSkillButton_${slotProps.data.skillId}`"
                  :aria-label="'delete Skill '+slotProps.data.skillName"
                  title="Delete Skill"
                  size="small" />
              </template>
            </Column>

            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
            </template>
            <template #expansion="slotProps">
              <imported-skill-info :skill="slotProps.data"></imported-skill-info>
            </template>
          </SkillsDataTable>
        </div>
      </template>
    </Card>
    <removal-validation
      v-if="removalValidation.show"
      v-model="removalValidation.show"
      :item-name="removalValidation.skillToRemove.skillName"
      item-type="skill"
      @do-remove="doRemoveSkill">
      <exported-skill-removal-validation :skill-to-remove="removalValidation.skillToRemove" />
    </removal-validation>
  </div>
</template>

<style scoped>

</style>