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
import { computed, nextTick, onMounted, provide, ref } from 'vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useElementHelper } from '@/components/utils/inputForm/UseElementHelper.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import ProjectService from '@/components/projects/ProjectService'
import MyProject from '@/components/projects/MyProject.vue'
import EditProject from '@/components/projects/EditProject.vue'
import { SkillsReporter } from '@skilltree/skills-client-js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAccessState } from '@/stores/UseAccessState.js'
import PinProjects from '@/components/projects/PinProjects.vue'
import Sortable from 'sortablejs'
import BlockUI from 'primevue/blockui'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import SettingsService from '@/components/settings/SettingsService.js'
import LengthyOperationProgressBarModal from '@/components/utils/modal/LengthyOperationProgressBarModal.vue'
import { useAdminProjectsState } from '@/stores/UseAdminProjectsState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import LinkToSkillPage from "@/components/utils/LinkToSkillPage.vue";
import {useRouter} from "vue-router";

const router = useRouter()
const appConfig = useAppConfig()
const accessState = useAccessState()
const announcer = useSkillsAnnouncer()
const elementHelper = useElementHelper()
const projectsState = useAdminProjectsState()
const log = useLog()

onMounted(() => {
  loadProjects()
})

const projRef = ref([])
const isLoading = computed(() => projectsState.isLoadingProjects)
const projects = computed(() => projectsState.projects)
const newProject = ref({
  show: false,
  isEdit: false,
  project: {}
})
const showSearchProjectModal = ref(false)
const sortOrder = ref({
  loading: false,
  loadingProjectId: '-1'
})
const copyProgressModal = ref({
  show: false,
  isComplete: false,
  hasFailed: false,
  skillIdThatFailedParagraphValidation: null,
  isSubject: false,
  isBadge: false,
  isSkill: false,
  isVideoTranscript: false,
  copiedProjectId: '',
  originalProjectId: '',
})

const addProjectDisabled = computed(() => {
  return projects.value && projects.value.length >= appConfig.maxProjectsPerAdmin && !accessState.isRoot
})

const addProjectsDisabledMsg = computed(() => `The maximum number of Projects allowed is ${appConfig.maxProjectsPerAdmin}`)
const isRootUser = computed(() => accessState.isRoot)

// Functions
const pinModalClosed = () => {
  showSearchProjectModal.value = false
  loadProjects()
  nextTick(() => {
    document.getElementById('pinProjectsButton').focus()
  })
}
const projectUnpinned = (project) => {
  loadProjects().then(() => {
    nextTick(() => {
      announcer.polite(`Project ${project.name} has been unpinned from the root user projects view`)
      nextTick(() => {
        document.getElementById('pinProjectsButton').focus()
      })
    })
  })
}
const loadProjects = () => {
  return projectsState.loadProjects()
    .finally(() => {
      enableDropAndDrop()
    })
}
const projectRemoved = (project) => {
  ProjectService.deleteProject(project.projectId)
    .then(() => {
      loadProjects()
      announcer.polite(`Project ${project.name} has been deleted`)
    })
}
const copyProject = (projectInfo) => {
  copyProgressModal.value.isComplete = false
  copyProgressModal.value.hasFailed = false
  copyProgressModal.value.isSubject = false
  copyProgressModal.value.isBadge = false
  copyProgressModal.value.isSkill = false
  copyProgressModal.value.isVideoTranscript = false
  copyProgressModal.value.skillIdThatFailedParagraphValidation = null
  copyProgressModal.value.copiedProjectId = ''
  copyProgressModal.value.originalProjectId = projectInfo.originalProjectId
  copyProgressModal.value.show = true
  ProjectService.copyProject(projectInfo.originalProjectId, projectInfo.newProject)
    .then(() => {
      copyProgressModal.value.copiedProjectId = projectInfo.newProject.projectId
      copyProgressModal.value.isComplete = true
      announcer.polite(`Project ${projectInfo.newProject.name} was copied`)
      SkillsReporter.reportSkill('CopyProject')
      loadProjectsAfterCopy()
    }).catch((err) => {
      const isParagraphValidationFailed = err && err.response && err.response.data && err.response.data.errorCode === 'ParagraphValidationFailed'
      if (isParagraphValidationFailed) {
        const explanation = err.response.data.explanation
        const isSubject = explanation.includes('Failed to copy a subject')
        const isBadge = explanation.includes('Failed to copy a badge')
        const isSkill = explanation.includes('Failed to copy a skill')
        const isVideoTranscript = explanation.includes('Video transcript validation failed')
        copyProgressModal.value.isSubject = isSubject
        copyProgressModal.value.isBadge = isBadge
        copyProgressModal.value.isSkill = isSkill
        copyProgressModal.value.isVideoTranscript = isVideoTranscript
        copyProgressModal.value.hasFailed = true
        copyProgressModal.value.isComplete = true
        const failedSkillId = err.response.data.skillId
        copyProgressModal.value.skillIdThatFailedParagraphValidation = failedSkillId
        announcer.polite(`Project ${projectInfo.newProject.name} failed to be copied due to paragraph validation for skill with id ${failedSkillId}`)
      } else {
        router.push({ name: 'ErrorPage', query: { err } });
      }
  });
}
const loadProjectsAfterCopy = () => {
  loadProjects()
    .then(() => {
      focusOnProjectCard(copyProgressModal.value.copiedProjectId)
    })
}


