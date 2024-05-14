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

  const theme = testThemeUtils.constructThemeForTest()
  if (theme) {
    themeState.initThemeObjInStyleTag(theme)
  }

})
</script>

<template>
  <div class="my-3" :class="{'test-skills-display-theme p-4 border-50 border-round': testThemeUtils.isThemed.value }" data-cy="testDisplayTheme">
    <skills-display-home />
  </div>
</template>

<style scoped>
.test-skills-display-theme {
  background-color: #626d7d;
}
</style>