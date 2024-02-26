<script setup>
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { useRoute } from 'vue-router'
import { computed, inject, ref } from 'vue'
import { FilterMatchMode } from 'primevue/api'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useSubjSkillsDisplayOrder } from '@/components/skills/UseSubjSkillsDisplayOrder.js'
import { useTimeWindowFormatter } from '@/components/skills/UseTimeWindowFormatter.js'
import { useStorage } from '@vueuse/core'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import DateCell from '@/components/utils/table/DateCell.vue'
import ChildRowSkillsDisplay from '@/components/skills/ChildRowSkillsDisplay.vue'
import SelfReportTableCell from '@/components/skills/skillsTableCells/SelfReportTableCell.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import SkillRemovalValidation from '@/components/skills/SkillRemovalValidation.vue'

const skillsState = useSubjectSkillsState()
const subjectState = useSubjectsState()
const projConfig = useProjConfig()
const route = useRoute()
const announcer = useSkillsAnnouncer()
const timeWindowFormatter = useTimeWindowFormatter()
const subjectId = computed(() => {
  return route.params.subjectId
})
const options = ref({
  emptyText: 'Click Test+ on the top-right to create a test!',
  bordered: true,
  outlined: true,
  stacked: 'md',
  sortBy: 'created',
  sortOrder: -1,
  fields: [
    {
      key: 'name',
      label: 'Skill',
      sortable: true,
      imageClass: 'fas fa-graduation-cap',
      isSticky: true
    },
    {
      key: 'displayOrder',
      label: 'Display Order',
      sortable: true,
      imageClass: 'far fa-eye',
      isSticky: true
    },
    {
      key: 'created',
      label: 'Created On',
      sortable: true,
      imageClass: 'fas fa-clock',
      isSticky: true
    },
    {
      key: 'totalPoints',
      label: 'Points',
      sortable: true,
      imageClass: 'far fa-arrow-alt-circle-up'
    },
    {
      key: 'selfReportingType',
      label: 'Self Report',
      sortable: true,
      imageClass: 'fas fa-laptop'
    },
    {
      key: 'catalogType',
      label: 'Catalog',
      sortable: true,
      imageClass: 'fas fa-book'
    },
    {
      key: 'expiration',
      label: 'Expiration',
      sortable: true,
      imageClass: 'fas fa-stopwatch'
    },
    {
      key: 'timeWindow',
      label: 'Time Window',
      sortable: true,
      imageClass: 'fas fa-hourglass-end'
    },
    {
      key: 'version',
      label: 'Version',
      sortable: true,
      imageClass: 'fas fa-code-branch'
    }
  ],
  pagination: {
    server: false,
    currentPage: 1,
    totalRows: 0,
    pageSize: 5,
    possiblePageSizes: [5, 10, 15, 20]
  }
})

const additionalColumns = ref(options.value.fields.filter((f) => !f.isSticky))
const additionalSelectedColumnKeys = useStorage('subjectSkillsTableAdditionalSelectedColumnKeys', [])
const additionalSelectedColumns = ref(options.value.fields.filter((f) => additionalSelectedColumnKeys.value.includes(f.key)))
const displayedColumns = ref(options.value.fields.filter((f) => f.isSticky || additionalSelectedColumnKeys.value.includes(f.key)))
const onToggle = (currentSelection) => {
  additionalSelectedColumnKeys.value = currentSelection.map((c) => c.key)
  displayedColumns.value = options.value.fields.filter((f) => additionalSelectedColumnKeys.value.includes(f.key) || f.isSticky)
}

const totalRows = ref(skillsState.subjectSkills.length)
const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS }
})
const clearFilter = () => {
  filters.value.global.value = null
}
const onFilter = (filterEvent) => {
  totalRows.value = filterEvent.filteredValue.length
  if (filterEvent.filters?.global?.value) {
    reorderEnable.value = false
  }
}

const selectedSkills = ref([])

const subjSkillsDisplayOrder = useSubjSkillsDisplayOrder()
const reorderEnable = ref(false)
const onReorderSwitchChanged = (enabled) => {
  if (enabled) {
    subjSkillsDisplayOrder.disableFirstAndLastButtons()
    options.value.sortBy = 'displayOrder'
    options.value.sortOrder = 1
    clearFilter()
  }
}
const onColumnSort = () => {
  reorderEnable.value = false
}

