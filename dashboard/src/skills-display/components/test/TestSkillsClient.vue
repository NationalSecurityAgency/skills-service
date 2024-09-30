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
import { SkillsDisplayJS } from '@skilltree/skills-client-js'
import { nextTick, onMounted, computed } from 'vue'
import { useBrowserLocation } from '@vueuse/core'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useRoute } from 'vue-router'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useTestThemeUtils } from '@/skills-display/components/test/UseTestThemeUtils.js'

const route = useRoute()
const appConfig = useAppConfig()
const browserLocation = useBrowserLocation()
const log = useLog()
const testThemeUtils = useTestThemeUtils()

// const clientDisplay = ref(null)
const skillsVersion = 2147483647 // max int

const projectId = route.params.projectId
log.info(`TestSkillsClient.vue: Using project id [${projectId}]`)
const isSummaryOnly = route.query.isSummaryOnly && route.query.isSummaryOnly === 'true'
const serviceUrl = browserLocation.value.origin
const authenticator = appConfig.isPkiAuthenticated ? 'pki' : `${serviceUrl}/api/projects/${encodeURIComponent(projectId)}/token`
const options = {
  projectId,
  authenticator: authenticator,
  serviceUrl: serviceUrl,
  autoScrollStrategy: 'top-of-page'
}

if (isSummaryOnly) {
  options.isSummaryOnly = true
}

log.info(`Running skills-client in test mode with params ${JSON.stringify(options)}`)


const constructSkillsDisplay = () => {
  let props = {
    version: skillsVersion,
    options: options,
  }

  const customTheme = testThemeUtils.constructThemeForTest()
  if (customTheme) {
    props.theme = customTheme
  }
  const clientDisplay = new SkillsDisplayJS(props)

  log.debug(`SkillsDisplay.vue: constructSkillsDisplay: ${JSON.stringify(props)}`)
  nextTick(() => {
    clientDisplay.attachTo(document.querySelector('#skills-client-container'))
  })
}

onMounted(() => {
  constructSkillsDisplay()
})

const isThemeApplied = computed(() => route.query.enableTheme && route.query.enableTheme.toLocaleLowerCase() === 'true')
</script>

<template>
  <div class="mt-3 p-3" :class="{'themed-applied': isThemeApplied}">
    <div id="skills-client-container">
    </div>
  </div>

</template>

<style scoped>
.themed-applied {
  background: #626d7d !important;
}
</style>