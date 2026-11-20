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
import {computed, onMounted, ref, watch} from 'vue'
import ProjectService from '@/components/projects/ProjectService.js'
import Column from 'primevue/column'
import {useResponsiveBreakpoints} from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import {useSkillsAnnouncer} from '@/common-components/utilities/UseSkillsAnnouncer.js'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import {useAppInfoState} from '@/stores/UseAppInfoState.js'
import ProjectDescriptionRow from '@/components/myProgress/discover/ProjectDescriptionRow.vue'
import {useMyProgressState} from '@/stores/UseMyProgressState.js'
import NoProjectsInCatalogMsg from '@/components/myProgress/discover/NoProjectsInCatalogMsg.vue'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import {useColors} from '@/skills-display/components/utilities/UseColors.js'
import MyProgressTitle from "@/components/myProgress/MyProgressTitle.vue";
import BackToMyProgressBtn from "@/components/myProgress/BackToMyProgressBtn.vue";
import ContactProjectAdminsDialog from "@/components/contact/ContactProjectAdminsDialog.vue";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import {useDebounceFn, useStorage} from "@vueuse/core";

const responsive = useResponsiveBreakpoints()
const announcer = useSkillsAnnouncer()
const appInfoState = useAppInfoState()
const myProgressState = useMyProgressState()
const colors = useColors()
const numberFormat = useNumberFormat()

const isLoading = ref(true)
const originalProjects = ref([])
const projects = ref([])
const pageSize = useStorage('discoverProjects-pageSize', 5)
const possiblePageSizes = [5, 10, 15, 25, 50]
const searchText = ref('')
// the service searches project descriptions, subjects and skills; results are keyed by projectId
const searchMatches = ref(new Map())
const isSearching = ref(false)
let searchRequestNum = 0
const maxSearchLength = 100
const maxSearchWords = 10
const firstRow = ref(0)
const countsAll = ref(0)
const countsMyProjects = ref(0)
const countsDiscoverProjects = ref(0)
const expandedRows = ref([])

onMounted(() => {
  loadAll()
})