const addSkillDisabled = ref(false)

const editGroupInfo = ref({ group: {}, show: false, isEdit: false })
const editImportedSkillInfo = ref({ skill: {}, show: false })

const createOrUpdateSkill = inject('createOrUpdateSkill')
// const editSkill = (itemToEdit) => {
//   // this.currentlyFocusedSkillId = itemToEdit.skillId;
//   if (itemToEdit.isCatalogSkill && itemToEdit.catalogType === 'imported') {
//     editImportedSkillInfo.value = {
//       show: true,
//       skill: itemToEdit
//     }
//   } else if (itemToEdit.isGroupType) {
//     editGroupInfo.value = {
//       isEdit: true,
//       show: true,
//       group: itemToEdit
//     }
//   } else {
//     editSkillInfo.value = { skill: itemToEdit, show: true, isEdit: true }
//   }
// }


const deleteButtonsDisabled = ref(false)
const deleteSkillInfo = ref({
  show: false,
  skill: {}
})
const deleteSkill = (skillToDelete) => {
  deleteSkillInfo.value.skill = skillToDelete
  deleteSkillInfo.value.show = true
}
const doDeleteSkill = () => {
  skillsState.setLoadingSubjectSkills(true)
  const skill = deleteSkillInfo.value.skill
  SkillsService.deleteSkill(skill)
    .then(() => {
      const itemIndex = skillsState.subjectSkills.findIndex((item) => item.skillId === skill.skillId)
      skillsState.subjectSkills.splice(itemIndex, 1)
      announcer.polite(`Removed ${skill.name} skill`)
      subjectState.loadSubjectDetailsState(skill.projectId, skill.subjectId)
      skillsState.setLoadingSubjectSkills(false)
      // skillsState.loadSubjectSkills(skill.projectId, skill.subjectId, false)
      //   .then(() => {
      //     skillsState.setLoadingSubjectSkills(false)
      //     subjectState.loadSubjectDetailsState(skill.projectId, skill.subjectId)
      //     announcer.polite(`Removed ${skill.name} skill`)
      //   })
    })
}

const skillsActionsMenu = ref(false)
const toggleActionsMenu = (event) => {
  skillsActionsMenu.value.toggle(event)
}
const actionsMenu = ref([
  {
    label: ' Export To Catalog',
    icon: 'far fa-arrow-alt-circle-up'
  },
  {
    label: 'Reuse in this Project',
    icon: 'fas fa-recycle'
  },
  {
    label: 'Move Skills',
    icon: 'fas fa-shipping-fast'
  },
  {
    label: 'Add To Badge',
    icon: 'fas fa-award'
  },
  {
    label: 'Tags',
    items: [
      {
        label: 'Add',
        icon: 'fas fa-tag'
      },
      {
        label: 'Remove',
        icon: 'fas fa-trash'
      }
    ]
  }
])
const expandedRows = ref([])


const skillsTable = ref(null)
// const exportCSV = () => {
//   skillsTable.value.exportCSV();
// };
</script>

