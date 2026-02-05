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
import { computed, inject, ref, toRaw } from 'vue'
import { useRoute } from 'vue-router'
import { useStorage } from '@vueuse/core'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { FilterMatchMode } from '@primevue/core/api'
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useSubjSkillsDisplayOrder } from '@/components/skills/UseSubjSkillsDisplayOrder.js'
import { useTimeWindowFormatter } from '@/components/skills/UseTimeWindowFormatter.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import dayjs from '@/common-components/DayJsCustomizer'
import Column from 'primevue/column'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import DateCell from '@/components/utils/table/DateCell.vue'
import ChildRowSkillsDisplay from '@/components/skills/ChildRowSkillsDisplay.vue'
import SelfReportTableCell from '@/components/skills/skillsTableCells/SelfReportTableCell.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import SkillRemovalValidation from '@/components/skills/SkillRemovalValidation.vue'
import ChildRowSkillGroupDisplay from '@/components/skills/skillsGroup/ChildRowSkillGroupDisplay.vue'
import ReuseOrMoveSkillsDialog from '@/components/skills/reuseSkills/ReuseOrMoveSkillsDialog.vue'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import EditImportedSkillDialog from '@/components/skills/skillsGroup/EditImportedSkillDialog.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useInviteOnlyProjectState } from '@/stores/UseInviteOnlyProjectState.js'
import ExportToCatalogDialog from '@/components/skills/catalog/ExportToCatalogDialog.vue'
import AddSkillsToBadgeDialog from '@/components/skills/badges/AddSkillsToBadgeDialog.vue'
import AddSkillTagDialog from '@/components/skills/tags/AddSkillTagDialog.vue'
import RemoveSkillTagDialog from '@/components/skills/tags/RemoveSkillTagDialog.vue'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import SkillNameRouterLink from '@/components/skills/SkillNameRouterLink.vue';
import { useFocusState } from '@/stores/UseFocusState.js'
import skillsService from '@/components/skills/SkillsService.js';
import CopySubjectOrSkillsDialog from "@/components/subjects/CopySubjectOrSkillsDialog.vue";
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import SkillBadgesAndTags from "@/skills-display/components/progress/skill/SkillBadgesAndTags.vue";

const YEARLY = 'YEARLY';
const MONTHLY = 'MONTHLY';
const DAILY = 'DAILY';
const LAST_DAY_OF_MONTH = 'LAST_DAY_OF_MONTH';

const props = defineProps({
  groupId: String,
  groupEnabled: Boolean
})

const responsive = useResponsiveBreakpoints()
const appConfig = useAppConfig()
const skillsState = useSubjectSkillsState()
const subjectState = useSubjectsState()
const projConfig = useProjConfig()
const route = useRoute()
const announcer = useSkillsAnnouncer()
const timeWindowFormatter = useTimeWindowFormatter()
const numberFormat = useNumberFormat()
const inviteOnlyProjectState = useInviteOnlyProjectState()
const focusState = useFocusState()
const log = useLog()

const subjectId = computed(() => route.params.subjectId)
const projectId = computed(() => route.params.projectId)
const tableId = props.groupId || route.params.subjectId
const possiblePageSizes = [10, 20, 50, 100]
const pageSize = useStorage(`${tableId}-skillsTable-pageSize`, 10)

const sortInfo = ref({ sortOrder: -1, sortBy: 'created' })
const options = ref({
  emptyText: 'Click Test+ on the top-right to create a test!',
  bordered: true,
  outlined: true,
  stacked: 'md',
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
    },
    {
      key: 'updated',
      label: 'Last Updated',
      sortable: true,
      imageClass: 'fas fa-clock'
    }
  ]
})

