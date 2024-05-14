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