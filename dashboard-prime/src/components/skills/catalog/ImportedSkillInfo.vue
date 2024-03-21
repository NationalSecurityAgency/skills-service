<script setup>
import { onMounted, ref, computed } from 'vue'
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import DateCell from '@/components/utils/table/DateCell.vue'

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
const contactProjAdmins = () => {

}
</script>

<template>
  <div :data-cy="`importSkillInfo-${skill.projectId}_${skill.skillId}`" class="ml-5">

    <div v-if="skill.importedProjectCount > 0">
      <DataTable
        :value="importedProjects"
        sortField="skillName"
        :sortOrder="1"
        data-cy="importedSkillsTable">
        <Column field="importingProjectName" header="Importing Project" :sortable="true">
          <template #header>
            <i class="fas fa-graduation-cap mr-1" aria-hidden="true" />
          </template>
          <template #body="slotProps">
            <div class="flex">
              <div class="flex-1">
                <div>{{ slotProps.data.importingProjectName }}</div>
                <div v-if="slotProps.data.enabled !== 'true'" class="uppercase"><Tag severity="warning">Disabled</Tag></div>
              </div>
              <div class="">
                <SkillsButton
                  label="Contact"
                  icon="fas fa-mail-bulk"
                  outlined
                  size="small"
                  v-if="isEmailEnabled"
                  :aria-label="`Contact ${slotProps.data.name} project owner`"
                  @click="contactProjAdmins(slotProps.data, slotProps.data.importingProjectId)"
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
      </DataTable>

    </div>
    <div v-else>
      <Message :closable="false">This skill has not been imported by any other projects yet...</Message>
    </div>
  </div>
</template>

<style scoped>

</style>