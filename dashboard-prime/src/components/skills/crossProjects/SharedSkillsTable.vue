<script setup>
import { ref, onMounted } from 'vue';
import Column from "primevue/column";

const props = defineProps(['sharedSkills', 'disableDelete']);
const emit = defineEmits(['skill-removed']);

const loaded = ref(false);
const table = ref({
  options: {
    busy: false,
        bordered: false,
        outlined: true,
        stacked: 'md',
        fields: [
        {
          key: 'skillName',
          label: 'Shared Skill',
          sortable: true,
        },
        {
          key: 'projectName',
          label: 'Project',
          sortable: true,
        },
      ],
        pagination: {
      remove: true,
    },
  },
});

onMounted(() => {
  if (!props.disableDelete) {
    table.value.options.fields.push({
      key: 'edit',
      label: 'Remove',
      sortable: false,
    });
  }
  loaded.value = true;
})

const onDeleteEvent = (skill) => {
  emit('skill-removed', skill);
};

const getProjectName = (row) => {
  if (row.sharedWithAllProjects) {
    return 'All Projects';
  }
  return row.projectName;
};

const getProjectId = (row) => {
  if (row.sharedWithAllProjects) {
    return 'All';
  }
  return row.projectId;
};
</script>

<template>
  <div id="shared-skills-table" v-if="sharedSkills && sharedSkills.length" data-cy="sharedSkillsTableDiv">
    <DataTable v-if="loaded" :options="table.options" :value="sharedSkills" data-cy="sharedSkillsTable" tableStoredStateId="sharedSkillsTable">
      <Column field="skillName" header="Shared Skill">
        <template #body="slotProps">
          {{ slotProps.data.skillName }}
        </template>
      </Column>
      <Column field="projectName" header="Project">
        <template #body="slotProps">
          <div><i v-if="slotProps.data.sharedWithAllProjects" class="fas fa-globe text-secondary" /> {{ getProjectName(slotProps.data) }}</div>
          <div v-if="slotProps.data.projectName" class="text-secondary" style="font-size: 0.9rem;">ID: {{ getProjectId(slotProps.data) }}</div>
        </template>
      </Column>
      <Column field="remove" header="Remove" v-if="!disableDelete">
        <template #body="slotProps">
          <Button @click="onDeleteEvent(slotProps.data)"
                    variant="outline-info" size="small" class="text-info"
                    :aria-label="`Remove shared skill ${slotProps.data.skillName}`"
                    data-cy="sharedSkillsTable-removeBtn"><i class="fa fa-trash"/></Button>
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
</style>