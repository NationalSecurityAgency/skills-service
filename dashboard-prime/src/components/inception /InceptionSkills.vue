<script setup>
import { onMounted, watch } from 'vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useRoute } from 'vue-router'

const skillsDisplayAttributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()

skillsDisplayAttributes.projectId = 'Inception'
skillsDisplayAttributes.serviceUrl = ''
skillsDisplayAttributes.loadingConfig = false
skillsDisplayAttributes.internalBackButton = false

themeState.theme.landingPageTitle = 'Dashboard Skills'
themeState.theme.disableSkillTreeBrand = true
themeState.theme.disableBreadcrumb = true
skillsDisplayAttributes.loadConfigStateIfNeeded()


</script>

<template>
  <div>
    <skills-display-home id="inception" class="my-3" />
  </div>
</template>

<style scoped>

</style>