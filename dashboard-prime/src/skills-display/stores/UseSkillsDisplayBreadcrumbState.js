import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useLog } from '@/components/utils/misc/useLog.js'

export const useSkillsDisplayBreadcrumbState = defineStore('skillsDisplayBreadcrumbState', () => {

  const router = useRouter()
  const skillsDisplayInfo = useSkillsDisplayInfo()
  const log = useLog()

  const breadcrumbItems = ref([])

  const navUpBreadcrumb = () => {
    const url = breadcrumbItems.value.length > 1 ? breadcrumbItems.value[breadcrumbItems.value.length - 2].contextUrl : skillsDisplayInfo.getRootUrl()
    log.trace(`navUpBreadcrumb: ${url}`)
    router.push({ path: url })
  }
  return {
    breadcrumbItems,
    navUpBreadcrumb
  }
})