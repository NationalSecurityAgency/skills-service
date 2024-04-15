<script setup>
import { nextTick, onMounted, ref, computed, toRaw } from 'vue'
import { SkillsDisplayJS } from '@skilltree/skills-client-js'
import { useRoute } from 'vue-router'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useBrowserLocation } from '@vueuse/core'
import { useLog } from '@/components/utils/misc/useLog.js'

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
  <div>
    <div id="skills-client-container">
    </div>
  </div>
</template>

<style scoped>

</style>