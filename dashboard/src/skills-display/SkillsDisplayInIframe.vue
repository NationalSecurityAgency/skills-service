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
import { ref } from 'vue'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import { useRouter } from 'vue-router'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import { usePageVisitService } from '@/components/utils/services/UsePageVisitService.js'

const appStyleObject = ref({})

const skillDisplayParentFrameState = useSkillsDisplayParentFrameState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const router = useRouter()
const log = useLog()
const pageVisitService = usePageVisitService()

const reportRouteChangeToParentFrame = (route) => {
  if (skillDisplayParentFrameState.parentFrame) {
    const params = {
      path: skillsDisplayInfo.cleanPath(route.path)  || '/',
      fullPath: skillsDisplayInfo.cleanPath(route.fullPath) || '/',
      name: route.name,
      query: route.query,
      currentLocation: window.location.toString(),
      historySize: window.history.length
    }

    if (log.isDebugEnabled()) {
      log.debug(`SkillsDisplayInIframe.vue: emit route-change event to parent frame with ${JSON.stringify(params)}`)
    }
    skillDisplayParentFrameState.parentFrame.emit('route-changed', params)
  } else {
    log.debug(`SkillsDisplayInIframe.vue: called reportRouteChangeToParentFrame but parentFrame was undefined`)
  }
}

router.afterEach((to, from) => {
  if (log.isDebugEnabled()) {
    log.debug(`SkillsDisplayInIframe.vue: Route changed from ${JSON.stringify(from.fullPath)} to ${JSON.stringify(to.fullPath)}`)
  }
  reportRouteChangeToParentFrame(to)
  pageVisitService.reportPageVisit(to.path, to.fullPath)
})
// pageVisitService.reportPageVisit(route)

</script>

<template>
  <div
    id="skills-display-app"
    class="skills-client-app"
    ref="content"
    tabindex="-1"
    role="main"
    :style="appStyleObject"
    aria-label="SkillTree Skills Display">
      <skills-display-home class="skills-display-home"/>
  </div>
</template>

<style scoped>
.skills-display-home {
  margin: 0 auto !important;
}
</style>