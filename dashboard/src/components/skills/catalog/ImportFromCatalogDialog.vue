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
import { computed, onMounted, ref, watch } from 'vue'
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import { useRoute } from 'vue-router'
import SettingsService from '@/components/settings/SettingsService.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import Column from 'primevue/column'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import SkillToImportInfo from '@/components/skills/catalog/SkillToImportInfo.vue'
import { useStorage } from '@vueuse/core'
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import SkillAlreadyExistingWarning from '@/components/skills/catalog/SkillAlreadyExistingWarning.vue'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'
import { useFocusState } from '@/stores/UseFocusState.js'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue';
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const model = defineModel()
const props = defineProps({
  groupId: {
    type: String,
    default: ''
  }
})
const route = useRoute()
const appConfig = useAppConfig()
const responsive = useResponsiveBreakpoints()
const subjectState = useSubjectsState()
const skillsState = useSubjectSkillsState()
const finalizeState = useFinalizeInfoState()
const numberFormat = useNumberFormat()

const initialLoad = ref(true)
const reloadData = ref(false)
const data = ref([])
const totalRows = ref(1)
const pageSize = ref(5)
const possiblePageSizes = [5, 10, 15, 25, 50]
const currentPage = ref(1)
const sortInfo = useStorage('importFromSkillsCatalogTable', { sortOrder: 1, sortBy: 'name' })
const initialLoadHadData = ref(false)
const isInFinalizeState = ref(false)
const filters = ref({
  skillName: '',
  projectName: '',
  subjectName: ''
})
const selectedRows = ref([])
const expandedRows = ref([])

onMounted(() => {
  loadFinalizationState()
    .then(() => {
      loadData()
    })
})

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

const loadData = () => {
  reloadData.value = true
  const params = {
    limit: pageSize.value,
    page: currentPage.value,
    orderBy: sortInfo.value.sortBy,
    ascending: sortInfo.value.sortOrder === 1,
    projectNameSearch: encodeURIComponent(filters.value.projectName.trim()),
    subjectNameSearch: encodeURIComponent(filters.value.subjectName.trim()),
    skillNameSearch: encodeURIComponent(filters.value.skillName.trim())
  }
  return CatalogService.getCatalogSkills(route.params.projectId, params)
    .then((res) => {
      const dataSkills = res.data
      if (dataSkills) {
        data.value = dataSkills.map((item) => ({
          selected: false,
          ...item
        }))
        totalRows.value = res.totalCount
        if (totalRows.value > 0) {
          initialLoadHadData.value = true
        }
      }
    })
    .finally(() => {
      initialLoad.value = false
      reloadData.value = false
    })
}
const loadFinalizationState = () => {
  return SettingsService.getProjectSetting(route.params.projectId, 'catalog.finalize.state')
    .then((res) => {
      isInFinalizeState.value = res && res.value === 'RUNNING'
    })
}

const emptyCatalog = computed(() => !initialLoadHadData.value)
const showTable = computed(() => !emptyCatalog.value && !isInFinalizeState.value)
const maxBulkImportExceeded = computed(() => {
  return selectedRows.value.length > appConfig.maxSkillsInBulkImport
})
const maxSkillsInSubjectExceeded = computed(() => {
  return (selectedRows.value.length + skillsState.totalNumSkillsInSubject) > appConfig.maxSkillsPerSubject
})

const importInProgress = ref(false)
const importDisabled = computed(() => selectedRows.value <= 0)
const doImport = () => {
  importInProgress.value = true
  loadFinalizationState()
    .then(() => {
      if (!isInFinalizeState.value) {
        const projAndSkillIds = selectedRows.value.map((skill) => ({
          projectId: skill.projectId,
          skillId: skill.skillId
        }))
        const commonActionsAfterImport = () => {
          subjectState.loadSubjectDetailsState()
          finalizeState.loadInfo()
          SkillsReporter.reportSkill('ImportSkillfromCatalog')
        }
        if (props.groupId && props.groupId.length > 0) {
          CatalogService.bulkImportIntoGroup(route.params.projectId, route.params.subjectId, props.groupId, projAndSkillIds)
            .then(() => {
              skillsState.loadGroupSkills(route.params.projectId, props.groupId)
              commonActionsAfterImport()
            }).finally(() => {
            handleClose()
          })

        } else {
          CatalogService.bulkImport(route.params.projectId, route.params.subjectId, projAndSkillIds)
            .then(() => {
              skillsState.loadSubjectSkills(route.params.projectId, route.params.subjectId)
              commonActionsAfterImport()
            }).finally(() => {
            handleClose()
          })
        }
      } else {
        selectedRows.value = []
      }
    }).finally(() => {
    importInProgress.value = false
  })
}
const reset = () => {
  filters.value.skillName = ''
  filters.value.projectName = ''
  filters.value.subjectName = ''
  loadData()
}
const setProjectFilter = (projectName) => {
  filters.value.projectName = projectName
  loadData()
}
const setSubjectFilter = (projectName) => {
  filters.value.subjectName = projectName
  loadData()
}
const isImportBtnDisabled = computed(() => {
  return importDisabled.value || importInProgress.value || maxBulkImportExceeded.value || maxSkillsInSubjectExceeded.value
})

