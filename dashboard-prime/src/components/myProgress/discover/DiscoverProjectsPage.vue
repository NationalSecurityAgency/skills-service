<script setup>
import { computed, onMounted, ref } from 'vue'
import ProjectService from '@/components/projects/ProjectService.js'
import Column from 'primevue/column'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import { FilterMatchMode } from 'primevue/api'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog.vue'
import ProjectDescriptionRow from '@/components/myProgress/discover/ProjectDescriptionRow.vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'

const responsive = useResponsiveBreakpoints()
const announcer = useSkillsAnnouncer()
const appInfoState = useAppInfoState()
const myProgressState = useMyProgressState()

const isLoading = ref(true)
const searchValue = ref('')
const originalProjects = ref([])
const projects = ref([])
const totalRows = ref(0)
const pageSize = ref(5)
const possiblePageSizes = [5, 10, 15, 25, 50]
const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS }
})
const firstRow = ref(0)
const countsAll = ref(0)
const countsMyProjects = ref(0)
const countsDiscoverProjects = ref(0)
const expandedRows = ref([])

onMounted(() => {
  loadAll()
})

const loadAll = () => {
  searchValue.value = ''
  isLoading.value = true
  ProjectService.getAvailableForMyProjects()
    .then((response) => {
      originalProjects.value = response.map((item) => ({ loading: false, ...item }))
      // need a shallow copy
      projects.value = originalProjects.value.map((item) => item)
      totalRows.value = projects.value.length
      updateCounts()
    })
    .finally(() => {
      isLoading.value = false
    })
}

const updateCounts = () => {
  countsAll.value = originalProjects.value.length
  countsMyProjects.value = originalProjects.value.filter((item) => item.isMyProject).length
  countsDiscoverProjects.value = countsAll.value - countsMyProjects.value
}

const hasProjects = computed(() => originalProjects.value.length > 0)
const sortInfo = ref({ sortOrder: -1, sortBy: 'name' })
const clearFilter = () => {
  firstRow.value = 0
  filters.value.global.value = null
  announcer.polite('Skills filter was reset. Showing all results')
}

const addToMyProjects = (item) => {
  const itemRef = item
  itemRef.loading = true
  ProjectService.addToMyProjects(item.projectId)
    .then(() => {
      itemRef.isMyProject = true
      updateCounts()
      return myProgressState.loadMyProgressSummary()
        .then(() => {
          announcer.polite(`${item.name} has been added to my projects`)
        })
    })
    .finally(() => {
      itemRef.loading = false
    })
}
const removeFromMyProjects = (item) => {
  const itemRef = item
  itemRef.loading = true
  ProjectService.removeFromMyProjects(item.projectId)
    .then(() => {
      itemRef.isMyProject = false
      updateCounts()
      return myProgressState.loadMyProgressSummary()
        .then(() => {
          announcer.polite(`${item.name} has been removed from my projects`)
        })
    })
    .finally(() => {
      itemRef.loading = false
    })
}

const contactModal = ref({
  show: false,
  projectId: null,
  projectName: null
})
const contactProject = (name, id) => {
  contactModal.value.projectName = name
  contactModal.value.projectId = id
  contactModal.value.show = true
}
</script>