<template>
  <div>
    <DataTable
      :loading="skillsState.loadingSubjectSkills"
      ref="skillsTable"
      :value="skillsState.subjectSkills"
      :reorderableColumns="true"
      v-model:expandedRows="expandedRows"
      v-model:selection="selectedSkills"
      v-model:filters="filters"
      v-model:sort-field="options.sortBy"
      v-model:sort-order="options.sortOrder"
      @filter="onFilter"
      @sort="onColumnSort"
      paginator
      :rows="10"
      :rowsPerPageOptions="[10, 20, 50, 100]"
      stateStorage="local"
      stateKey="subjectSkillsTable"
      :globalFilterFields="['name']"
      :exportFilename="`skilltree-${subjectId}-skills`"
      data-cy="skillsTable">

      <template #header>
        <div class="flex gap-1">
            <span class="p-input-icon-left flex flex-grow-1">
              <i class="pi pi-search" />
              <InputText
                class="flex flex-grow-1"
                v-model="filters['global'].value"
                data-cy="skillsTable-skillFilter"
                placeholder="Skill Search" />
            </span>
          <SkillsButton class="flex flex-grow-none"
                        label="Reset"
                        icon="fa fa-times"
                        outlined
                        @click="clearFilter"
                        aria-label="Reset skills filter"
                        data-cy="filterResetBtn" />
        </div>
        <div class="mt-4">
          <div class="mt-2 flex">
            <div class="flex-1 text-left">
              <MultiSelect
                v-model="additionalSelectedColumns"
                :options="additionalColumns"
                display="chip"
                :max-selected-labels="3"
                optionLabel="label"
                @update:modelValue="onToggle"
                placeholder="Optional Fields"
                data-cy="skillsTable-additionalColumns" />
            </div>
            <div class="align-items-center flex">
              <label for="sortEnabledSwitch" class="ml-3 mr-1">Reorder:</label>
              <InputSwitch
                id="sortEnabledSwitch"
                data-cy="enableDisplayOrderSort"
                @update:modelValue="onReorderSwitchChanged"
                aria-label="When enabled move-up and move-down buttons will be visible within each display order cell"
                v-model="reorderEnable" />
            </div>
            <Button
              severity="info"
              class="ml-3"
              @click="toggleActionsMenu"
              aria-label="User Settings Button"
              aria-haspopup="true"
              aria-controls="user_settings_menu"
              :disabled="selectedSkills.length === 0"
              data-cy="skillActionsBtn">
              <i class="fas fa-tools mr-1" aria-hidden="true"></i>
              <span>Action</span>
              <Badge :value="selectedSkills.length" data-cy="skillActionsNumSelected"></Badge>
              <i class="fas fa-caret-down ml-2"></i>
            </Button>
            <Menu ref="skillsActionsMenu" id="skillsActionsMenu" :model="actionsMenu" :popup="true">
            </Menu>
          </div>
        </div>
      </template>

      <Column expander style="width: 2rem" />
      <Column selectionMode="multiple" headerStyle="width: 1rem"></Column>
      <Column v-for="col of displayedColumns"
              :key="col.key"
              :field="col.key"
              :sortable="col.sortable">
        <template #header>
          <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field == 'name'" class="flex flex-wrap align-items-center w-min-20rem">
            <div class="flex-1 w-min-10rem">
              <router-link
                class="no-underline"
                :data-cy="`manageSkillLink_${slotProps.data.skillId}`"
                :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId, subjectId, skillId: slotProps.data.skillId }}"
              >
                <highlighted-value :value="slotProps.data.name" :filter="filters.global.value" />
              </router-link>
            </div>
            <div class="flex-none">
              <div class="flex">
                <router-link
                  :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId, subjectId, skillId: slotProps.data.skillId }}"
                >
                  <SkillsButton
                    :label="slotProps.data.isCatalogImportedSkills || projConfig.isReadOnlyProj ? 'View' : 'Manage'"
                    :icon="slotProps.data.isCatalogImportedSkills || projConfig.isReadOnlyProj ? 'fas fa-eye' : 'fas fa-arrow-circle-right'"
                    size="small"
                    outlined
                    severity="info"
                    :aria-label="`Manage skill ${slotProps.data.name}`"
                    :data-cy="`manageSkillBtn_${slotProps.data.skillId}`"
                  />
                </router-link>

                <div v-if="!projConfig.isReadOnlyProj" class="p-buttonset ml-2">
                  <SkillsButton
                    :id="`editSkillButton_${slotProps.data.skillId}`"
                    v-if="!slotProps.data.reusedSkill"
                    icon="fas fa-edit"
                    @click="createOrUpdateSkill(slotProps.data, true)"
                    size="small"
                    outlined
                    severity="info"
                    :track-for-focus="true"
                    :data-cy="`editSkillButton_${slotProps.data.skillId}`"
                    :aria-label="'edit Skill '+slotProps.data.name"
                    :ref="`edit_${slotProps.data.skillId}`"
                    title="Edit Skill" />
                  <SkillsButton
                    :id="`copySkillButton_${slotProps.data.skillId}`"
                    v-if="slotProps.data.type === 'Skill' && !slotProps.data.isCatalogImportedSkills"
                    icon="fas fa-copy"
                    @click="createOrUpdateSkill(slotProps.data, false, true)"
                    size="small"
                    outlined
                    severity="info"
                    :track-for-focus="true"
                    :data-cy="`copySkillButton_${slotProps.data.skillId}`"
                    :aria-label="'copy Skill '+slotProps.data.name"
                    :ref="'copy_'+slotProps.data.skillId"
                    :disabled="addSkillDisabled"
                    title="Copy Skill" />
                  <!--                  v-skills="'CopySkill'" -->
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
                    :track-for-focus="true"
                    :class="{'delete-btn-border-fix' : !slotProps.data.reusedSkill }"
                    :disabled="deleteButtonsDisabled" />
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'displayOrder'" class="w-min-9rem">
            <div class="flex align-items-center">
              <div class="flex-1">
                {{ slotProps.data[col.key] }}
              </div>

              <div class="p-buttonset" v-if="reorderEnable">
                <SkillsButton
                  icon="fas fa-arrow-circle-down"
                  @click="subjSkillsDisplayOrder.moveDisplayOrderDown(slotProps.data)"
                  size="small"
                  outlined
                  :disabled="slotProps.data.disabledDownButton"
                  :aria-label="'move '+slotProps.data.name+' down in the display order'"
                  :data-cy="`orderMoveDown_${slotProps.data.skillId}`" />
                <!--                v-skills="'ChangeSkillDisplayOrder'"-->
                <SkillsButton
                  icon="fas fa-arrow-circle-up"
                  @click="subjSkillsDisplayOrder.moveDisplayOrderUp(slotProps.data)"
                  size="small"
                  outlined
                  :disabled="slotProps.data.disabledUpButton"
                  :aria-label="'move '+slotProps.data.name+' up in the display order'"
                  :data-cy="`orderMoveUp_${slotProps.data.skillId}`" />
                <!--                v-skills="'ChangeSkillDisplayOrder'"-->
              </div>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'created'">
            <DateCell :value="slotProps.data[col.key]" />
          </div>
          <div v-else-if="slotProps.field === 'timeWindow'">
            <div v-if="slotProps.data.isSkillType">
              <div class="text-lg">
                {{ timeWindowFormatter.timeWindowTitle(slotProps.data) }}
              </div>
              <div class="text-sm mt-1">
                {{ timeWindowFormatter.timeWindowDescription(slotProps.data) }}
              </div>
            </div>
            <div v-if="slotProps.data.isGroupType" class="text-secondary">
              N/A
            </div>
          </div>
          <div v-else-if="slotProps.field === 'selfReportingType'">
            <self-report-table-cell :skill="slotProps.data" />
          </div>
          <div v-else>
            {{ slotProps.data[col.key] }}
          </div>
        </template>
      </Column>

      <template #expansion="slotProps">
        <child-row-skills-display
          :id="`childRow-${slotProps.data.skillId}`"
          :key="`childRow-${slotProps.data.skillId}`"
          :skill="slotProps.data" :load-skill-async="true" />
      </template>
      <template #paginatorstart>
        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
      </template>
      <template #paginatorend>
        <!--        <SkillsButton type="button" icon="fas fa-download" text @click="exportCSV" label="Export"/>-->
      </template>

      <template #empty>
        <div class="flex justify-content-center flex-wrap">
          <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle"
             aria-hidden="true"></i>
          <span class="flex align-items-center justify-content-center">No Skills Found.
            <SkillsButton class="flex flex align-items-center justify-content-center px-1"
                          label="Reset"
                          link
                          size="small"
                          @click="clearFilter"
                          aria-label="Reset surveys and quizzes filter"
                          data-cy="skillResetBtnNoFilterRes" /> to clear the existing filter.
              </span>
        </div>
      </template>
    </DataTable>

    <skill-removal-validation
      v-if="deleteSkillInfo.show"
      v-model="deleteSkillInfo.show"
      :skill="deleteSkillInfo.skill"
      @do-remove="doDeleteSkill" />
  </div>
</template>

<style scoped>

</style>