const openProjectModal = (project = {}, isEdit = false) => {
  newProject.value.isEdit = isEdit
  newProject.value.project = project
  newProject.value.show = true
}
provide('createOrUpdateProject', openProjectModal)

const projectAdded = (project) => {
  const isUpdated = projectsState.updateOrAddProject(project)
  if (isUpdated) {
    announcer.polite(`Project ${project.name} has been updated`)
  } else {
    SkillsReporter.reportSkill('CreateProject')
    announcer.polite(`Project ${project.name} has been created`)
  }
}
const enableDropAndDrop = () => {
  if (projects.value && projects.value.length > 0) {
    nextTick(() => {
      const projectCardId = 'projectCards'
      elementHelper.getElementById(projectCardId).then((cards) => {
        // need to check for null because this logic is within nextTick method
        // an may actually run after the user moved onto another page
        if (cards) {
          Sortable.create(cards, {
            handle: '.sort-control',
            animation: 150,
            ghostClass: 'skills-sort-order-ghost-class',
            onUpdate(event) {
              sortOrderUpdate(event)
            }
          })
        } else {
          log.error(`Failed to find element with id [${projectCardId}] sort will not work property`)
        }
      })
    })
  }
}
const sortOrderUpdate = (updateEvent) => {
  const { id } = updateEvent.item
  sortOrder.value.loadingProjectId = id
  sortOrder.value.loading = true
  ProjectService.updateProjectDisplaySortOrder(id, updateEvent.newIndex)
    .finally(() => {
      sortOrder.value.loading = false
    })
}
const updateSortAndReloadProjects = (updateInfo) => {
  const currentIndex = projects.value.sort((a, b) => {
    if (a.displayOrder > b.displayOrder) {
      return 1
    }
    if (b.displayOrder > a.displayOrder) {
      return -1
    }
    return 0
  })
    .findIndex((item) => item.projectId === updateInfo.projectId)
  const newIndex = updateInfo.direction === 'up' ? currentIndex - 1 : currentIndex + 1
  if (newIndex >= 0 && (newIndex) < projects.value.length) {
    isLoading.value = true
    ProjectService.updateProjectDisplaySortOrder(updateInfo.projectId, newIndex)
      .finally(() => {
        loadProjects()
          .then(() => {
            const foundRef = projRef.value[updateInfo.projectId]
            nextTick(() => {
              foundRef.focusSortControl()
            })
          })
      })
  }
}