const loadAll = () => {
  searchText.value = ''
  isLoading.value = true
  ProjectService.getAvailableForMyProjects()
      .then((response) => {
        originalProjects.value = response.map((item) => ({loading: false, ...item}))
        // need a shallow copy
        projects.value = originalProjects.value.map((item) => item)
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
const sortInfo = ref({sortOrder: 1, sortBy: 'name'})

const searchWords = computed(() => searchText.value.trim().toLowerCase().split(/\s+/).filter((word) => word).slice(0, maxSearchWords))
const isSearchActive = computed(() => searchWords.value.length > 0)
// every word must appear in the project name, or the project must be returned by the service search
const filteredProjects = computed(() => {
  if (!isSearchActive.value) {
    return projects.value
  }
  return projects.value.filter((project) => {
    const name = project.name.toLowerCase()
    return searchWords.value.every((word) => name.includes(word)) || searchMatches.value.has(project.projectId)
  })
})
const totalRows = computed(() => filteredProjects.value.length)

const runSearch = useDebounceFn((words) => {
  const requestNum = ++searchRequestNum
  if (words.length === 0) {
    searchMatches.value = new Map()
    isSearching.value = false
    return
  }
  isSearching.value = true
  ProjectService.searchAvailableForMyProjects(words.join(' '))
      .then((results) => {
        if (requestNum !== searchRequestNum) {
          return
        }
        searchMatches.value = new Map(results.map((result) => [result.projectId, result]))
        announcer.polite(`Search for ${words.join(' ')} returned ${filteredProjects.value.length} projects`)
      })
      .finally(() => {
        if (requestNum === searchRequestNum) {
          isSearching.value = false
        }
      })
}, 400)

watch(searchWords, (words, oldWords) => {
  if (words.join(' ') === (oldWords || []).join(' ')) {
    return
  }
  firstRow.value = 0
  searchMatches.value = new Map()
  runSearch(words)
})

// only matches that the project name doesn't explain are shown: skills and description
const matchFor = (projectId) => {
  const match = isSearchActive.value ? searchMatches.value.get(projectId) : null
  return match && (match.numMatchedSkills > 0 || match.descriptionMatched) ? match : null
}
const moreSkillsCount = (match) => match.numMatchedSkills - match.matchedSkills.length

const clearFilter = () => {
  firstRow.value = 0
  searchText.value = ''
  announcer.polite('Project search was reset. Showing all results')
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
  <div>
    <my-progress-title title="Projects Catalog">
      <template #rightContent>
        <back-to-my-progress-btn/>
      </template>
    </my-progress-title>
    <Card class="mt-4" :pt="{  content: { class: 'p-0' }, body: { class: 'p-0' } }">
      <template #content>
        <skills-spinner :is-loading="isLoading" class="mt-20"/>
        <div v-if="!isLoading">
          <div v-if="!hasProjects">
            <no-projects-in-catalog-msg/>
          </div>
          <div v-if="hasProjects">

            <div class="flex flex-col md:flex-row gap-2 pt-4 px-4">
              <media-info-card :title="countsAll"
                               sub-title="ALL PROJECTS"
                               class="flex-1"
                               :icon-class="`fas fa-globe ${colors.getTextClass(1)}`"
                               data-cy="allProjectsCount">
              </media-info-card>
              <media-info-card :title="countsMyProjects"
                               sub-title="MY PROJECTS"
                               class="flex-1"
                               :icon-class="`fas fa-heart ${colors.getTextClass(2)}`"
                               data-cy="myProjectCount">
              </media-info-card>
              <media-info-card :title="countsDiscoverProjects"
                               sub-title="DISCOVER NEW"
                               class="flex-1"
                               :icon-class="`fas fa-search ${colors.getTextClass(3)}`"
                               data-cy="discoverNewProjCount">
              </media-info-card>
            </div>

            <InputGroup class="mt-2 pt-6 px-4 pb-2">
              <InputGroupAddon>
                <i class="fas fa-search" aria-hidden="true"/>
              </InputGroupAddon>
              <InputText
                  id="projectFilter"
                  class="flex grow"
                  v-model="searchText"
                  :maxlength="maxSearchLength"
                  data-cy="searchInput"
                  aria-label="Search project names, descriptions and skills"
                  placeholder="Search project names, descriptions and skills"/>
              <InputGroupAddon v-if="isSearching" data-cy="searchInProgress">
                <i class="fas fa-spinner fa-spin" aria-hidden="true"/>
                <span class="sr-only">Searching</span>
              </InputGroupAddon>
              <InputGroupAddon class="p-0 m-0">
                <SkillsButton
                    id="skillsFilterResetBtn"
                    icon="fa fa-times"
                    text
                    outlined
                    @click="clearFilter"
                    aria-label="Reset project filter"
                    data-cy="filterResetBtn"/>
              </InputGroupAddon>
            </InputGroup>

            <SkillsDataTable
                class="border border-surface rounded-border border-surface"
                style="max-width: 100%"
                :value="filteredProjects"
                stripedRows
                paginator
                :expander="true"
                :totalRecords="totalRows"
                :rows="pageSize"
                :rowsPerPageOptions="possiblePageSizes"
                v-model:expandedRows="expandedRows"
                v-model:sort-field="sortInfo.sortBy"
                v-model:sort-order="sortInfo.sortOrder"
                v-model:first="firstRow"
                data-cy="discoverProjectsTable"
                aria-label="Projects"
                tableStoredStateId="projectCatalogTable">

              <template #empty>
                <p>
                  No results found! Please modify your search string: <span
                    class="text-primary">[{{ searchText }}]</span>
                </p>
              </template>

              <Column field="name" header="Project" :sortable="true" :class="{'flex': responsive.md.value }">
                <template #header>
                  <i class="fas fa-tasks mr-1" aria-hidden="true"></i>
                </template>
                <template #body="slotProps">
                  <div class="flex items-center">
                    <div class="flex-1">
                      <highlighted-value :value="slotProps.data.name" :filter="searchText"/>
                      <div v-if="matchFor(slotProps.data.projectId)"
                           class="text-sm text-muted-color mt-1"
                           :data-cy="`searchMatch_${slotProps.data.projectId}`">
                        <div v-if="matchFor(slotProps.data.projectId).numMatchedSkills > 0" data-cy="matchedSkills">
                          Matched skills: {{ matchFor(slotProps.data.projectId).matchedSkills.join(', ') }}<span
                            v-if="moreSkillsCount(matchFor(slotProps.data.projectId)) > 0"> (+{{ moreSkillsCount(matchFor(slotProps.data.projectId)) }} more)</span>
                        </div>
                        <div v-if="matchFor(slotProps.data.projectId).descriptionMatched" data-cy="matchedDescription">
                          Matched in description
                        </div>
                      </div>
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
                          :data-cy="`contactOwnerBtn_${ slotProps.data.projectId }`"/>
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
                      class="animate-fadein animate-duration-300"
                      size="small"
                      :data-cy="`addButton-${slotProps.data.projectId}`"
                      :loading="slotProps.data.loading"
                      :aria-label="`add project ${slotProps.data.projectId} to my projects`"/>
                  <router-link :to="{ path: `/progress-and-rankings/projects/${slotProps.data.projectId}` }"
                               tabindex="-1" v-if="!slotProps.data.isMyProject">
                    <SkillsButton
                        label="Preview"
                        icon="fas fa-eye"
                        outlined
                        class="animate-fadein animate-duration-300 ml-2"
                        size="small"
                        :data-cy="`viewButton-${slotProps.data.projectId}`"
                        :loading="slotProps.data.loading"
                        :aria-label="`preview project ${slotProps.data.projectId}`"/>
                  </router-link>
                  <div v-if="slotProps.data.isMyProject" class="flex items-center">
                    <Tag
                        class="animate__bounceIn"
                        severity="success"><i class="fas fa-heart mr-1"/> My Project
                    </Tag>
                    <SkillsButton
                        icon="fas fa-times-circle"
                        @click="removeFromMyProjects(slotProps.data)"
                        outlined
                        class="ml-2"
                        size="small"
                        severity="warn"
                        :data-cy="`removeBtn-${slotProps.data.projectId}`"
                        :loading="slotProps.data.loading"
                        :aria-label="`remove project ${slotProps.data.projectId} from my projects`"/>
                  </div>
                </template>
              </Column>
              <Column field="numSkills" header="Skills" :sortable="true" :class="{'flex': responsive.md.value }">
                <template #header>
                  <i class="fas fa-graduation-cap mr-1" aria-hidden="true"></i>
                </template>
              </Column>

              <template #expansion="slotProps">
                <div>
                  <p>
                    <Tag>{{ numberFormat.pretty(slotProps.data.numSubjects) }}</Tag>
                    Subjects
                  </p>
                  <p class="my-1">
                    <Tag>{{ numberFormat.pretty(slotProps.data.numBadges) }}</Tag>
                    Badges
                  </p>
                  <p>
                    <Tag>{{ numberFormat.pretty(slotProps.data.totalPoints) }}</Tag>
                    Points
                  </p>
                  <project-description-row :project-id="slotProps.data.projectId"/>
                </div>
              </template>
              <template #paginatorstart>
                <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{
                  totalRows
                }}</span>
              </template>
            </SkillsDataTable>
          </div>
        </div>

        <contact-project-admins-dialog
            v-if="contactModal.show && appInfoState.emailEnabled"
            v-model="contactModal.show"
            :projectId="contactModal.projectId"/>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>