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
import { ref, computed, watch } from 'vue'
import axios from 'axios'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useRoute } from 'vue-router'
import { useLog } from '@/components/utils/misc/useLog.js'
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";
import SkillType from "@/common-components/utilities/SkillType.js";
import { useAuthState } from '@/stores/UseAuthState.js'

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
  const pluralize = usePluralize()
  const route = useRoute()
  const auth = useAuthState()
  let configRevision = 0
  let pendingConfig = null
  // Shared native/preview pages can change project or displayed user. Embedded
  // displays have a fixed learner; their recovery explicitly invalidates this cache.
  const configContext = computed(() => JSON.stringify([serviceUrl.value, projectId.value, userId.value, auth.userInfo?.userId]))
  const invalidateConfig = () => {
    configRevision += 1
    config.value = {}
    pendingConfig = null
    // Invalidating cached data does not schedule a request, especially after logout.
    // Only loadConfigState should mark configuration as loading.
    loadingConfig.value = false
  }
  watch(configContext, () => {
    invalidateConfig()
  }, { flush: 'sync' })
  // Startup, navigation, settings updates and iframe recovery own configuration
  // loading. Cache invalidation must not start a competing identity-change request.
  const loadConfigStateIfNeeded = (optionalToPathObj = null) => {
    if (skillsDisplayInfo.isSkillsDisplayPath(optionalToPathObj)) {
      if (route.params.projectId) {
        projectId.value = route.params.projectId
      }
      if (optionalToPathObj && optionalToPathObj.params.projectId) {
        projectId.value = optionalToPathObj.params.projectId
      }

      const sameContext = projectId.value && config.value.configForContext === configContext.value
      if (!sameContext) {
        log.debug(`loadConfigStateIfNeeded for [${projectId.value}]`)
        return loadConfigState()
      }
    }

    return Promise.resolve()
  }
  const loadConfigState = () => {
    if (pendingConfig) {
      return pendingConfig
    }
    const revision = configRevision
    const context = configContext.value
    loadingConfig.value = true
    pendingConfig = axios.get(`${serviceUrl.value}/public/clientDisplay/config?projectId=${encodeURIComponent(projectId.value)}`, {
      withCredentials: false
    }).then((result) => {
      if (revision === configRevision) {
        config.value = { ...result.data, configForContext: context }
      } else if (auth.userInfo && skillsDisplayInfo.isSkillsDisplayPath()) {
        return loadConfigState()
      }
    }).finally(() => {
      if (revision === configRevision) {
        loadingConfig.value = false
        pendingConfig = null
      }
    })
    return pendingConfig
  }

  const displayProjectDescription = computed(() => config.value.displayProjectDescription)
  const levelDisplayName = computed(() => config.value.levelDisplayName || 'Level')
  const levelDisplayNamePlural = computed(() => pluralize.plural(levelDisplayName.value))
  const projectDisplayName = computed(() => config.value.projectDisplayName || 'Project')
  const projectDisplayNamePlural = computed(() => pluralize.plural(projectDisplayName.value))
  const subjectDisplayName = computed(() => config.value.subjectDisplayName || 'Subject')
  const subjectDisplayNamePlural = computed(() => pluralize.plural(subjectDisplayName.value))
  const groupDisplayName = computed(() => config.value.groupDisplayName || 'Group')
  const groupDisplayNameLower = computed(() => groupDisplayName.value.toLowerCase())
  const groupDisplayNamePlural = computed(() => pluralize.plural(groupDisplayName.value))
  const skillDisplayName = computed(() => config.value.skillDisplayName || 'Skill')
  const skillDisplayNameLower = computed(() => skillDisplayName.value.toLowerCase())
  const skillDisplayNamePlural = computed(() => pluralize.plural(skillDisplayName.value))
  const pointDisplayName = computed(() => config.value.pointDisplayName || 'Point')
  const pointDisplayNameLower = computed(() => pointDisplayName.value.toLowerCase())
  const pointDisplayNamePlural = computed(() => pluralize.plural(pointDisplayName.value))
  const displayNameBySkillType = (type) => {
    if (SkillType.isBadge(type)) {
      return SkillType.Badge
    }
    if (SkillType.isTag(type)) {
      return SkillType.Tag
    }
    if (SkillType.isSkillsGroup(type)) {
      return groupDisplayName.value
    }
    if (SkillType.isSubject(type)) {
      return subjectDisplayName.value
    }
    return skillDisplayName.value
  }
  const projectName = computed(() => config.value.projectName || route.params.projectId)
  const projectUserCommunityDescriptor = computed(() => {
    return config.value.projectUserCommunityDescriptor || null
  })

  const maxSelfReportMessageLength = computed(() => config.value.maxSelfReportMessageLength)
  const groupDescriptionsOn = computed(() => config.value.groupDescriptionsOn)
  const groupInfoOnSkillPage = computed(() => config.value.groupInfoOnSkillPage)
  const disableAchievementsCelebrations = computed(() => config.value.disableAchievementsCelebrations)
  const compactViewDefault = computed(() => config.value.compactViewDefault === true)
  const compactViewPreference = computed(() => typeof config.value.compactViewPreference === 'boolean' ? config.value.compactViewPreference : null)

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
    invalidateConfig,
    loadingConfig,
    displayProjectDescription,
    levelDisplayName,
    levelDisplayNamePlural,
    isInIframe,
    maxSelfReportMessageLength,
    groupDescriptionsOn,
    groupInfoOnSkillPage,
    projectDisplayName,
    projectDisplayNamePlural,
    subjectDisplayName,
    subjectDisplayNamePlural,
    groupDisplayName,
    groupDisplayNameLower,
    groupDisplayNamePlural,
    skillDisplayName,
    skillDisplayNameLower,
    skillDisplayNamePlural,
    pointDisplayName,
    pointDisplayNameLower,
    pointDisplayNamePlural,
    displayNameBySkillType,
    internalBackButton,
    isSummaryOnly,
    userId,
    version,
    projectName,
    projectUserCommunityDescriptor,
    disableAchievementsCelebrations,
    compactViewDefault,
    compactViewPreference
  }
})
