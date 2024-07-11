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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import PageHeader from '@/components/utils/pages/PageHeader.vue'
import Navigation from '@/components/utils/Navigation.vue'
import ProjectService from '@/components/projects/ProjectService'
import ProjectDates from '@/components/projects/ProjectDates.vue'
import dayjs from '@/common-components/DayJsCustomizer.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import ImportFinalizeAlert from '@/components/skills/catalog/ImportFinalizeAlert.vue'
import EditProject from '@/components/projects/EditProject.vue'
import { useProjDetailsState } from '@/stores/UseProjDetailsState.js'
import UserRolesUtil from '@/components/utils/UserRolesUtil'
import Avatar from 'primevue/avatar'
import ProjectShareDialog from '@/components/projects/ProjectShareDialog.vue'
import ProjectExpirationWarning from '@/components/projects/ProjectExpirationWarning.vue'

// const props = defineProps(['project'])
const router = useRouter()
const route = useRoute()
const projConfig = useProjConfig()
const appConfig = useAppConfig()
const announcer = useSkillsAnnouncer()
const projectDetailsState = useProjDetailsState()


const cancellingExpiration = ref(false)
const editProject = ref(false)
const shareProjModal = ref(false)
const shareUrl = ref('')
const project = computed(() => projectDetailsState.project)
const isLoadingData = computed(() => projectDetailsState.isLoading)
const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj)

const isLoading = computed(() => {
  return isLoadingData.value // || projConfig.loadingProjConfig;
})

onMounted(() => {
  projectDetailsState.loadProjectDetailsState(true)
})

const navItems = computed(() => {
  const items = [
    { name: 'Subjects', iconClass: 'fa-cubes skills-color-subjects', page: 'Subjects' },
    { name: 'Badges', iconClass: 'fa-award skills-color-badges', page: 'Badges' },
    { name: 'Self Report', iconClass: 'fa-laptop skills-color-selfreport', page: 'SelfReport' },
    { name: 'Learning Path', iconClass: 'fa-project-diagram skills-color-dependencies', page: 'FullDependencyGraph' }
  ]

  if (!isReadOnlyProj.value) {
    items.push({ name: 'Skill Catalog', iconClass: 'fa-book skills-color-skill-catalog', page: 'SkillsCatalog' })
    items.push({ name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'ProjectLevels' })
  }

  items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'ProjectUsers' })
  items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'ProjectMetrics' })

  if (!isReadOnlyProj.value) {
    items.push({ name: 'Contact Users', iconClass: 'fas fa-mail-bulk', page: 'EmailUsers' })
    items.push({ name: 'Issues', iconClass: 'fas fa-exclamation-triangle', page: 'ProjectErrorsPage' })
    items.push({ name: 'Access', iconClass: 'fa-shield-alt skills-color-access', page: 'ProjectAccess' })
    items.push({ name: 'Admin Activity', iconClass: 'fa-users-cog text-success', page: 'ProjectActivityHistory' })
    items.push({
      name: 'Skill Expiration History',
      iconClass: 'fa-clock skills-color-expiration',
      page: 'ExpirationHistory'
    })
    items.push({ name: 'Settings', iconClass: 'fa-cogs skills-color-settings', page: 'ProjectSettings' })
  }

  return items
})

const headerOptions = computed(() => {
  if (!project.value || !projConfig.projConfig) {
    return {}
  }
  let visibilityLabel = 'Project Catalog'
  let visibilityIcon = 'fas fa-eye-slash text-warning'
  let visibilityDescription = ''
  let visibilityType = 'Hidden'
  if (projConfig.isProjConfigInviteOnly) {
    visibilityLabel = 'Protection'
    visibilityDescription = 'Invite Only'
    visibilityIcon = 'fas fa-user-lock text-danger'
    visibilityType = 'PRIVATE'
  } else if (projConfig.isProjConfigDiscoverable) {
    visibilityType = 'Discoverable'
    visibilityIcon = 'fas fa-search-plus text-success'
    visibilityDescription = ''
  }

  const stats = []
  stats.push({
    label: visibilityLabel,
    preformatted: `<div class="h5 font-weight-bold mb-0">${visibilityType}</div>`,
    secondaryPreformatted: `<div class="text-secondary text-uppercase text-truncate" style="font-size:0.8rem;margin-top:0.1em;">${visibilityDescription}</div>`,
    icon: `${visibilityIcon} skills-color-visibility`
  })
  stats.push({
    label: 'Skills',
    count: project.value.numSkills,
    secondaryStats: [{
      label: 'reused',
      count: project.value.numSkillsReused,
      badgeVariant: 'info'
    }, {
      label: 'disabled',
      count: project.value.numSkillsDisabled,
      badgeVariant: 'warning'
    }],
    icon: 'fas fa-graduation-cap skills-color-skills'
  })
  stats.push({
    label: 'Points',
    count: project.value.totalPoints,
    icon: 'far fa-arrow-alt-circle-up skills-color-points',
    secondaryStats: [{
      label: 'reused',
      count: project.value.totalPointsReused,
      badgeVariant: 'info'
    }]
  })
  stats.push({
    label: 'Badges',
    count: project.value.numBadges,
    icon: 'fas fa-award skills-color-badges'
  })

  if (!isReadOnlyProj.value) {
    stats.push({
      label: 'Issues',
      count: project.value.numErrors,
      icon: 'fas fa-exclamation-triangle'
    })
  }

  return {
    icon: 'fas fa-list-alt skills-color-projects',
    title: `PROJECT: ${project.value.name}`,
    subTitle: `ID: ${project.value.projectId}`,
    stats
  }
})

