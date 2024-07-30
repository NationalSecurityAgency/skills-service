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
import { onMounted, ref, computed } from 'vue'
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import DateCell from '@/components/utils/table/DateCell.vue'
import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog.vue'

const props = defineProps({
  skill: Object
})

const isLoading = ref(true)
const importedProjects = ref([])

onMounted(() => {
  loadImportedProjectDetails()
})
const loadImportedProjectDetails = () => {
  if (props.skill.importedProjectCount > 0) {
    isLoading.value = true
    CatalogService.getExportedStats(props.skill.projectId, props.skill.skillId)
      .then((res) => {
        importedProjects.value = res.users
      }).finally(() => {
      isLoading.value = false
    })
  } else {
    isLoading.value = false
  }
}

const isEmailEnabled = computed(() => {
  return true
})

const contactDialog = ref({
  show: false,
  projectId: null,
  projectName: null
})
const contactProjAdmins = (projInfo) => {
  contactDialog.value.projectId = projInfo.importingProjectId
  contactDialog.value.projectName = projInfo.importingProjectName
  contactDialog.value.show = true
}
</script>

<template>
  <div :data-cy="`importSkillInfo-${skill.projectId}_${skill.skillId}`" class="ml-5">

    <div v-if="skill.importedProjectCount > 0">
      <SkillsDataTable
        tableStoredStateId="importSkillInfo"
        :value="importedProjects"
        sortField="skillName"
        :sortOrder="1"
        aria-label="Imported Skills"
        data-cy="importedSkillsTable">
        <Column field="importingProjectName" header="Importing Project" :sortable="true">
          <template #header>
            <i class="fas fa-graduation-cap mr-1" aria-hidden="true" />
          </template>
          <template #body="slotProps">
            <div class="flex">
              <div class="flex-1">
                <div>{{ slotProps.data.importingProjectName }}</div>
                <div v-if="slotProps.data.enabled !== 'true'" class="uppercase">
                  <Tag severity="warning">Disabled</Tag>
                </div>
              </div>
              <div class="">
                <SkillsButton
                  label="Contact"
                  icon="fas fa-mail-bulk"
                  outlined
                  size="small"
                  v-if="isEmailEnabled"
                  :aria-label="`Contact ${slotProps.data.name} project owner`"
                  @click="contactProjAdmins(slotProps.data)"
                  :data-cy="`contactOwnerBtn_${ slotProps.data.importingProjectId}`" />
              </div>
            </div>
          </template>
        </Column>
        <Column field="importedOn" header="Imported On" :sortable="true">
          <template #header>
            <i class="fas fa-clock mr-1" aria-hidden="true" />
          </template>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.importedOn" />
          </template>
        </Column>
      </SkillsDataTable>

    </div>
    <div v-else>
      <Message :closable="false">This skill has not been imported by any other projects yet...</Message>
    </div>

    <contact-owners-dialog
      v-if="contactDialog.show"
      v-model="contactDialog.show"
      :project-id="contactDialog.projectId"
      :project-name="contactDialog.projectName"
       />
  </div>
</template>

<style scoped>

</style>