<script setup>
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useRoute } from 'vue-router'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { ref } from 'vue'
import { FilterMatchMode } from 'primevue/api'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import DateCell from '@/components/utils/table/DateCell.vue'

const skillsState = useSubjectSkillsState()
const projConfig = useProjConfig()
const route = useRoute()

const options = ref({
  emptyText: 'Click Test+ on the top-right to create a test!',
  busy: false,
  bordered: true,
  outlined: true,
  stacked: 'md',
  sortBy: 'created',
  sortDesc: false,
  fields: [
    {
      key: 'name',
      label: 'Skill',
      sortable: true,
      imageClass: 'fas fa-graduation-cap'
    },
    {
      key: 'displayOrder',
      label: 'Display Order',
      sortable: true,
      imageClass: 'far fa-eye'
    },
    {
      key: 'created',
      label: 'Created On',
      sortable: true,
      imageClass: 'fas fa-clock'
    },
    {
      key: 'totalPoints',
      label: 'Points',
      sortable: true,
      imageClass: 'fas fa-clock'
    },
  ],
  pagination: {
    server: false,
    currentPage: 1,
    totalRows: 0,
    pageSize: 5,
    possiblePageSizes: [5, 10, 15, 20]
  }
})
const selectedColumns = ref(options.value.fields);
const onToggle = (val) => {
  selectedColumns.value = options.value.fields.filter(col => val.includes(col));
};

const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS }
})
const clearFilter = () => {
  filters.value.global.value = null
}

const selectedProduct = ref();

</script>

<template>
  <div>
    <DataTable
      :value="skillsState.subjectSkills"
      :reorderableColumns="true"
      v-model:selection="selectedProduct"
      v-model:filters="filters"
      :globalFilterFields="['name']">
      <template #header>
        <div class="flex gap-1">
            <span class="p-input-icon-left flex flex-grow-1">
              <i class="pi pi-search" />
              <InputText class="flex flex-grow-1" v-model="filters['global'].value" placeholder="Quiz/Survey Search" />
            </span>
          <SkillsButton class="flex flex-grow-none"
                        label="Reset"
                        icon="fa fa-times"
                        outlined
                        @click="clearFilter"
                        aria-label="Reset surveys and quizzes filter"
                        data-cy="quizResetBtn" />
        </div>
        <div>
          <div style="text-align:left" class="mt-2">
            <MultiSelect :modelValue="selectedColumns"
                         :options="options.fields"
                         display="chip"
                         optionLabel="label"
                         @update:modelValue="onToggle"
                         placeholder="Optinal Fields" />
          </div>
        </div>
      </template>

      <Column selectionMode="multiple" headerStyle="width: 3rem"></Column>
      <Column v-for="col of selectedColumns" :key="col.key" :field="col.key" :sortable="col.sortable">
        <template #header>
          <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field == 'name'" class="flex flex-row flex-wrap">
            <div class="flex-1">
              <highlighted-value :value="slotProps.data.name" :filter="filters.global.value" />
            </div>
            <div class="flex-none">
              <div class="flex">
                <router-link
                  :data-cy="`manageSkillBtn_${slotProps.data.skillId}`"
                  :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId, subjectId: route.params.subjectId, skillId: slotProps.data.skillId }}"
                  :aria-label="`Manage skill ${slotProps.data.name}`" custom>
                  <SkillsButton
                    :label="slotProps.data.isCatalogImportedSkills || projConfig.isReadOnlyProj ? 'View' : 'Manage'"
                    :icon="slotProps.data.isCatalogImportedSkills || projConfig.isReadOnlyProj ? 'fas fa-eye' : 'fas fa-arrow-circle-right'"
                    size="small"
                    outlined
                    severity="info"
                  />
                </router-link>

                <div v-if="!projConfig.isReadOnlyProj" class="p-buttonset ml-2">
                  <SkillsButton
                    v-if="!slotProps.data.reusedSkill"
                    icon="fas fa-edit"
                    @click="editSkill(slotProps.data)"
                    size="small"
                    outlined
                    severity="info"
                    :data-cy="`editSkillButton_${slotProps.data.skillId}`"
                    :aria-label="'edit Skill '+slotProps.data.name"
                    :ref="`edit_${slotProps.data.skillId}`"
                    title="Edit Skill" />
                  <SkillsButton
                    v-if="slotProps.data.type === 'Skill' && !slotProps.data.isCatalogImportedSkills"
                    icon="fas fa-copy"
                    @click="copySkill(slotProps.data)"
                    v-skills="'CopySkill'"
                    size="small"
                    outlined
                    severity="info"
                    :data-cy="`copySkillButton_${slotProps.data.skillId}`"
                    :aria-label="'copy Skill '+slotProps.data.name"
                    :ref="'copy_'+slotProps.data.skillId"
                    :disabled="addSkillDisabled"
                    title="Copy Skill" />
                  <SkillsButton
                    icon="fas fa-trash"
                    :id="`deleteSkillButton_${slotProps.data.skillId}`"
                    :ref="`deleteSkillButton_${slotProps.data.skillId}`"
                    @click="deleteSkill(slotProps.data)" variant="outline-primary"
                    :data-cy="`deleteSkillButton_${slotProps.data.skillId}`"
                    :aria-label="'delete Skill '+slotProps.data.name"
                    title="Delete Skill"
                    size="small"
                    outlined
                    severity="info"
                    :class="{'delete-btn-border-fix' : !slotProps.data.reusedSkill }"
                    :disabled="deleteButtonsDisabled" />
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'displayOrder'" class="max-w-23rem">
            <div class="flex">
              <div class="flex-1">
                {{ slotProps.data[col.key] }}
              </div>

              <div class="p-buttonset">
                <SkillsButton
                  icon="fas fa-arrow-circle-down"
                  @click="moveDisplayOrderDown(slotProps.data)"
                  size="small"
                  outlined
                  :class="{disabled:slotProps.data.disabledDownButton}"
                  :disabled="!sortButtonEnabled || slotProps.data.disabledDownButton"
                  :aria-label="'move '+slotProps.data.name+' down in the display order'"
                  v-skills="'ChangeSkillDisplayOrder'"
                  :data-cy="`orderMoveDown_${slotProps.data.skillId}`" />
                <SkillsButton
                  icon="fas fa-arrow-circle-up"
                  @click="moveDisplayOrderUp(slotProps.data)"
                  size="small"
                  outlined
                  :class="{disabled: slotProps.data.disabledUpButton}"
                  :disabled="!sortButtonEnabled || slotProps.data.disabledUpButton"
                  :aria-label="'move '+slotProps.data.name+' up in the display order'"
                  v-skills="'ChangeSkillDisplayOrder'"
                  :data-cy="`orderMoveUp_${slotProps.data.skillId}`" />
              </div>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'created'">
            <DateCell :value="slotProps.data[col.key]" />
          </div>
          <div v-else>
            {{ slotProps.data[col.key] }}
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>

</style>