<script setup>
import { SkillsDisplayJS } from '@skilltree/skills-client-js'
import { nextTick, onMounted } from 'vue'
import { useBrowserLocation } from '@vueuse/core'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useRoute } from 'vue-router'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const route = useRoute()
const appConfig = useAppConfig()
const browserLocation = useBrowserLocation()
const log = useLog()

// const clientDisplay = ref(null)
const skillsVersion = 2147483647 // max int

const projectId = route.params.projectId
const serviceUrl = browserLocation.value.origin
const authenticator = appConfig.isPkiAuthenticated ? 'pki' : `${serviceUrl}/api/projects/${encodeURIComponent(projectId)}/token`
const options = {
  projectId,
  authenticator: authenticator,
  serviceUrl: serviceUrl,
  autoScrollStrategy: 'top-of-page'
}

log.info(`Running skills-client in test mode with params ${JSON.stringify(options)}`)

const constructSkillsDisplay = () => {
  let props = {
    version: skillsVersion,
    options: options
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
</script>

<template>
  <div class="mt-3">
    <div id="skills-client-container">
    </div>
  </div>

</template>

<style scoped>

</style>