const additionalColumns = ref(options.value.fields.filter((f) => !f.isSticky))
const additionalSelectedColumnKeys = useStorage('subjectSkillsTableAdditionalSelectedColumnKeys', [])
const additionalSelectedColumns = ref(options.value.fields.filter((f) => additionalSelectedColumnKeys.value.includes(f.key)))
const additionalColumnsNumSelectedLabels = computed (() => {
  if (responsive.currentWidth.value < 400) {
    return 1
  }
  if (responsive.currentWidth.value < 600) {
    return 2
  }
  if (responsive.currentWidth.value < 1400) {
    return 3
  }
  if (responsive.currentWidth.value < 1600) {
    return 4
  }
  return 5
})
const displayedColumns = ref(options.value.fields.filter((f) => f.isSticky || additionalSelectedColumnKeys.value.includes(f.key)))
const onToggle = (currentSelection) => {
  additionalSelectedColumnKeys.value = currentSelection.map((c) => c.key)
  displayedColumns.value = options.value.fields.filter((f) => additionalSelectedColumnKeys.value.includes(f.key) || f.isSticky)
  SkillsReporter.reportSkill('SkillsTableAdditionalColumns')
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
  return res
})

const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS }
})
const clearFilter = () => {
  filters.value.global.value = null
  announcer.polite('Skills filter was reset. Showing all results')
}
const onFilter = (filterEvent) => {
  filteredCount.value = filterEvent.filteredValue.length
  if (filterEvent.filters?.global?.value) {
    reorderEnable.value = false
    announcer.polite(`Filtered by ${filterEvent.filters?.global?.value} and returned ${filterEvent.filteredValue.length} results`)
                         }
}

const selectedRows = ref([])
const selectedSkills = computed(() => {
  return selectedRows.value.filter((row) => row.isSkillType && !row.isCatalogImportedSkills)
})

const subjSkillsDisplayOrder = useSubjSkillsDisplayOrder()
const reorderEnable = ref(false)
const onReorderSwitchChanged = (enabled) => {
  if (enabled) {
    subjSkillsDisplayOrder.disableFirstAndLastButtons(props.groupId)
    sortInfo.value.sortBy = 'displayOrder'
    sortInfo.value.sortOrder = 1
    clearFilter()
  }
}
const onColumnSort = () => {
  reorderEnable.value = false
}


const addSkillDisabled = computed(() => {
  return (subjectState.subject?.numSkills || 0) + (subjectState.subject?.numSkillsReused || 0) >= appConfig.maxSkillsPerSubject;
})
const createOrUpdateSkill = inject('createOrUpdateSkill')
const handleEditBtnClick = (skill) => {
  if (skill.isCatalogSkill && skill.catalogType === 'imported') {
    editImportedSkillInfo.value.skill = skill
    editImportedSkillInfo.value.show = true
  } else {
    createOrUpdateSkill(skill, true, false, skill.groupId, props.groupEnabled)
  }
}

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
      skills.sort((a, b) => a.displayOrder - b.displayOrder)
      for (let i = 0; i < skills.length; i++) {
        skills[i].displayOrder = i + 1
      }
      if (skill.groupId) {
        skillsState.setGroupSkills(skill.groupId, skills)
        const parentGroup = skillsState.subjectSkills.find((item) => item.skillId === skill.groupId)
        parentGroup.totalPoints = skills
          .filter((item) => item.enabled === true)
          .map((item) => item.totalPoints)
          .reduce((accumulator, currentValue) => {
            return accumulator + currentValue
          }, 0)
        parentGroup.numSkillsInGroup = skills.length
      }
      announcer.polite(`Removed ${skill.name} skill`)
      subjectState.loadSubjectDetailsState()
      skillsState.setLoadingSubjectSkills(false)
    })
}

const importedSkillUpdated = (skill) => {
  const skills = skill.groupId ? skillsState.getGroupSkills(skill.groupId) : skillsState.subjectSkills
  const itemIndex = skills.findIndex((item) => item.skillId === skill.skillId)
  skills[itemIndex].pointIncrement = skill.pointIncrement
  skills[itemIndex].totalPoints = skill.pointIncrement * skill.numPerformToCompletion
  announcer.polite(`Updated imported ${skill.name} skill's point increment to ${skill.pointIncrement}`)
}