const focusState = useFocusState()
const handleClose = () => {
  model.value = false
  focusState.focusOnLastElement()
}
const rowClass = (row) => (row.skillIdAlreadyExist || row.skillNameAlreadyExist) ? 'remove-checkbox' : ''

const onUpdateVisible = (newVal) => {
  if (!newVal) {
    handleClose()
  }
}
</script>

<template>
  <Dialog
    modal
    header="Import Skills from the Catalog"
    :maximizable="true"
    :close-on-escape="true"
    class="w-11 xl:w-8"
    @update:visible="onUpdateVisible"
    v-model:visible="model"
    :pt="{ maximizableButton: { 'aria-label': 'Expand to full screen and collapse back to the original size of the dialog' } }"
  >
    <skills-spinner :is-loading="initialLoad" />
    <div v-if="!initialLoad">
      <no-content2 v-if="emptyCatalog && !isInFinalizeState" class="mt-4 mb-5"
                   icon="fas fa-user-clock"
                   title="Nothing Available for Import" data-cy="catalogSkillImportModal-NoData">
        When other projects export Skills to the Catalog then they will be available here
        to be imported.
      </no-content2>

      <no-content2 v-if="isInFinalizeState" class="mt-4 mb-5"
                   title="Finalization in Progress" data-cy="catalogSkillImport-finalizationInProcess">
        Cannot import while imported skills are being finalized. Unfortunately will have to wait, thank you for the
        patience!
      </no-content2>


      <div v-if="showTable">
        <div class="md:flex gap-2">
          <div class="field mt-2 md:mt-0 mb-0 w-full md:w-auto">
            <label for="skill-name-filter">Skill Name:</label>
            <InputText
              id="skill-name-filter"
              v-model="filters.skillName"
              v-on:keydown.enter="loadData"
              data-cy="skillNameFilter"
              maxlength="50"
              class="w-full" />
          </div>
          <div class="field mt-2 md:mt-0 mb-0 w-full md:w-auto">
            <label for="project-name-filter">Project Name:</label>
            <InputText
              id="project-name-filter"
              v-model="filters.projectName"
              v-on:keydown.enter="loadData"
              data-cy="projectNameFilter"
              maxlength="50"
              class="w-full" />
          </div>
          <div class="field mt-2 md:mt-0 mb-0 w-full md:w-auto">
            <label for="subject-name-filter">Subject Name:</label>
            <InputText
              id="subject-name-filter"
              v-model="filters.subjectName"
              v-on:keydown.enter="loadData"
              data-cy="subjectNameFilter"
              maxlength="50"
              class="w-full" />
          </div>
        </div>
        <div class="mb-3 mt-1">
          <SkillsButton
            label="Filter"
            icon="fa fa-filter"
            severity="primary"
            @click="loadData"
            size="small"
            outlined
            class="mt-1"
            data-cy="filterBtn" />
          <SkillsButton
            label="Reset"
            icon="fa fa-times"
            outlined
            size="small"
            severity="primary"
            @click="reset"
            class="ml-1 mt-1"
            data-cy="filterResetBtn" />

        </div>

        <SkillsDataTable
          tableStoredStateId="importSkillsFromCatalog"
          aria-label="Import Skills"
          :value="data"
          :loading="reloadData"
          v-model:selection="selectedRows"
          v-model:expandedRows="expandedRows"
          v-model:sort-field="sortInfo.sortBy"
          v-model:sort-order="sortInfo.sortOrder"
          :auto-max-width="false"
          stripedRows
          paginator
          lazy
          :totalRecords="totalRows"
          :rows="pageSize"
          @page="pageChanged"
          :rowClass="rowClass"
          :expander="true"
          data-cy="importSkillsFromCatalogTable"
          :rowsPerPageOptions="possiblePageSizes">
          <!--      <template #loading> Loading customers data. Please wait. </template>-->
          <Column selectionMode="multiple" :class="{'flex': responsive.md.value }">
            <template #header>
              <span class="mr-1 lg:mr-0 md:hidden"><i class="fas fa-check-double"
                                                      aria-hidden="true"></i> Select Rows:</span>
            </template>
          </Column>
          <Column field="name" header="Skill" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-user mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <skill-already-existing-warning :skill="slotProps.data" />
              <div class="max-wrap">
                {{ slotProps.data.name }}
              </div>
            </template>
          </Column>
          <Column field="projectName" header="Project" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-tasks mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <div class="flex align-items-center">
                <div class="flex-1">
                  {{ slotProps.data.projectName }}
                </div>
                <div>
                  <SkillsButton
                    aria-label="Filter by Project Name"
                    @click="setProjectFilter(slotProps.data.projectName)"
                    data-cy="addProjectFilter"
                    icon="fas fa-search-plus"
                    size="small"
                    rounded text />
                </div>
              </div>
            </template>
          </Column>
          <Column field="subjectName" header="Subject" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-cubes mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <div class="flex align-items-center">
                <div class="flex-1">
                  {{ slotProps.data.subjectName }}
                </div>
                <div>
                  <SkillsButton
                    aria-label="Filter by Subject Name"
                    data-cy="addSubjectFilter"
                    @click="setSubjectFilter(slotProps.data.subjectName)"
                    icon="fas fa-search-plus"
                    size="small" rounded text />
                </div>
              </div>
            </template>
          </Column>
          <Column field="totalPoints" header="Points" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-arrow-alt-circle-up mr-1" aria-hidden="true"></i>
            </template>
          </Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
          </template>
          <template #expansion="slotProps">
            <skill-to-import-info :skill="slotProps.data" />
          </template>
          <template #empty>
            <div class="text-center">
              <i class="fas fa-exclamation-circle" aria-hidden="true" /> There are no records to show
            </div>
          </template>
        </SkillsDataTable>
      </div>

      <Message v-if="maxBulkImportExceeded" data-cy="maximum-selected" :closable="false" severity="warn">
        Cannot import more than
        <Tag>{{ appConfig.maxSkillsInBulkImport }}</Tag>
        Skills at once
      </Message>
      <Message v-if="maxSkillsInSubjectExceeded" data-cy="maximum-selected" :closable="false" severity="warn">
        No more than
        <Tag>{{ appConfig.maxSkillsPerSubject }}</Tag>
        Skills per Subject are allowed, this project already has
        <Tag>{{ skillsState.totalNumSkillsInSubject }}</Tag>
      </Message>

      <div v-if="!emptyCatalog" class="text-right mt-3">
        <SkillsButton
          label="Cancel"
          icon="fas fa-times"
          severity="warning"
          size="small"
          class="mr-2"
          outlined
          @click="handleClose"
          data-cy="closeButton" />
        <SkillsButton
          v-if="!emptyCatalog"
          severity="success"
          size="small"
          outlined
          @click="doImport"
          data-cy="importBtn"
          badge="2"
          :disabled="isImportBtnDisabled">Import
          <!--          <i class="far fa-arrow-alt-circle-down ml-1" aria-hidden="true"/>-->
          <Badge :value="selectedRows.length" severity="info" data-cy="numSelectedSkills" class="ml-2" />
        </SkillsButton>
      </div>
    </div>

    <div class="loading-indicator" v-if="importInProgress">
      <skills-spinner :is-loading="true" />
    </div>

  </Dialog>
</template>

<style scoped>
.max-wrap {
  max-width: 20rem;
  word-wrap: break-word;
  display: inline-block;
}

.loading-indicator {
  position: fixed;
  z-index: 999;
  height: 2em;
  width: 2em;
  overflow: show;
  margin: auto;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
}

/* Transparent Overlay */
.loading-indicator:before {
  content: '';
  display: block;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(222, 217, 217, 0.53);
}

</style>

<style>
.remove-checkbox .p-checkbox {
  visibility: hidden !important;
}
</style>