<script setup>
const emit = defineEmits(['level-removed', 'change-level'])
defineProps(['levels']);

const onDeleteEvent = (level) => {
  emit('level-removed', level);
};

const onEditLevel = (level) => {
  emit('change-level', level);
};
</script>

<template>
  <div id="simple-levels-table" v-if="levels">
    <SkillsDataTable :value="levels" tableStoredStateId="simpleLevelsTable" data-cy="simpleLevelsTable"
                     :rows="5"
                     :totalRecords="levels.length"
                     :rowsPerPageOptions="[5, 10, 15, 20]"
                     paginator>
      <Column field="projectName" header="Project Name" sortable></Column>
      <Column field="level" header="Level" sortable></Column>
      <Column header="Edit">
        <template #body="slotProps">
          <ButtonGroup>
            <SkillsButton @click="onEditLevel(slotProps.data)" size="small"
                          :track-for-focus="true"
                          :id="`editProjectLevelButton_${slotProps.data.projectId}`"
                          :data-cy="`editProjectLevelButton_${slotProps.data.projectId}`"
                          :aria-label="`edit level ${slotProps.data.level} from ${slotProps.data.projectId}`" :ref="'edit_'+slotProps.data.projectId"
                          title="Edit Project Level Requirement" icon="fas fa-edit">
            </SkillsButton>
            <SkillsButton v-on:click="onDeleteEvent(slotProps.data)" size="small"
                          :aria-label="`delete level ${slotProps.data.level} from ${slotProps.data.projectId}`"
                          :data-cy="`deleteLevelBtn_${slotProps.data.projectId}-${slotProps.data.level}`" icon="fas fa-trash">
            </SkillsButton>
          </ButtonGroup>
        </template>
      </Column>

      <template #paginatorstart>
        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ levels.length }}</span>
      </template>
    </SkillsDataTable>
  </div>
</template>

<style scoped>

</style>