const isInsufficientPoints = computed(() => {
  const projPoints = project.value?.totalPoints || 0
  return projPoints < appConfig.minimumProjectPoints
})

const copyAndDisplayShareProjInfo = () => {
  const host = window.location.origin
  shareUrl.value = `${host}/progress-and-rankings/projects/${project.value.projectId}?invited=true`
  navigator.clipboard.writeText(shareUrl.value).then(() => {
    shareProjModal.value = true
  })
}

const projectSaved = (updatedProject) => {
  const origProjId = project.value.projectId
  ProjectService.saveProject(updatedProject).then(() => {
    ProjectService.getProject(updatedProject.projectId)
      .then((retrievedProject) => {
        setProject(retrievedProject)
      })
    if (updatedProject.projectId !== origProjId) {
      router.replace({ name: route.name, params: { ...route.params, projectId: updatedProject.projectId } })
        .then(() => {
          projConfig.loadProjConfigState({ projectId: updatedProject.projectId, updateLoadingVar: false })
        })
    } else {
      projConfig.loadProjConfigState({ projectId: updatedProject.projectId, updateLoadingVar: false })
    }
    announcer.polite(`Project ${updatedProject.name} has been edited`)
  })
}

const setProject = (newProject) => {
  projectDetailsState.project = newProject
}

const isProjectExpiring = computed(() => {
  return project.value && project.value.expiring && !isReadOnlyProj.value
})

</script>

<template>
  <div ref="mainFocus">
    <PageHeader :loading="isLoading" :options="headerOptions">
      <template #banner v-if="isProjectExpiring">
        <project-expiration-warning :project="project" @extended="project.expiring = false" />
      </template>
      <template #subTitle v-if="project">
        <div v-if="project.userCommunity" class="mb-3" data-cy="userCommunity">
          <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
          <span
            class="text-secondary font-italic ml-1">{{ appConfig.userCommunityBeforeLabel }}</span> <span
          class="font-weight-bold text-primary">{{ project.userCommunity }}</span> <span
          class="text-secondary font-italic">{{ appConfig.userCommunityAfterLabel }}</span>
        </div>
        <div class="">
          <span class="border-1 border-round px-1 mr-2">
           <i class="fas fa-fingerprint" aria-hidden="true" />
          </span>
          <span class="font-italic text-color-secondary">Project ID</span>: {{ project.projectId }}
        </div>
      </template>
      <template #subSubTitle v-if="project">
        <div v-if="!isReadOnlyProj">
          <SkillsButton
            id="editProjectBtn"
            @click="editProject = true"
            ref="editProjectButton"
            size="small"
            outlined
            severity="info"
            :track-for-focus="true"
            data-cy="btn_edit-project"
            label="Edit"
            icon="fas fa-edit"
            :aria-label="`edit Project ${project.name}`">
          </SkillsButton>
          <router-link
            class="ml-1"
            data-cy="projectPreview"
            :to="{ name:'MyProjectSkillsPage', params: { projectId: project.projectId } }"
            tabindex="-1"
            target="_blank" rel="noopener">
            <SkillsButton
              target="_blank"
              v-if="project"
              outlined
              severity="info"
              size="small"
              label="Preview"
              icon="fas fa-eye"
              v-skills="'PreviewProjectClientDisplay'"
              :aria-label="`preview client display for project ${project.name}`">
            </SkillsButton>
          </router-link>
          <SkillsButton
            id="projectShareBtn"
            v-if="projConfig.isProjConfigDiscoverable"
            ref="shareProjectButton"
            size="small"
            severity="info"
            @click="copyAndDisplayShareProjInfo"
            data-cy="shareProjBtn"
            class=" ml-1"
            label="Share" icon="fas fa-share-alt"
            :track-for-focus="true"
            v-skills="'ShareProject'"
            :aria-label="`Share ${project.name} with new users`">
          </SkillsButton>
        </div>
        <div data-cy="projectCreated" class="mt-3">
          <i class="fas fa-clock text-success mr-1" aria-hidden="true" />
          <ProjectDates :created="project.created" :load-last-reported-date="true" />
        </div>
        <div v-if="projConfig.userProjRole">
          <i class="fas fa-user-shield text-success header-status-icon" aria-hidden="true" /> <span
          class="text-secondary font-italic small">Role:</span> <span class="small text-primary"
                                                                      data-cy="userRole">{{ UserRolesUtil.userRoleFormatter(projConfig.userProjRole)
          }}</span>
        </div>
      </template>
    </PageHeader>

    <Message v-if="isInsufficientPoints" :closable="false" data-cy="projectInsufficientPoints">
      Project has insufficient points assigned. Skills cannot be achieved until project has at least <Tag>{{ appConfig.minimumProjectPoints }}</Tag> points.
    </Message>

    <import-finalize-alert />

    <Navigation :nav-items="navItems">
    </Navigation>

    <edit-project
      v-if="editProject"
      v-model="editProject"
      :is-edit="true"
      :id="`editProjectModal${project.projectId}`"
      :project="project"
      :enable-return-focus="true"
      @project-saved="projectSaved" />

    <project-share-dialog
      v-if="shareProjModal"
      v-model="shareProjModal"
      :share-url="shareUrl" />
  </div>

</template>

<style scoped></style>