const skillsActionsMenu = ref(false)
const toggleActionsMenu = (event) => {
  skillsActionsMenu.value.toggle(event)
}
const addWidthToIcon = (menuItems) => menuItems.map((item) => ({...item, icon: `${item.icon} w-1_5rem`}))
const actionsMenu = ref(addWidthToIcon([
  {
    label: 'Export To Catalog',
    icon: 'far fa-arrow-alt-circle-up',
    command: () => {
      showExportToCatalogDialog.value = true
    }
  },
  {
    label: 'Reuse in this Project',
    icon: 'fas fa-recycle',
    command: () => {
      showSkillsReuseModal.value = true
    }
  },
  {
    label: 'Move Skills',
    icon: 'fas fa-shipping-fast',
    command: () => {
      showMoveSkillsInfoModal.value = true
    }
  },
  {
    label: 'Add To Badge',
    icon: 'fas fa-award',
    command: () => {
      showAddSkillsToBadgeDialog.value = true
    }
  },
  {
    label: 'Copy to another Project',
    icon: 'fas fa-copy',
    command: () => {
      showCopySkillsModal.value = true
    }
  },
  {
    label: 'Skill Tags',
    items: addWidthToIcon([
      {
        label: 'Add Tag',
        icon: 'fas fa-tag',
        command: () => {
          showAddSkillsTag.value = true
        }
      },
      {
        label: 'Remove Tag',
        icon: 'fas fa-trash',
        command: () => {
          showRemoveSkillsTag.value = true
        }
      }
    ])
  }
]))
const expandedRows = ref([])

const showMoveSkillsInfoModal = ref(false)
const showSkillsReuseModal = ref(false)
const showCopySkillsModal = ref(false)
const showExportToCatalogDialog = ref(false)
const showAddSkillsToBadgeDialog = ref(false)
const showAddSkillsTag = ref(false)
const showRemoveSkillsTag = ref(false)
const isExporting = ref(false)

// const skillsTable = ref(null)
// const exportCSV = () => {
//   skillsTable.value.exportCSV();
// };

const disableRow = (row) => {
  return (row.isGroupType || row.isCatalogImportedSkills) ? 'remove-checkbox' : ''
}

const onMovedOrReused = (movedInfo, isMoved = true) => {
  if (log.isTraceEnabled()) {
    log.trace(`onMovedOrReused ${JSON.stringify(movedInfo)}`)
  }
  const movedSkillIds = movedInfo.moved.map((sk) => sk.skillId)
  const groupSkill = movedInfo.moved.find((sk) => sk.groupId)
  const origSkills = groupSkill ? skillsState.getGroupSkills(groupSkill.groupId) : skillsState.subjectSkills
  const movedSkills = origSkills.filter((sk) => movedSkillIds.includes(sk.skillId))

  if (route.params.subjectId === movedInfo.destination.subjectId && !movedInfo.destination.groupId) {
    skillsState.pushIntoSubjectSkills(toRaw(movedSkills))
  }
  const aMovedSkills = movedInfo.moved[0]
  if (isMoved && route.params.subjectId === aMovedSkills.subjectId && !aMovedSkills.groupId) {
    skillsState.removeSubjectSkillsBySkillIds(movedSkillIds)
  }
  if (movedInfo.destination.groupId) {
    skillsState.loadGroupSkills(route.params.projectId, movedInfo.destination.groupId)
  }
  if (groupSkill) {
    skillsState.loadGroupSkills(route.params.projectId, groupSkill.groupId)
  }

  removeSelectedRows()
  subjectState.loadSubjectDetailsState()
}

