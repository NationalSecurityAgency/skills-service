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
import { computed, ref } from 'vue'
import SkillsDisplayBreadcrumb from '@/skills-display/components/header/SkillsDisplayBreadcrumb.vue'
import PoweredBySkilltree from '@/skills-display/components/header/PoweredBySkilltree.vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useSkillsDisplayBreadcrumbState } from '@/skills-display/stores/UseSkillsDisplayBreadcrumbState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import SkillsDisplaySearch from '@/skills-display/components/SkillsDisplaySearch.vue'
import { useUserPreferences } from '@/stores/UseUserPreferences.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useMagicKeys, watchDebounced } from '@vueuse/core'
import { useRoute } from 'vue-router'

const attributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const breadcrumb = useSkillsDisplayBreadcrumbState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const log = useLog()
const userPreferences = useUserPreferences()
const route = useRoute()
const keys = useMagicKeys({
  passive: false,
  onEventFired(e) {
    if (searchTrainingButtonShortcut.value?.toLowerCase() === 'ctrl+k' && e.ctrlKey && e.key === 'k' && e.type === 'keydown') {
      e.preventDefault()
    }
  },
})

const projectId = route.params.projectId
const showSkillsDisplaySearchDialog = ref(false)
const searchTrainingButtonShortcut = ref('ctrl+k')
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

const props = defineProps({
  backButton: { type: Boolean, default: true },
  animatePowerByLabel: { type: Boolean, default: true }
})

const showBackButton = computed(() => {
  return props.backButton && attributes.internalBackButton
})
const navigateBack = () => {
  breadcrumb.navUpBreadcrumb()
}
const toTitleCase = (str) => {
  return str.toLowerCase().split('+').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
}
const isTrueCaseInsensitive = (value) => {
  return value === true || String(value).toLowerCase() === 'true';
}

const disableSearchButton = computed(() => isTrueCaseInsensitive(themeState.theme.disableSearchButton))
const disableBreadcrumb = computed(() => isTrueCaseInsensitive(themeState.theme.disableBreadcrumb))
const disableSkillTreeBrand = computed(() => isTrueCaseInsensitive(themeState.theme.disableSkillTreeBrand))
const renderDivWhereBackButtonResides = computed(() => (showBackButton.value || !disableSearchButton.value || !disableSkillTreeBrand.value))
const renderDivWhereBrandResides = computed(() => showBackButton.value || !disableSkillTreeBrand.value)
const isThemeAligned = computed(() => themeState.theme?.pageTitle?.textAlign)
</script>

<template>
  <Card class="skills-theme-page-title" data-cy="skillsTitle"
        :pt="{ body: { class: 'p-0!' }, content: { class: 'px-2! pt-2! pb-3!' } }">
    <template #content>
      <div class="flex flex-wrap flex-col md:flex-row content-center gap-2" :class="{'px-2': !renderDivWhereBackButtonResides}">
        <div v-if="renderDivWhereBackButtonResides"
             :class="{'text-center md:text-left md:w-32': !isThemeAligned}">
          <SkillsButton
            v-if="showBackButton"
            @click="navigateBack"
            outlined
            icon="fas fa-arrow-left"
            class="skills-theme-btn"
            data-cy="back"
            aria-label="navigate back" />

          <SkillsButton
              v-if="!disableSearchButton"
              id="skillsDisplaySearchBtn"
              :track-for-focus="true"
              class="skills-search-btn"
              :class="{'ml-2': showBackButton}"
              @click="showSkillsDisplaySearchDialog = true"
              data-cy="skillsDisplaySearchBtn"
              :title="`Search Project (${toTitleCase(searchTrainingButtonShortcut)})`"
              icon="fa-solid fa-magnifying-glass" />
        </div>

        <div :class="{'mx-5': showBackButton}" class="text-center flex-1">
          <SkillsDisplayBreadcrumb v-if="!disableBreadcrumb"></SkillsDisplayBreadcrumb>
          <h1 data-cy="title"
               :class="{ 'mt-2': disableBreadcrumb}"
               class="skills-title uppercase text-2xl font-normal m-0">
            <slot />
          </h1>
        </div>

        <div v-if="renderDivWhereBrandResides" class="md:w-32">
          <div v-if="!disableSkillTreeBrand"
               class="flex items-center justify-center" >
            <powered-by-skilltree :animate-power-by-label="animatePowerByLabel && skillsDisplayInfo.isHomePage.value" />
          </div>
        </div>

        <skills-display-search v-if="showSkillsDisplaySearchDialog && !disableSearchButton"
                               ref="skillsDisplaySearch"
                               v-model="showSkillsDisplaySearchDialog"
                               :project-id="projectId" />
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>