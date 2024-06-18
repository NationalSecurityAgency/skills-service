<script setup>
import { computed, onMounted, ref } from 'vue'
import Column from 'primevue/column'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'

const props = defineProps(['sharedSkills', 'disableDelete'])
const emit = defineEmits(['skill-removed'])
const sortField = ref('')
const sortOrder = ref(0)
const responsive = useResponsiveBreakpoints()
const isFlex = computed(() => responsive.lg.value)

const loaded = ref(false)

onMounted(() => {
  loaded.value = true
})

const onDeleteEvent = (skill) => {
  emit('skill-removed', skill)
}

const getProjectName = (row) => {
  if (row.sharedWithAllProjects) {
    return 'All Projects'
  }
  return row.projectName
}

const getProjectId = (row) => {
  if (row.sharedWithAllProjects) {
    return 'All'
  }
  return row.projectId
}

const sortTable = (criteria) => {
  sortField.value = criteria.sortField
  sortOrder.value = criteria.sortOrder
}

</script>

<template>
  <div id="shared-skills-table" v-if="sharedSkills && sharedSkills.length" data-cy="sharedSkillsTableDiv">
    <SkillsDataTable
      v-if="loaded" :value="sharedSkills" :sortField="sortField" :sortOrder="sortOrder" @sort="sortTable"
      data-cy="sharedSkillsTable" tableStoredStateId="sharedSkillsTable">
      <Column field="skillName" header="Shared Skill" sortable :class="{'flex': isFlex }">
        <template #body="slotProps">
          {{ slotProps.data.skillName }}
        </template>
      </Column>
      <Column field="projectName" header="Project" sortable :class="{'flex': isFlex }">
        <template #body="slotProps">
          <div class="flex flex-column">
            <div>
              <i v-if="slotProps.data.sharedWithAllProjects" class="fas fa-globe text-secondary" />
              {{ getProjectName(slotProps.data) }}
            </div>
            <div v-if="slotProps.data.projectName" class="text-secondary" style="font-size: 0.9rem;">ID:
              {{ getProjectId(slotProps.data) }}
            </div>
          </div>
        </template>
      </Column>
      <Column field="remove" header="Remove" v-if="!disableDelete" :class="{'flex': isFlex }">
        <template #body="slotProps">
          <Button @click="onDeleteEvent(slotProps.data)"
                  variant="outline-info" size="small" class="text-info"
                  :aria-label="`Remove shared skill ${slotProps.data.skillName}`"
                  data-cy="sharedSkillsTable-removeBtn"><i class="fa fa-trash" /></Button>
        </template>
      </Column>
    </SkillsDataTable>
  </div>
</template>

<style scoped>
</style>