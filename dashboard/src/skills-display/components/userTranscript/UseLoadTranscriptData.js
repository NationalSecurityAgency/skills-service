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
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { ref } from 'vue'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useProjectCommunityReplacement } from '@/components/customization/UseProjectCommunityReplacement.js'
import { useRoute } from 'vue-router'
import UsersService from '@/components/users/UsersService.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

export const useLoadTranscriptData = () => {
  const skillsDisplayService = useSkillsDisplayService()
  const userAuthState = useAuthState()
  const skillsDisplayAttributesState = useSkillsDisplayAttributesState()
  const projectCommunityReplacement = useProjectCommunityReplacement()

  const isLoading = ref(false)
  const appConfig = useAppConfig()
  const route = useRoute()
  const skillsDisplayInfo = useSkillsDisplayInfo()

  const loadTranscriptData = () => {
    isLoading.value = true

    const headerAndFooter = appConfig.exportHeaderAndFooter ?
      projectCommunityReplacement.populateProjectCommunity(appConfig.exportHeaderAndFooter, skillsDisplayAttributesState.projectUserCommunityDescriptor)
      : null

    const loadUserInfo = () => {
      const currPath = route.path?.toLowerCase() || ''
      const isInceptionProject = currPath.startsWith('/administrator/skills/inception')
      const isAdminPath = currPath.startsWith('/administrator') && !isInceptionProject

      if (isAdminPath) {
        return UsersService.getUserInfo(skillsDisplayAttributesState.projectId, skillsDisplayAttributesState.userId).then((userInfo) => {
          return userInfo
        })
      }

      return Promise.resolve(userAuthState.userInfo)
    }

    const buildUserName = (userInfo) => {
      if(!userInfo && skillsDisplayInfo.isSkillsClientPath()) {
        return skillsDisplayAttributesState.userId || ''
      }

      if(userInfo) {
        const hasUserIdForDisplay = userInfo.userIdForDisplay && userInfo.userIdForDisplay.length > 0
        const userIdToPrint = hasUserIdForDisplay ? userInfo.userIdForDisplay : userInfo.userId

        if (userInfo.nickname && userInfo.nickname.length > 0) {
          return `${userInfo.nickname} (${userIdToPrint})`
        }

        const hasLastName = userInfo.last && userInfo.last.length > 0
        const hasFirstName = userInfo.first && userInfo.first.length > 0
        if (hasLastName && hasFirstName) {
          return `${userInfo.first} ${userInfo.last} (${userIdToPrint})`
        }

        return userIdToPrint
      }
      return ''
    }

    const buildProjectName = (projRes) => {
      return projRes.projectName?.toLowerCase() === 'inception' ? 'Dashboard Skills' : projRes.projectName
    }

    return loadUserInfo().then((userInfo) => {
      return skillsDisplayService.loadUserProjectSummary()
        .then((projRes) => {
          const getSubjectPromises = projRes.subjects.map((subjRes) => skillsDisplayService.loadSubjectSummary(subjRes.subjectId, true))
          getSubjectPromises.push(skillsDisplayService.getBadgeSummaries())
          return Promise.all(getSubjectPromises).then((endpointsRes) => {
            const transcriptInfo = {}
            transcriptInfo.labelsConf = buildLabelConf()
            transcriptInfo.headerAndFooter = headerAndFooter
            transcriptInfo.userName = buildUserName(userInfo)
            transcriptInfo.projectName = buildProjectName(projRes)
            transcriptInfo.userLevel = projRes.skillsLevel
            transcriptInfo.totalLevels = projRes.totalLevels
            transcriptInfo.userPoints = projRes.points
            transcriptInfo.totalPoints = projRes.totalPoints

            const subjRes = endpointsRes.filter((r) => r && r.subjectId && r.subjectId.length > 0)

            transcriptInfo.subjects = subjRes.map((subjRes) => {
              return {
                name: subjRes.subject,
                userLevel: subjRes.skillsLevel,
                totalLevels: subjRes.totalLevels,
                userPoints: subjRes.points,
                totalPoints: subjRes.totalPoints,
                userSkillsCompleted: subjRes.skills ? subjRes.skills.filter((s) => s.points === s.totalPoints).length : 0,
                totalSkills: subjRes.skills ? subjRes.skills.length : 0,
                skills: subjRes.skills?.map((skillRes) => {
                  return {
                    name: skillRes.skill,
                    userPoints: skillRes.points,
                    totalPoints: skillRes.totalPoints
                  }
                })
              }
            })

            transcriptInfo.userSkillsCompleted = transcriptInfo.subjects.map((subj) => subj.userSkillsCompleted).reduce((a, b) => a + b, 0)
            transcriptInfo.totalSkills = transcriptInfo.subjects.map((subj) => subj.totalSkills).reduce((a, b) => a + b, 0)

            const badgeRes = endpointsRes.find((r) => r && (r instanceof Array) && r.length > 0 && r[0].badgeId && r[0].badgeId.length > 0)
            transcriptInfo.achievedBadges = badgeRes?.filter((b) => b.badgeAchieved)?.map((badgeRes) => {
              return {
                name: badgeRes.badge,
                dateAchieved: badgeRes.dateAchieved
              }
            })

            return transcriptInfo
          })
        })
    }).finally(() => {
      isLoading.value = false
    })
  }

  const buildLabelConf = () => {
    return {
      subject: skillsDisplayAttributesState.subjectDisplayName,
      skill: skillsDisplayAttributesState.skillDisplayName,
      level: skillsDisplayAttributesState.levelDisplayName,
      badge: 'Badge',
      point: skillsDisplayAttributesState.pointDisplayName,
    }
  }

  return {
    isLoading,
    loadTranscriptData
  }
}