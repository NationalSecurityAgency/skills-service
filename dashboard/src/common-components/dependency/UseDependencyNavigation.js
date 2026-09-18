/*
 * Copyright 2026 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useRoute, useRouter } from 'vue-router'
import SkillType from '@/common-components/utilities/SkillType.js'
import { useSkillOverviewRouteUtil } from '@/components/skills/UseSkillOverviewRouteUtil.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

export const useDependencyNavigation = () => {
  const router = useRouter()
  const route = useRoute()
  const skillRouteUtil = useSkillOverviewRouteUtil()
  const skillsDisplayInfo = useSkillsDisplayInfo()

  const getAdminRoute = (item) => {
    if (SkillType.isSkill(item.type)) {
      return skillRouteUtil.toRouteProps(item.projectId, item.subjectId, item.skillId, item.type, item.groupId)
    }
    return {
      name: 'BadgeSkills',
      params: { projectId: item.projectId, badgeId: item.skillId },
    }
  }

  const getSkillsDisplayRoute = (item) => {
    const currentProjectId = route.params.projectId
    const isCrossProject = item.isCrossProject ?? item.projectId !== currentProjectId
    if (isCrossProject && SkillType.isSkill(item.type)) {
      return {
        name: skillsDisplayInfo.getContextSpecificRouteName('crossProjectSkillDetails'),
        params: {
          projectId: currentProjectId,
          subjectId: item.subjectId,
          skillId: item.skillId,
          crossProjectId: item.projectId,
          dependentSkillId: item.skillId,
        },
      }
    }
    if (SkillType.isSkill(item.type)) {
      return {
        name: skillsDisplayInfo.getContextSpecificRouteName(item.groupId ? 'skillDetailsUnderGroup' : 'skillDetails'),
        params: {
          projectId: currentProjectId,
          subjectId: item.subjectId,
          skillId: item.skillId,
          groupId: item.groupId,
        },
      }
    }
    return {
      name: skillsDisplayInfo.getContextSpecificRouteName('badgeDetails'),
      params: { projectId: currentProjectId, badgeId: item.skillId },
    }
  }

  const getRoute = (item, skillsDisplay = skillsDisplayInfo.isSkillsDisplayPath()) => {
    return skillsDisplay ? getSkillsDisplayRoute(item) : getAdminRoute(item)
  }

  const navigate = (item, skillsDisplay = skillsDisplayInfo.isSkillsDisplayPath()) => router.push(getRoute(item, skillsDisplay))

  return { getRoute, navigate }
}
