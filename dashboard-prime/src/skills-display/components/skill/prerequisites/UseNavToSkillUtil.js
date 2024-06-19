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
import { useRoute } from 'vue-router'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

export const useNavToSkillUtil = () => {

  const route = useRoute()
  const displayInfo = useSkillsDisplayInfo()

  const navigateToSkill = (skillItem) => {

    if (skillItem && skillItem.skillId && !skillItem.isThisSkill) {
      if (skillItem.isCrossProject) {
        if (route.params.badgeId) {
          const params = {
            badgeId: route.params.badgeId,
            crossProjectId: skillItem.projectId,
            dependentSkillId: skillItem.skillId
          }
          displayInfo.routerPush('crossProjectSkillDetailsUnderBadge', params)
        } else {
          displayInfo.routerPush(
            'crossProjectSkillDetails',
            {
              subjectId: route.params.subjectId,
              crossProjectId: skillItem.projectId,
              skillId: route.params.skillId,
              dependentSkillId: skillItem.skillId
            }
          )
        }
      } else if (skillItem.type !== 'Badge') {
        displayInfo.routerPush(
          'skillDetails',
          {
            subjectId: skillItem.subjectId,
            skillId: skillItem.skillId
          }
        )
      } else {
        displayInfo.routerPush(
          'badgeDetails',
          {
            badgeId: skillItem.skillId
          }
        )
      }
    }
  }

  return {
    navigateToSkill
  }
}