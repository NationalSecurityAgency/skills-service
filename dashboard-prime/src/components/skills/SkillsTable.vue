<script setup>
import { computed, inject, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useStorage } from '@vueuse/core'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { FilterMatchMode } from 'primevue/api'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useSubjSkillsDisplayOrder } from '@/components/skills/UseSubjSkillsDisplayOrder.js'
import { useTimeWindowFormatter } from '@/components/skills/UseTimeWindowFormatter.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import DateCell from '@/components/utils/table/DateCell.vue'
import ChildRowSkillsDisplay from '@/components/skills/ChildRowSkillsDisplay.vue'
import SelfReportTableCell from '@/components/skills/skillsTableCells/SelfReportTableCell.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import SkillRemovalValidation from '@/components/skills/SkillRemovalValidation.vue'
import ChildRowSkillGroupDisplay from '@/components/skills/skillsGroup/ChildRowSkillGroupDisplay.vue'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'

const props = defineProps({
  groupId: String,
})

const skillsState = useSubjectSkillsState()
const subjectState = useSubjectsState()
const projConfig = useProjConfig()
const route = useRoute()
const announcer = useSkillsAnnouncer()
const timeWindowFormatter = useTimeWindowFormatter()
const subjectId = computed(() => {
  return route.params.subjectId
})
const tableId = props.groupId || route.params.subjectId
const pagination = {
  pageSize: 10,
  possiblePageSizes: [10, 20, 50, 100]
}
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
      label: 'Display',
      sortable: true,
      imageClass: 'far fa-eye',
      isSticky: true
    },
    {
      key: 'created',
      label: 'Created',
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
})

const additionalColumns = ref(options.value.fields.filter((f) => !f.isSticky))
const additionalSelectedColumnKeys = useStorage('subjectSkillsTableAdditionalSelectedColumnKeys', [])
const additionalSelectedColumns = ref(options.value.fields.filter((f) => additionalSelectedColumnKeys.value.includes(f.key)))
const displayedColumns = ref(options.value.fields.filter((f) => f.isSticky || additionalSelectedColumnKeys.value.includes(f.key)))
const onToggle = (currentSelection) => {
  additionalSelectedColumnKeys.value = currentSelection.map((c) => c.key)
  displayedColumns.value = options.value.fields.filter((f) => additionalSelectedColumnKeys.value.includes(f.key) || f.isSticky)
}

const tableSkills = computed(() => {
  if (props.groupId) {
    return skillsState.getGroupSkills(props.groupId)
  }
  return skillsState.subjectSkills
})

const filteredCount = ref(-1)
const totalRows = computed(() => {
  let res = null
  if (filteredCount.value > 0) {
    res = filteredCount.value
  } else if (props.groupId) {
    res = skillsState.getGroupSkills(props.groupId).length
  } else {
    res = skillsState.subjectSkills.length
  }
  console.log(res);
  return  res
})

const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS }
})
const clearFilter = () => {
  filters.value.global.value = null
}
const onFilter = (filterEvent) => {
  filteredCount.value = filterEvent.filteredValue.length
  if (filterEvent.filters?.global?.value) {
    reorderEnable.value = false
  }
}

const selectedRows = ref([])
const selectedSkills = computed(() => {
  return selectedRows.value.filter((row) => row.isSkillType)
})

const subjSkillsDisplayOrder = useSubjSkillsDisplayOrder()
const reorderEnable = ref(false)
const onReorderSwitchChanged = (enabled) => {
  if (enabled) {
    subjSkillsDisplayOrder.disableFirstAndLastButtons(props.groupId)
    options.value.sortBy = 'displayOrder'
    options.value.sortOrder = 1
    clearFilter()
  }
}
const onColumnSort = () => {
  reorderEnable.value = false
}

const addSkillDisabled = ref(false)

const editImportedSkillInfo = ref({ skill: {}, show: false })

const createOrUpdateSkill = inject('createOrUpdateSkill')

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
      const skills = skill.groupId ? skillsState.getGroupSkills(skill.groupId) : skillsState.subjectSkills
      const itemIndex = skills.findIndex((item) => item.skillId === skill.skillId)
      skills.splice(itemIndex, 1)
      skills.sort((a,b) => a.displayOrder - b.displayOrder)
      for (let i = 0; i < skills.length; i++) {
        skills[i].displayOrder = i+1
      }
      if (skill.groupId) {
        skillsState.setGroupSkills(skill.groupId, skills)
        const parentGroup = skillsState.subjectSkills.find((item) => item.skillId = skill.groupId)
        parentGroup.totalPoints -= skill.totalPoints
        parentGroup.numSkillsInGroup -= 1
      }
      announcer.polite(`Removed ${skill.name} skill`)
      subjectState.loadSubjectDetailsState()
      skillsState.setLoadingSubjectSkills(false)
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


// const skillsTable = ref(null)
// const exportCSV = () => {
//   skillsTable.value.exportCSV();
// };

const disableRow = (row) => {
  return row.isGroupType ? 'remove-checkbox' : '';
}
</script>

