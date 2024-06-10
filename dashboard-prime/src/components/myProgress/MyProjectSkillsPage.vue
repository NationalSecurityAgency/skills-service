<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import ProjectService from '@/components/projects/ProjectService.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useWindowSize } from '@vueuse/core'
import ResponsiveBreakpoints from '@/components/utils/misc/ResponsiveBreakpoints.js'
import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog.vue'

const route = useRoute()
const projectId = route.params.projectId
const skillsDisplayAttributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()

const windowSize = useWindowSize()
const currentWidth = ref(windowSize.width)
watch(() => windowSize.width,
  (newWidth) => {
    currentWidth.value = newWidth
  }
)
const mdOrless = computed(() => currentWidth.value < ResponsiveBreakpoints.md)

const landingTitle = `${skillsDisplayAttributes.projectDisplayName}: ${skillsDisplayAttributes.projectName}`
const oneRem = ref(0)
const compute1Rem = () => {
  oneRem.value = parseFloat(getComputedStyle(document.documentElement).fontSize)
}

const isContactButtonInline = computed(() => {
  if (mdOrless.value) {
    return false
  }
  const currentLen = landingTitle.length * 1.2
  const titleWidthPx = currentLen * oneRem.value
  return currentWidth.value > titleWidthPx
})

const showContact = ref(false)

themeState.theme.disableSkillTreeBrand = true
themeState.theme.disableBreadcrumb = true
themeState.theme.pageTitle = {
  textAlign: 'left',
  fontSize: '1.5rem',
  padding: '0.5rem 0rem 0.5rem 0rem'
}

onMounted(() => {
  compute1Rem()
  skillsDisplayAttributes.projectId = projectId
  skillsDisplayAttributes.serviceUrl = ''
  skillsDisplayAttributes.loadingConfig = false
  skillsDisplayAttributes.internalBackButton = false

  themeState.theme.landingPageTitle = landingTitle
  themeState.initThemeObjInStyleTag(themeState.theme)
  handleProjInvitation()
})

const handleProjInvitation = () => {
  const isInvited = route.query.invited
  if (isInvited) {
    ProjectService.addToMyProjects(projectId)
  }
}
</script>

<template>
  <div style="position: relative">
    <div :class="{
      'contact-button-inline': isContactButtonInline,
      'text-center': !isContactButtonInline
    }">
      <SkillsButton
        id="contactProjectAdminsBtn"
        :track-for-focus="true"
        @click="showContact = true"
        data-cy="contactOwnerBtn"
        label="Contact Project"
        icon="fas fa-mail-bulk" />
    </div>
    <skills-display-home :id="projectId" class="my-3" />

    <contact-owners-dialog v-if="showContact"
                           v-model="showContact"
                           :project-name="skillsDisplayAttributes.projectName"
                           :project-id="projectId"
                           save-button-label="Submit"
    />
  </div>
</template>

<style scoped>
.contact-button-inline {
  position: absolute;
  right: 1rem;
  top: 1rem;
}
</style>