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
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useTestThemeUtils } from '@/skills-display/components/test/UseTestThemeUtils.js'

const route = useRoute()
const projectId = route.params.projectId
const skillsDisplayAttributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const log = useLog()
const testThemeUtils = useTestThemeUtils()

onMounted(() => {

  log.info('Running skills-display in test mode')
  skillsDisplayAttributes.projectId = projectId
  skillsDisplayAttributes.serviceUrl = ''
  skillsDisplayAttributes.loadingConfig = false

  if (route.query.isSummaryOnly && route.query.isSummaryOnly === 'true') {
    skillsDisplayAttributes.isSummaryOnly = true
  }

  if (route.query.disableBackButton && route.query.disableBackButton === 'true') {
    log.info('Disabled back button')
    skillsDisplayAttributes.internalBackButton = false
  }

  if (route.query.skillsVersion) {
    log.info(`TestSkillsDisplay.vue: version=[${route.query.skillsVersion}]`)
    skillsDisplayAttributes.version = route.query.skillsVersion
  }

  const theme = testThemeUtils.constructThemeForTest()
  if (theme) {
    themeState.initThemeObjInStyleTag(theme)
  }

})
</script>

<template>
  <div class="my-4" :class="{'test-skills-display-theme p-6 border-surface-50 dark:border-surface-800 border-round': testThemeUtils.isThemed.value }" data-cy="testDisplayTheme">
    <skills-display-home />
  </div>
</template>

<style scoped>
.test-skills-display-theme {
  background-color: #626d7d;
}
</style>