const saveProject = (values, isEdit, projectId) => {
  const projToSave = {
    ...values,
    originalProjectId: projectId,
    isEdit: isEdit,
    name: InputSanitizer.sanitize(values.projectName),
    projectId: InputSanitizer.sanitize(values.projectId)
  }
  return ProjectService.saveProject(projToSave)
    .then((projRes) => {
      if (!isEdit && isRootUser.value) {
        SettingsService.pinProject(projToSave.projectId)
          .then(() => {
            return { ...projRes, originalProjectId: projectId }
          })
      }
      return ProjectService.getProject(projRes.projectId)
        .then((retrievedProj) => {
          const projWithOriginalId = { ...retrievedProj, originalProjectId: projectId }
          projectAdded(projWithOriginalId)
          return projWithOriginalId
        })
    })
}

const focusOnProjectCard = (projectId) => {
  nextTick(() => {
    const projCard = document.getElementById(`proj${projectId}`)
    if (projCard) {
      projCard.focus()
    }
  })
}

const hasData = computed(() => {
  return !isLoading.value && projects.value && projects.value.length > 0
})

const createNewProject = () => {
  newProject.value.isEdit = false
  newProject.value.project = {}
  newProject.value.show = true
};
</script>

<template>
  <div>
    <SubPageHeader title="Projects" action="Project" :title-level="1">
      <SkillsButton
        v-if="isRootUser"
        label="Pin"
        icon="fas fa-thumbtack"
        outlined
        id="pinProjectsButton"
        ref="pinProjectsButton"
        @click="showSearchProjectModal=true"
        aria-label="Pin projects to your Project page"
        role="button"
        size="small"
        data-cy="pinProjectsButton"
        class="mr-2 text-primary bg-primary-contrast" />
      <SkillsButton
        label="Project"
        icon="fas fa-plus-circle"
        id="newProjectBtn"
        ref="newProjButton"
        @click="createNewProject"
        outlined class="text-primary bg-primary-contrast"
        size="small"
        :disabled="addProjectDisabled"
        data-cy="newProjectButton"
        aria-label="Create new Project"
        :track-for-focus="true"
        role="button" />

      <div v-if="addProjectDisabled" class="mt-1">
        <InlineMessage severity="warn"
                       icon="fas fa-exclamation-circle"
                       data-cy="addProjectDisabledWarning">
          {{ addProjectsDisabledMsg }}
        </InlineMessage>
      </div>
    </SubPageHeader>

    <SkillsSpinner :is-loading="isLoading" class="my-8" />

    <div v-if="hasData" id="projectCards"
         :class="{
      'grid grid-cols-1 lg:grid-cols-2 2xl:grid-cols-3 gap-4': projectsState.shouldTileProjectsCards,
      '': !projectsState.shouldTileProjectsCards
    }">
      <div v-for="project of projects"
           :key="project.projectId"
           class="mb-4"
           :class="{
            '': projectsState.shouldTileProjectsCards,
            '': !projectsState.shouldTileProjectsCards
            }"
           :id="project.projectId">
        <BlockUI :blocked="sortOrder.loading" class="h-full">
          <div v-if="sortOrder.loading"
               class="text-center loading-indicator"
               :data-cy="`${project.projectId}_overlayShown`">
            <SkillsSpinner
              v-if="project.projectId === sortOrder.loadingProjectId"
              :is-loading="true"
              data-cy="overlaySpinner"
              aria-label="Updating sort order" />
          </div>
          <MyProject :id="`proj${project.projectId}`" tabindex="-1"
                     :project="project" :disable-sort-control="projects.length === 1"
                     :ref="(el) => (projRef[project.projectId] = el)"
                     @sort-changed-requested="updateSortAndReloadProjects"
                     @copy-project="copyProject"
                     v-on:project-deleted="projectRemoved" v-on:pin-removed="projectUnpinned" />
        </BlockUI>
      </div>
    </div>

    <NoContent2
      v-if="!hasData && !isLoading"
      title="No Projects Yet..."
      class="my-8"
      message="A Project represents a gamified training profile that consists of skills divided into subjects. Create as many Projects as you need."
      data-cy="noProjectsYet" />
    <edit-project
      v-if="newProject.show"
      v-model="newProject.show"
      :project="newProject.project"
      :is-edit="newProject.isEdit"
      @project-saved="saveProject"
      @close="newProject.show = false"
      :enable-return-focus="true" />
    <pin-projects v-if="showSearchProjectModal" v-model="showSearchProjectModal"
                  @done="pinModalClosed" />

    <lengthy-operation-progress-bar-modal v-if="copyProgressModal.show"
                                          v-model="copyProgressModal.show"
                                          :is-complete="copyProgressModal.isComplete"
                                          :has-failed="copyProgressModal.hasFailed"
                                          @operation-done="loadProjectsAfterCopy"
                                          title="Copying Project"
                                          progress-message="Copying Project's Training Profile"
                                          success-message="Project's training profile was successfully copied, please enjoy!">
      <template #failed>
        <Message severity="error" :closable="false">
          <div class="flex flex-col gap-1 text-left" data-cy="copyFailedMsg">
            <div class="text-xl">Failed to copy the project.</div>
            <div v-if="copyProgressModal.isSkill">
              <div>The skill with ID
              <link-to-skill-page
                  :skill-id="copyProgressModal.skillIdThatFailedParagraphValidation"
                  :project-id="copyProgressModal.originalProjectId"
                  :link-label="copyProgressModal.skillIdThatFailedParagraphValidation"
                  data-cy="failedSkillLink"
              />
              has a description that doesn't meet the validation requirements.
              </div>
              <div>
                Please update the skill's description to resolve the issue, then try copying the project again.
              </div>
            </div>
            <div v-if="copyProgressModal.isSubject">
              <div>The subject with ID
                <router-link
                    :to="{
                    name: 'SubjectSkills',
                    params: { projectId: copyProgressModal.originalProjectId, subjectId: copyProgressModal.skillIdThatFailedParagraphValidation }
                  }"
                    class="underline"
                    data-cy="failedSkillLink"
                > {{ copyProgressModal.skillIdThatFailedParagraphValidation }}
                </router-link> has a description that doesn't meet the validation requirements.
              </div>
              <div>
                Please update the subject's description to resolve the issue, then try copying the project again.
              </div>
            </div>
            <div v-if="copyProgressModal.isBadge">
              <div>The badge with ID
                <router-link
                    :to="{
                    name: 'BadgeSkills',
                    params: { projectId: copyProgressModal.originalProjectId, badgeId: copyProgressModal.skillIdThatFailedParagraphValidation }
                  }"
                    class="underline"
                    data-cy="failedSkillLink"
                > {{ copyProgressModal.skillIdThatFailedParagraphValidation }}
                </router-link> has a description that doesn't meet the validation requirements.
              </div>
              <div>
                Please update the badge's description to resolve the issue, then try copying the project again.
              </div>
            </div>
            <div v-if="copyProgressModal.isVideoTranscript">
              <div>
                Video/Audio for Skill ID
                <link-to-skill-page
                    :skill-id="copyProgressModal.skillIdThatFailedParagraphValidation"
                    :project-id="copyProgressModal.originalProjectId"
                    :link-label="copyProgressModal.skillIdThatFailedParagraphValidation"
                    data-cy="failedSkillLink"
                />
                has a transcript that doesn't meet the validation requirements.
              </div>
              <div>
                Please update the skill's video transcript to resolve the issue, then try copying the project again.
              </div>
            </div>
          </div>
        </Message>
      </template>
    </lengthy-operation-progress-bar-modal>
  </div>
</template>

<style scoped>
.loading-indicator {
  position: absolute;
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

@media screen and (min-width: 1600px) {
  .xlPlus\:col-4 {
    flex: 0 0 auto;
    padding: 0.5rem;
    width: 33.3333%;
  }
}

@media screen and (min-width: 2000px) {
  .xlPlusPlus\:col-3 {
    flex: 0 0 auto;
    padding: 0.5rem;
    width: 25%;
  }
}
</style>
