import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useRoute } from 'vue-router'
import { useLog } from '@/components/utils/misc/useLog.js'

export const useSkillsDisplayAttributesState = defineStore('skillsDisplayAttributesState', () => {
  const loadingConfig = ref(true)
  const config = ref({})
  const projectId = ref('')
  const serviceUrl = ref('')
  const isInIframe = ref(false)
  const isSummaryOnly = ref(false)
  const internalBackButton = ref(true)
  const userId = ref(null)

  const log = useLog()
  const skillsDisplayInfo = useSkillsDisplayInfo()
  const route = useRoute()
  const loadConfigStateIfNeeded = (optionalToPathObj = null) => {
    if (skillsDisplayInfo.isSkillsDisplayPath(optionalToPathObj?.path)) {
      if (route.params.projectId) {
        projectId.value = route.params.projectId
      }
      if (optionalToPathObj && optionalToPathObj.params.projectId) {
        projectId.value = optionalToPathObj.params.projectId
      }

      const sameProjId = projectId.value && config.value && config.value.configForProjectId === projectId.value
      if (!sameProjId) {
        log.debug(`loadConfigStateIfNeeded for [${projectId.value}]`)
        return loadConfigState()
      }
    }

    return Promise.resolve()
  }
  const loadConfigState = () => {
    loadingConfig.value = true
    return axios.get(`${serviceUrl.value}/public/clientDisplay/config?projectId=${projectId.value}`, {
      withCredentials: false
    }).then((result) => {
      config.value = { ...result.data, configForProjectId: projectId.value }
    }).finally(() => {
      loadingConfig.value = false
    })
  }

  const displayProjectDescription = computed(() => config.value.displayProjectDescription)
  const levelDisplayName = computed(() => config.value.levelDisplayName || 'Level')
  const projectDisplayName = computed(() => config.value.projectDisplayName || 'Project')
  const subjectDisplayName = computed(() => config.value.subjectDisplayName || 'Subject')
  const groupDisplayName = computed(() => config.value.groupDisplayName || 'Group')
  const skillDisplayName = computed(() => config.value.skillDisplayName || 'Skill')
  const projectName = computed(() => config.value.projectName || route.params.projectId)


  const maxSelfReportMessageLength = computed(() => config.value.maxSelfReportMessageLength)
  const groupDescriptionsOn = computed(() => config.value.groupDescriptionsOn)
  return {
    projectId,
    serviceUrl,
    loadConfigStateIfNeeded,
    loadingConfig,
    displayProjectDescription,
    levelDisplayName,
    isInIframe,
    maxSelfReportMessageLength,
    groupDescriptionsOn,
    projectDisplayName,
    subjectDisplayName,
    groupDisplayName,
    skillDisplayName,
    internalBackButton,
    isSummaryOnly,
    userId,
    projectName
  }
})