const onExported = (exportInfo) => {
  const exportedSkillIds = exportInfo.exported.map((sk) => sk.skillId)

  if (exportInfo.groupId) {
    skillsState.loadGroupSkills(route.params.projectId, exportInfo.groupId)
  } else {
    skillsState.subjectSkills.forEach((skill) => {
      if (exportedSkillIds.includes(skill.skillId)) {
        skill.sharedToCatalog = true
      }
    })
  }
  removeSelectedRows()
  subjectState.loadSubjectDetailsState()
}
const removeSelectedRows = () => {
  selectedRows.value = []
}


const editImportedSkillInfo = ref({
  show: false,
  skill: {}
})

const getExpirationDescription = (skill) => {
  if (skill.expirationType === YEARLY) {
    const d = dayjs(skill.nextExpirationDate);
    const plural = skill.every > 1;
    return `Every${plural ? ` ${skill.every}` : ''} year${plural ? 's' : ''} on ${d.format('MMMM')} ${d.format('Do')}`;
  }
  if (skill.expirationType === MONTHLY) {
    const d = dayjs(skill.nextExpirationDate);
    const plural = skill.every > 1;
    const date = skill.monthlyDay === LAST_DAY_OF_MONTH ? 'last' : d.format('Do');
    return `Every${plural ? ` ${skill.every}` : ''} month${plural ? 's' : ''} on the ${date} day of the month`;
  }
  if (skill.expirationType === DAILY) {
    const plural = skill.every > 1;
    return `After ${skill.every} day${plural ? 's' : ''} of inactivity`;
  }
  return '';
}
const getNextExpirationDate = (skill) => {
  if (skill.nextExpirationDate) {
    return `Expires next on ${dayjs(skill.nextExpirationDate).format('YYYY-MM-DD')}`;
  }
  return '';
}

const isLoading = computed(() => {
  if (props.groupId) {
    return skillsState.getLoadingGroupSkills(props.groupId)
  }

  return skillsState.loadingSubjectSkills
})

const isSkillsGroupTable = computed(() => {
  return !!props.groupId
})


// Actions Men Tab fix
// the following functions handle when pressing tab after Action Menu is open
// before this code tab would cause focus to jump all way up to the address bar
// these functions block tab and when menu is closed re-focus on the Actions Button
function handleKeyDown(event) {
  // tab=9
  if (event.keyCode === 9) {
    event.preventDefault();
    if (actionMenuFocused.value) {
      focusState.focusOnLastElement()
    }
  }
}
const actionMenuFocused = ref(false)
const actionMenuOnFocus = () => {
  actionMenuFocused.value = true
  document.addEventListener('keydown', handleKeyDown);
}
const actionMenuOnBlur = () => {
  actionMenuFocused.value = false
  document.removeEventListener('keydown', handleKeyDown);

}
// done with actions fix

const exportSkills = () => {
  isExporting.value = true
  return skillsService.exportSubjectSkills(projectId.value, subjectId.value).then((dataFromServer) => {
    isExporting.value = false
  })
}

const onRowExpand = () => {
  SkillsReporter.reportSkill('ExpandSkillDetailsSkillsPage')
}

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
}
</script>

