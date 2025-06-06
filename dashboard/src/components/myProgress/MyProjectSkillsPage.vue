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
import { useRoute } from 'vue-router'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import ProjectService from '@/components/projects/ProjectService.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useWindowSize, watchDebounced, useMagicKeys } from '@vueuse/core'
import ResponsiveBreakpoints from '@/components/utils/misc/ResponsiveBreakpoints.js'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import ContactProjectAdminsDialog from "@/components/contact/ContactProjectAdminsDialog.vue";
import SkillsDisplaySearch from '@/skills-display/components/SkillsDisplaySearch.vue'
import { useUserPreferences } from '@/stores/UseUserPreferences.js'
import { useLog } from '@/components/utils/misc/useLog.js'

const route = useRoute()
const projectId = route.params.projectId
const skillsDisplayAttributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const appInfo = useAppInfoState()

const log = useLog()
const userPreferences = useUserPreferences()
const keys = useMagicKeys({
  passive: false,
  onEventFired(e) {
    if (searchTrainingButtonShortcut.value === 'Ctrl+K' && e.ctrlKey && e.key === 'k' && e.type === 'keydown') {
      e.preventDefault()
    }
  },
})

const showSkillsDisplaySearchDialog = ref(false)
const searchTrainingButtonShortcut = ref('Ctrl+K')
userPreferences.afterUserPreferencesLoaded().then((options) => {
  const debounceOptions = { debounce: 250, maxWait: 1000 }
  if (options.sd_search_training_keyboard_shortcut) {
    searchTrainingButtonShortcut.value = options.sd_search_training_keyboard_shortcut?.toLowerCase().replace(/ /g, '')
  }

  log.debug(`Search training shortcut is : ${searchTrainingButtonShortcut.value}`)
  watchDebounced(
      keys[searchTrainingButtonShortcut.value],
      () => {
        showSkillsDisplaySearchDialog.value = true
      },
      debounceOptions
  )
})

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
        id="skillsDisplaySearchBtn"
        :track-for-focus="true"
        @click="showSkillsDisplaySearchDialog = true"
        data-cy="skillsDisplaySearchBtn"
        label="Search Project"
        icon="fa-solid fa-magnifying-glass" />

      <SkillsButton
          v-if="appInfo.emailEnabled"
          class="ml-2"
          id="contactProjectAdminsBtn"
          :track-for-focus="true"
          @click="showContact = true"
          data-cy="contactOwnerBtn"
          label="Contact Project"
          icon="fas fa-mail-bulk" />
    </div>
    <skills-display-home :id="projectId" class="my-4" />

    <contact-project-admins-dialog v-if="showContact"
                           v-model="showContact"
                           :project-id="projectId"
                           save-button-label="Submit"
    />

    <skills-display-search ref="skillsDisplaySearch" v-if="showSkillsDisplaySearchDialog" v-model="showSkillsDisplaySearchDialog" :project-id="projectId" />
  </div>
</template>

<style scoped>
.contact-button-inline {
  position: absolute;
  right: 1rem;
  top: 1rem;
}
</style>