<template>
  <div>
    <DataTable
      :id="tableId"
      :loading="skillsState.loadingSubjectSkills"
      :value="tableSkills"
      :reorderableColumns="true"
      v-model:expandedRows="expandedRows"
      v-model:selection="selectedRows"
      v-model:filters="filters"
      v-model:sort-field="options.sortBy"
      v-model:sort-order="options.sortOrder"
      @filter="onFilter"
      @sort="onColumnSort"
      :paginator="true"
      :rows="pagination.pageSize"
      :rowsPerPageOptions="pagination.possiblePageSizes"
      stateStorage="local"
      stateKey="subjectSkillsTable"
      :globalFilterFields="['name']"
      :exportFilename="`skilltree-${subjectId}-skills`"
      :row-class="disableRow"
      data-cy="skillsTable">

      <template #header>
        <div class="flex gap-1">
          <InputGroup>
            <InputGroupAddon>
              <i class="fas fa-search" aria-hidden="true"/>
            </InputGroupAddon>
            <InputText
              class="flex flex-grow-1"
              v-model="filters['global'].value"
              data-cy="skillsTable-skillFilter"
              placeholder="Skill Search" />
          </InputGroup>
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
      <Column selectionMode="multiple">
<!--        <template #rowcheckboxicon>-->
<!--          <i  class="fas fa-layer-group" />-->
<!--        </template>-->
      </Column>
      <Column v-for="col of displayedColumns"
              :key="col.key"
              :field="col.key"
              :sortable="col.sortable">
        <template #header>
          <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field == 'name'"
               class="flex flex-wrap align-items-center w-min-20rem"
               :data-cy="`nameCell_${slotProps.data.skillId}`">
            <div v-if="slotProps.data.isGroupType" class="flex-1">
              <div>
                <i class="fas fa-layer-group" aria-hidden="true"></i> <span class="uppercase">Group</span>
              </div>
              <highlighted-value
                class="text-lg"
                :value="slotProps.data.name"
                :filter="filters.global.value" />
            </div>
            <div v-if="!slotProps.data.isGroupType" class="flex-1 w-min-10rem">
              <router-link
                class=""
                :data-cy="`manageSkillLink_${slotProps.data.skillId}`"
                :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId, subjectId, skillId: slotProps.data.skillId }}"
              >
                <highlighted-value
                  class="text-lg"
                  :value="slotProps.data.name" :filter="filters.global.value" />
              </router-link>
            </div>
            <div class="flex-none">
              <div class="flex">
                <router-link
                  :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId, subjectId, skillId: slotProps.data.skillId }}"
                >
<!--                  <SkillsButton-->
<!--                    v-if="!slotProps.data.isGroupType"-->
<!--                    :label="slotProps.data.isCatalogImportedSkills || projConfig.isReadOnlyProj ? 'View' : 'Manage'"-->
<!--                    :icon="slotProps.data.isCatalogImportedSkills || projConfig.isReadOnlyProj ? 'fas fa-eye' : 'fas fa-arrow-circle-right'"-->
<!--                    size="small"-->
<!--                    outlined-->
<!--                    severity="info"-->
<!--                    :aria-label="`Manage skill ${slotProps.data.name}`"-->
<!--                    :data-cy="`manageSkillBtn_${slotProps.data.skillId}`"-->
<!--                  />-->
                </router-link>

                <ButtonGroup v-if="!projConfig.isReadOnlyProj" class="mt-2">
                  <SkillsButton
                    :id="`editSkillButton_${slotProps.data.skillId}`"
                    v-if="!slotProps.data.reusedSkill"
                    icon="fas fa-edit"
                    @click="createOrUpdateSkill(slotProps.data, true, false, slotProps.data.groupId)"
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
                    @click="createOrUpdateSkill(slotProps.data, false, true, slotProps.data.groupId)"
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
                </ButtonGroup>
              </div>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'displayOrder'" class="w-min-9rem">
            <div class="flex align-items-center">
              <div class="flex-1">
                {{ slotProps.data[col.key] }}
              </div>

              <ButtonGroup v-if="reorderEnable">
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
              </ButtonGroup>
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
          <div v-else-if="slotProps.field === 'totalPoints'">
            <div :data-cy="`totalPointsCell_${slotProps.data.skillId}`">
              <div class="text-lg">{{ slotProps.data.totalPoints }}</div>
              <div v-if="slotProps.data.isSkillType" class="text-color-secondary">{{ slotProps.data.pointIncrement  }} pts x {{ slotProps.data.numPerformToCompletion }} repetitions</div>
              <div v-if="slotProps.data.isGroupType" class="text-color-secondary">from <Tag>{{ slotProps.data.numSkillsInGroup }}</Tag> skill{{ slotProps.data.numSkillsInGroup !== 1 ? 's' : ''}}</div>
            </div>
          </div>
          <div v-else>
            {{ slotProps.data[col.key] }}
          </div>
        </template>
      </Column>

      <template #expansion="slotProps">
        <child-row-skill-group-display
          v-if="slotProps.data.isGroupType"
          :id="`childRow-${slotProps.data.skillId}`"
          :key="`childRow-${slotProps.data.skillId}`"
          :skill="slotProps.data"
          class="ml-4"
        />
        <child-row-skills-display
          v-if="slotProps.data.isSkillType"
          :id="`childRow-${slotProps.data.skillId}`"
          :key="`childRow-${slotProps.data.skillId}`"
          :skill="slotProps.data" :load-skill-async="true" />
      </template>
      <template #paginatorstart>
<!--        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>-->
      </template>
      <template #paginatorend>
        <!--        <SkillsButton type="button" icon="fas fa-download" text @click="exportCSV" label="Export"/>-->
      </template>
      <template #footer>
        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
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

<style>
.remove-checkbox .p-checkbox {
  visibility: hidden !important;
}
</style>