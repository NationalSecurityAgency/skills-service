/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useRoute } from 'vue-router'
import { useLog } from '@/components/utils/misc/useLog.js'

export const useSkillsDisplayAttributesState = defineStore('skillsDisplayAttributesState', () => {
  const loadingConfig = ref(true)
  const config = ref({})
  const defaultEmptyValue = ''
  const projectId = ref(defaultEmptyValue)
  const serviceUrl = ref(defaultEmptyValue)
  const isInIframe = ref(false)
  const isSummaryOnly = ref(false)
  const internalBackButton = ref(true)
  const userId = ref(null)
  const version = ref(null)

  const log = useLog()
  const skillsDisplayInfo = useSkillsDisplayInfo()
  const route = useRoute()
  const loadConfigStateIfNeeded = (optionalToPathObj = null) => {
    if (skillsDisplayInfo.isSkillsDisplayPath(optionalToPathObj)) {
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
  const pointDisplayName = computed(() => config.value.pointDisplayName || 'Point')
  const projectName = computed(() => config.value.projectName || route.params.projectId)
  const projectUserCommunityDescriptor = computed(() => {
    return config.value.projectUserCommunityDescriptor || null
  })

  const maxSelfReportMessageLength = computed(() => config.value.maxSelfReportMessageLength)
  const groupDescriptionsOn = computed(() => config.value.groupDescriptionsOn)
  const disableAchievementsCelebrations = computed(() => config.value.disableAchievementsCelebrations)

  const afterPropsAreSet = () => {
    return new Promise((resolve) => {
      const interval = setInterval(() => {
        const serverUrlCheck = serviceUrl.value !== defaultEmptyValue || !skillsDisplayInfo.isSkillsClientPath()
        if (projectId.value !== defaultEmptyValue && serverUrlCheck) {
          clearInterval(interval);
          resolve();
        } else {
          if (log.isTraceEnabled()) {
            log.trace(`waiting to set: projectId: ${projectId.value}, serviceUrl: ${serviceUrl.value}`)
          }
        }
      }, 100); // Check every 100 milliseconds
    });
  }
  return {
    projectId,
    afterPropsAreSet,
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
    pointDisplayName,
    internalBackButton,
    isSummaryOnly,
    userId,
    version,
    projectName,
    projectUserCommunityDescriptor,
    disableAchievementsCelebrations
  }
})