<template>
  <Card>
    <template #header>
      <div class="flex pt-4 px-3">
        <div class="flex-1">
          <span class="text-2xl uppercase">Projects Catalog</span>
        </div>
        <div>
          <router-link :to="{ name: 'MyProgressPage' }">
            <SkillsButton
              label="Back to My Progress"
              icon="fas fa-arrow-alt-circle-left"
              outlined
              data-cy="backToProgressAndRankingBtn"
              variant="outline-primary" />
          </router-link>
        </div>
      </div>
    </template>
    <template #content>
      <skills-spinner :is-loading="isLoading" class="mt-8" />
      <div v-if="!isLoading">
        <div v-if="!hasProjects">
        </div>
        <div v-if="hasProjects">
          <SkillsDataTable
            class="border-1 surface-border border-round border-surface"
            :value="projects"
            stripedRows
            paginator
            :expander="true"
            :totalRecords="totalRows"
            :rows="pageSize"
            :rowsPerPageOptions="possiblePageSizes"
            v-model:expandedRows="expandedRows"
            v-model:sort-field="sortInfo.sortBy"
            v-model:sort-order="sortInfo.sortOrder"
            v-model:filters="filters"
            v-model:first="firstRow"
            :globalFilterFields="['name']"
            data-cy="discoverProjectsTable"
            tableStoredStateId="projectCatalogTable">

            <template #header>
              <div class="flex gap-3 align-items-end">
                <div class="flex-1">
                  <label for="projectFilter">Project Name Search:</label>
                  <InputGroup class="mt-2">
                    <InputGroupAddon>
                      <i class="fas fa-search" aria-hidden="true" />
                    </InputGroupAddon>
                    <InputText
                      id="projectFilter"
                      class="flex flex-grow-1"
                      v-model="filters['global'].value"
                      data-cy="skillsTable-skillFilter"
                      placeholder="Project Search" />
                    <InputGroupAddon class="p-0 m-0">
                      <SkillsButton
                        id="skillsFilterResetBtn"
                        icon="fa fa-times"
                        text
                        outlined
                        @click="clearFilter"
                        aria-label="Reset project filter"
                        data-cy="filterResetBtn" />
                    </InputGroupAddon>
                  </InputGroup>
                </div>
                <div class="flex gap-2">
                  <media-info-card :title="countsAll"
                                   sub-title="ALL PROJECTS"
                                   icon-class="fas fa-globe"
                                   data-cy="allProjectsCount">
                  </media-info-card>
                  <media-info-card :title="countsMyProjects" sub-title="MY PROJECTS"
                                   icon-class="fas fa-heart"
                                   data-cy="myProjectCount">
                  </media-info-card>
                  <media-info-card :title="countsDiscoverProjects" sub-title="DISCOVER NEW"
                                   icon-class="fas fa-search"
                                   data-cy="discoverNewProjCount">
                  </media-info-card>
                </div>
              </div>
            </template>

            <Column field="name" header="Project" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-tasks mr-1" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <div class="flex align-items-center">
                  <div class="flex-1">
                    {{ slotProps.data.name }}
                  </div>
                  <div v-if="appInfoState.emailEnabled">
                    <SkillsButton
                      label="Contact Project"
                      icon="fas fas fa-mail-bulk"
                      outlined
                      severity="info"
                      :aria-label="`Contact ${slotProps.data.name} project owner`"
                      size="small"
                      @click="contactProject(slotProps.data.name, slotProps.data.projectId)"
                      :data-cy="`contactOwnerBtn_${ slotProps.data.projectId }`" />
                  </div>
                </div>

              </template>
            </Column>
            <Column field="isMyProject" header="My Projects" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-heart mr-1" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <SkillsButton
                  v-if="!slotProps.data.isMyProject"
                  label="Add"
                  icon="fas fa-plus-circle"
                  @click="addToMyProjects(slotProps.data) "
                  outlined
                  class="fadein animation-duration-300"
                  size="small"
                  data-cy="addButton"
                  :loading="slotProps.data.loading"
                  :aria-label="`add project ${slotProps.data.projectId} to my projects`" />
                <div v-if="slotProps.data.isMyProject" class="flex align-items-center">
                  <Tag
                    class="animate__bounceIn"
                    severity="success"><i class="fas fa-heart mr-1" /> My Project
                  </Tag>
                  <SkillsButton
                    icon="fas fa-times-circle"
                    @click="removeFromMyProjects(slotProps.data)"
                    outlined
                    class="ml-2"
                    size="small"
                    severity="warning"
                    data-cy="removeBtn"
                    :loading="slotProps.data.loading"
                    :aria-label="`remove project ${slotProps.data.projectId} from my projects`" />
                </div>
              </template>
            </Column>
            <Column field="numSkills" header="Skills" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-graduation-cap mr-1" aria-hidden="true"></i>
              </template>
            </Column>
            <Column field="numSubjects" header="Subjects" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-cubes mr-1" aria-hidden="true"></i>
              </template>
            </Column>
            <Column field="numBadges" header="Badges" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-arrow-alt-circle-up mr-1" aria-hidden="true"></i>
              </template>
            </Column>
            <Column field="totalPoints" header="Points" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fas fa-award mr-1" aria-hidden="true"></i>
              </template>
            </Column>

            <template #expansion="slotProps">
              <project-description-row :project-id="slotProps.data.projectId" />
            </template>
            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
            </template>
          </SkillsDataTable>
        </div>
      </div>

      <contact-owners-dialog
        v-if="contactModal.show && appInfoState.emailEnabled"
        v-model="contactModal.show"
        :projectName="contactModal.projectName"
        :projectId="contactModal.projectId" />
    </template>
  </Card>
</template>

<style scoped>

</style>