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