<template>
  <div>
    <div class="p-4 text-primary bg-primary-contrast">
      <div class="flex gap-1">
        <InputGroup>
          <InputGroupAddon>
            <i class="fas fa-search" aria-hidden="true"/>
          </InputGroupAddon>
          <InputText
              class="flex grow"
              v-model="filters['global'].value"
              data-cy="skillsTable-skillFilter"
              aria-label="Skill Search"
              placeholder="Skill Search"/>
          <InputGroupAddon class="p-0 m-0">
            <SkillsButton
                id="skillsFilterResetBtn"
                icon="fa fa-times"
                text
                outlined
                @click="clearFilter"
                aria-label="Reset skills filter"
                data-cy="filterResetBtn"/>
          </InputGroupAddon>
        </InputGroup>
      </div>
      <div class="mt-6">
        <div class="mt-2 flex flex-wrap">
          <div class="flex-1 w-full lg:w-auto">
            <MultiSelect
                class="w-full lg:w-auto"
                v-model="additionalSelectedColumns"
                :options="additionalColumns"
                display="chip"
                :max-selected-labels="additionalColumnsNumSelectedLabels"
                optionLabel="label"
                @update:modelValue="onToggle"
                placeholder="Optional Fields"
                data-cy="skillsTable-additionalColumns"/>
          </div>
          <div v-if="!projConfig.isReadOnlyProj" class="w-full lg:w-auto flex mt-4 lg:mt-0 flex-col sm:flex-row gap-2">
            <div class="flex-1 items-center flex">
              <label for="sortEnabledSwitch" class="lg:ml-4 mr-1">Reorder:</label>
              <ToggleSwitch
                  inputId="sortEnabledSwitch"
                  data-cy="enableDisplayOrderSort"
                  @update:modelValue="onReorderSwitchChanged"
                  aria-label="Sorting Control - when enabled move-up and move-down buttons will be visible within each display order cell"
                  v-model="reorderEnable"/>
            </div>
            <SkillsButton
                :id="`skillActionsBtn${groupId || ''}`"
                severity="info"
                class="ml-4"
                @click="toggleActionsMenu"
                aria-label="Skill's actions button"
                aria-haspopup="true"
                :disabled="selectedSkills.length === 0"
                :track-for-focus="true"
                data-cy="skillActionsBtn">
              <i class="fas fa-tools" aria-hidden="true"></i>
              <span>Action</span>
              <Tag data-cy="skillActionsNumSelected" severity="info">{{ selectedSkills.length }}</Tag>
              <i class="fas fa-caret-down ml-2"></i>
            </SkillsButton>
            <Menu ref="skillsActionsMenu"
                  id="skills-actions-menu"
                  data-cy="skillsActionsMenu"
                  :model="actionsMenu"
                  @focus="actionMenuOnFocus"
                  @blur="actionMenuOnBlur"
                  aria-label="Menu to perform actions on selected skills"
                  :popup="true">
              <template #item="{ item, props }">
                <a :href="item.url" target="_blank" v-bind="props.action">
                  <span class="w-7 border text-center rounded-sm text-green-800 bg-green-50 dark:bg-gray-900 dark:text-green-500 dark:border-green-700"><i :class="item.icon"/></span>
                  <span class="">{{ item.label }}</span>
                </a>
              </template>
            </Menu>
          </div>
        </div>
      </div>
    </div>
    <SkillsDataTable
      :id="tableId"
      :tableStoredStateId="tableId"
      :loading="isLoading || isExporting"
      :value="tableSkills"
      dataKey="skillId"
      :reorderableColumns="true"
      v-model:expandedRows="expandedRows"
      v-model:selection="selectedRows"
      v-model:filters="filters"
      v-model:sort-field="sortInfo.sortBy"
      v-model:sort-order="sortInfo.sortOrder"
      @update:first="(val) => indexOfFirstRow = val"
      @filter="onFilter"
      @sort="onColumnSort"
      @page="pageChanged"
      paginator
      :rows="pageSize"
      :rowsPerPageOptions="possiblePageSizes"
      :globalFilterFields="['name']"
      :exportFilename="`skilltree-${subjectId}-skills`"
      :row-class="disableRow"
      :expander="true"
      @rowExpand="onRowExpand"
      aria-label="Skills"
      data-cy="skillsTable">

      <template #loading>
