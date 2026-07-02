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
import {useRoute} from 'vue-router'
import {useSkillsDisplayAttributesState} from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import ProjectService from '@/components/projects/ProjectService.js'
import {useSkillsDisplayThemeState} from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import {useWindowSize} from '@vueuse/core'
import ResponsiveBreakpoints from '@/components/utils/misc/ResponsiveBreakpoints.js'
import {useAppInfoState} from '@/stores/UseAppInfoState.js'
import ContactProjectAdminsDialog from "@/components/contact/ContactProjectAdminsDialog.vue";
import SkillsDisplaySearchButton from "@/skills-display/components/utilities/SkillsDisplaySearchButton.vue";

const route = useRoute()
const projectId = route.params.projectId
const skillsDisplayAttributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const appInfo = useAppInfoState()


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

const showContact = ref(route.query.openContact === 'true')

themeState.theme.disableSkillTreeBrand = true
themeState.theme.disableBreadcrumb = true
themeState.theme.disableSearchButton = true
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
    ProjectService.addToMyProjects(projectId, true)
  }
}

</script>

<template>
  <div style="position: relative">
    <div :class="{
      'contact-button-inline': isContactButtonInline,
      'text-center': !isContactButtonInline
    }">

      <skills-display-search-button />

      <SkillsButton
          v-if="appInfo.emailEnabled"
          class="ml-2"
          id="contactProjectAdminsBtn"
          :track-for-focus="true"
          @click="showContact = true"
          data-cy="contactOwnerBtn">
        <div class="flex gap-1 items-center">
          <i class="fa-solid fa-mail-bulk"></i>
          <div>Contact</div>
        </div>
      </SkillsButton>
    </div>
    <skills-display-home :id="projectId" class="my-4" />

    <contact-project-admins-dialog v-if="showContact"
                           v-model="showContact"
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