￼          <div data-cy="skillsTable-loading">
￼            <Message v-if="isExporting" icon="fas fa-download" severity="contrast" :closable="false">Exporting, please wait...</Message>
￼            <SkillsSpinner :is-loading="true"></SkillsSpinner>
￼          </div>
      </template>

      <template #header v-if="!isSkillsGroupTable">
        <div class="flex justify-end flex-wrap mt-2">
          <SkillsButton :disabled="totalRows <= 0"
                        size="small"
                        icon="fas fa-download"
                        label="Export All Skills"
                        @click="exportSkills"
                        data-cy="exportSkillsTableBtn" />
        </div>
      </template>

      <Column v-if="!projConfig.isReadOnlyProj" selectionMode="multiple" :class="{'flex': responsive.lg.value }">
        <template #header>
          <span class="mr-1 lg:mr-0 lg:hidden"><i class="fas fa-check-double"
                                                  aria-hidden="true"></i> Select Rows:</span>
        </template>
      </Column>
      <Column v-for="col of displayedColumns"
              :key="col.key"
              :field="col.key"
              :class="{'flex': responsive.lg.value }"
              :sortable="col.sortable">
        <template #header>
          <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field == 'name'"
               class="flex flex-wrap items-center flex-col sm:flex-row"
               :data-cy="`nameCell_${slotProps.data.skillId}`">
            <div class="d-inline-block rounded-border w-16 skill-icon-container text-primary text-center border mr-4">
              <i class="skill-icon" :class="`${slotProps.data.iconClass ? slotProps.data.iconClass : 'fas fa-graduation-cap'}`"></i>
            </div>
            <div v-if="slotProps.data.isGroupType" class="flex-1">
              <div>
                <i class="fas fa-layer-group" aria-hidden="true"></i> <span class="uppercase">Group</span>
                <Tag class="uppercase ml-2" data-cy="numSkillsInGroup">{{ slotProps.data.numSkillsInGroup }}
                  skill{{ slotProps.data.numSkillsInGroup !== 1 ? 's' : '' }}
                </Tag>
              </div>
              <highlighted-value
                class="text-lg w-min-10rem"
                :value="slotProps.data.name"
                :filter="filters.global.value" />
              <Tag
                  v-if="!slotProps.data.enabled"
                  severity="secondary"
                  class="mt-1"
                  :data-cy="`disabledBadge-${slotProps.data.skillId}`">
                <span><i class="fas fa-eye-slash" aria-hidden="true"></i> Disabled</span>
              </Tag>
            </div>
            <div v-if="!slotProps.data.isGroupType" class="flex-1">
              <div class="flex w-min-10rem">
                <SkillNameRouterLink :skill="slotProps.data" :subjectId="subjectId"
                                     :filter-value="filters.global.value"
                                     :read-only="projConfig.isReadOnlyProj || slotProps.data.isCatalogImportedSkills"
                />
              </div>
              <div class="flex flex-wrap gap-2 items-center">
                <Tag
                  v-if="slotProps.data.isCatalogImportedSkills"
                  severity="success"
                  class="mt-1"
                  aria-label="Reused"
                  :data-cy="`importedBadge-${slotProps.data.skillId}`">
                  <span v-if="slotProps.data.reusedSkill"><i class="fas fa-recycle"
                                                             aria-hidden="true"></i> Reused</span>
                  <span v-else><i class="fas fa-book" aria-hidden="true"></i> Imported</span>
                </Tag>
                <Tag
                  v-if="!slotProps.data.enabled"
                  severity="secondary"
                  class="mt-1"
                  :data-cy="`disabledBadge-${slotProps.data.skillId}`">
                  <span><i class="fas fa-eye-slash" aria-hidden="true"></i> Disabled</span>
                </Tag>
                <Tag
                  v-if="slotProps.data.sharedToCatalog"
                  class="mt-1"
                  :data-cy="`exportedBadge-${slotProps.data.skillId}`">
                  <span><i class="fas fa-book" aria-hidden="true"></i> Exported</span>
                </Tag>
                <Chip :pt="{ root: { class: 'p-0!'}}"
                      v-for="(tag) in slotProps.data.tags"
                      :key="tag.tagId"
                      :data-cy="`skillTag-${slotProps.data.skillId}-${tag.tagId}`">
                  <span class="bg-primary text-primary-contrast rounded-full w-6 h-6 flex items-center justify-center"><i
                      class="fas fa-tag text-sm" aria-hidden="true"/></span>
                        <span class="font-medium pr-3">{{ tag.tagValue }}</span>
                </Chip>
              </div>
              <div v-if="slotProps.data.badges && slotProps.data.badges.length > 0" class="mt-2">
                <i class="fa fa-award text-purple-500" aria-hidden="true"></i> Badges:
                <router-link v-for="badge in slotProps.data.badges" :key="badge.id"
                             :to="{ name: badge.skillType === 'GlobalBadge' ? 'GlobalBadgeSkills' : 'BadgeSkills', params: { badgeId: badge.badgeId, projectId: projectId } }"
                             class="skills-theme-primary-color mr-2" style="text-decoration:underline;">
                  {{ badge.name }}
                </router-link>
              </div>
            </div>
            <div class="flex items-start justify-end">
              <div class="flex flex-nowrap">
                <ButtonGroup v-if="!projConfig.isReadOnlyProj" class="mt-2 ml-1">
                  <SkillsButton
                    :id="`editSkillButton_${slotProps.data.skillId}`"
                    v-if="!slotProps.data.reusedSkill"
                    icon="fas fa-edit"
                    @click="handleEditBtnClick(slotProps.data)"
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
                    @click="createOrUpdateSkill(slotProps.data, false, true, slotProps.data.groupId, groupEnabled)"
                    size="small"
                    outlined
                    severity="info"
                    :track-for-focus="true"
                    :data-cy="`copySkillButton_${slotProps.data.skillId}`"
                    :aria-label="'copy Skill '+slotProps.data.name"
                    :ref="'copy_'+slotProps.data.skillId"
                    :disabled="addSkillDisabled"
                    v-skills="'CopySkill'"
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
                    :track-for-focus="true"
                    :class="{'delete-btn-border-fix' : !slotProps.data.reusedSkill }"
                    :disabled="deleteButtonsDisabled" />
                </ButtonGroup>
              </div>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'displayOrder'" class="w-min-9rem">
            <div class="flex items-center">
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
                  v-skills="'ChangeSkillDisplayOrder'"
                  :data-cy="`orderMoveDown_${slotProps.data.skillId}`" />
                <SkillsButton
                  icon="fas fa-arrow-circle-up"
                  @click="subjSkillsDisplayOrder.moveDisplayOrderUp(slotProps.data)"
                  size="small"
                  outlined
                  :disabled="slotProps.data.disabledUpButton"
                  :aria-label="'move '+slotProps.data.name+' up in the display order'"
                  v-skills="'ChangeSkillDisplayOrder'"
                  :data-cy="`orderMoveUp_${slotProps.data.skillId}`" />
              </ButtonGroup>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'created' || slotProps.field === 'updated'">
            <DateCell :value="slotProps.data[col.key]" />
          </div>
          <div v-else-if="slotProps.field === 'expiration'">
            <div v-if="slotProps.data.expirationType && slotProps.data.expirationType !== 'NEVER'">
              {{ getExpirationDescription(slotProps.data) }}
              <div v-if="getNextExpirationDate(slotProps.data)" class="font-light text-sm">
                {{ getNextExpirationDate(slotProps.data) }}
              </div>
            </div>
            <div v-else class="text-secondary">
              None
            </div>
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
              <div class="text-lg">{{ numberFormat.pretty(slotProps.data.totalPoints) }}</div>
              <div v-if="slotProps.data.isSkillType" class="text-muted-color">
                {{ numberFormat.pretty(slotProps.data.pointIncrement) }} pts x {{ slotProps.data.numPerformToCompletion
                }} repetitions
              </div>
              <div v-if="slotProps.data.isGroupType" class="text-muted-color">from
                <Tag>{{ slotProps.data.numSkillsInGroup }}</Tag>
                skill{{ slotProps.data.numSkillsInGroup !== 1 ? 's' : '' }}
              </div>
            </div>
          </div>
          <div v-else-if="slotProps.field === 'catalogType'">
            <div v-if="slotProps.data.isCatalogImportedSkills && !slotProps.data.reusedSkill">
              <Tag severity="success"><i class="fas fa-book mr-1" aria-hidden="true" /> IMPORTED</Tag>
              <p class="text-secondary">Imported from <span
                class="text-primary">{{ slotProps.data.copiedFromProjectName }}</span></p>
            </div>
            <div v-if="slotProps.data.sharedToCatalog">
              <Tag severity="secondary"><i class="fas fa-book mr-1" aria-hidden="true" /> EXPORTED</Tag>
              <p class="text-secondary">Exported to Skill Catalog</p>
            </div>

            <div v-if="!slotProps.data.isCatalogSkill" class="">
              N/A
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
          class="ml-6"
        />
        <child-row-skills-display
          v-if="slotProps.data.isSkillType"
          :id="`childRow-${slotProps.data.skillId}`"
          :key="`childRow-${slotProps.data.skillId}`"
          :skill="slotProps.data" :load-skill-async="true" />
      </template>
      <template #paginatorstart>
        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
      </template>

      <template #empty>
        <table-no-res no-res-msg="No Skills Found." :showResetFilter="true" @resetFilter="clearFilter"/>
      </template>
    </SkillsDataTable>

    <skill-removal-validation
      v-if="deleteSkillInfo.show"
      v-model="deleteSkillInfo.show"
      :skill="deleteSkillInfo.skill"
      @do-remove="doDeleteSkill" />
    <export-to-catalog-dialog
      v-if="showExportToCatalogDialog"
      v-model="showExportToCatalogDialog"
      :skills="selectedSkills"
      :show-invite-only-warning="inviteOnlyProjectState.isInviteOnlyProject"
      :group-id="groupId"
      @on-exported="onExported"
      @on-nothing-to-export="removeSelectedRows"
    />
    <reuse-or-move-skills-dialog
      id="moveSkillsModal"
      v-if="showMoveSkillsInfoModal"
      v-model="showMoveSkillsInfoModal"
      :skills="selectedSkills"
      @on-moved="onMovedOrReused"
    />
    <reuse-or-move-skills-dialog
      id="reuseSkillsModal"
      :is-reuse-type="true"
      v-if="showSkillsReuseModal"
      v-model="showSkillsReuseModal"
      :skills="selectedSkills"
      @on-moved="onMovedOrReused($event, false)"
    />
    <edit-imported-skill-dialog
      v-if="editImportedSkillInfo.show"
      v-model="editImportedSkillInfo.show"
      :skill="editImportedSkillInfo.skill"
      @skill-updated="importedSkillUpdated" />
    <add-skills-to-badge-dialog
      id="addSkillsToBadgeModal"
      v-if="showAddSkillsToBadgeDialog"
      v-model="showAddSkillsToBadgeDialog"
      :skills="selectedSkills"
      @on-added="removeSelectedRows"
    />
    <copy-subject-or-skills-dialog
        v-if="showCopySkillsModal"
        v-model="showCopySkillsModal"
        copy-type="SelectSkills"
        :selected-skills="selectedSkills"
        @after-copied="removeSelectedRows"
    />
    <add-skill-tag-dialog
      id="addTagSkillsModal"
      v-if="showAddSkillsTag"
      v-model="showAddSkillsTag"
      :skills="selectedSkills"
      :group-id="groupId"
      @added-tag="removeSelectedRows"
    />
    <remove-skill-tag-dialog
      id="removeTagSkillsModal"
      v-if="showRemoveSkillsTag"
      v-model="showRemoveSkillsTag"
      :skills="selectedSkills"
      :group-id="groupId"
      @removed-tag="removeSelectedRows"
    />
  </div>
</template>

<style>
.remove-checkbox .p-checkbox {
  visibility: hidden !important;
}

.skill-icon {
  height: 100% !important;
  width: 100% !important;
  background-size: cover;
  background-position: center;
  font-size: 30px !important;
  line-height: 46px !important;
}

.skill-icon-container {
  max-width:48px;
  max-height:48px;
  height:48px;
  width: 48px;
}
</style>