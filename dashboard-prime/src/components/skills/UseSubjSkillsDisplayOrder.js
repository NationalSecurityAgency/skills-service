import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsService from '@/components/skills/SkillsService.js'

export const useSubjSkillsDisplayOrder = () => {

  const skillsState = useSubjectSkillsState()
  const announcer = useSkillsAnnouncer()
  const moveDisplayOrderUp = (row) => {
    moveDisplayOrder(row, 'DisplayOrderUp', -1)
      .then(() => {
        announcer.polite(`Moved skill ${row.name} display order up by one`)
      })
  }
  const moveDisplayOrderDown = (row) => {
    moveDisplayOrder(row, 'DisplayOrderDown', 1)
      .then(() => {
        announcer.polite(`Moved skill ${row.name} display order down by one`)
      })
  }

  const moveDisplayOrder = (row, actionToSubmit, displayIndexIncrement) => {
    if (!row.groupId) {
      skillsState.setLoadingSubjectSkills(true)
    } else {
      skillsState.setLoadingGroupSkills(row.groupId, true)
    }
    return SkillsService.updateSkill(row, actionToSubmit)
      .then(() => {
        const skills = row.groupId ? skillsState.getGroupSkills(row.groupId) : skillsState.subjectSkills
        const index = skills.findIndex((item) => item.skillId === row.skillId)
        const newIndex = index + displayIndexIncrement

        const movedSkill = skills[index]
        const otherSkill = skills[newIndex]

        // switch display orders
        const movedSkillDisplayOrder = movedSkill.displayOrder
        movedSkill.displayOrder = otherSkill.displayOrder
        otherSkill.displayOrder = movedSkillDisplayOrder
        // this.skills = this.skills.map((s) => s);
        disableFirstAndLastButtons(row.groupId)

        if (row.groupId) {
          skillsState.setGroupSkills(row.groupId, skills)
        }

        if (!row.groupId) {
          skillsState.setLoadingSubjectSkills(false)
        } else {
          skillsState.setLoadingGroupSkills(row.groupId, false)
        }
      })
  }
  const disableFirstAndLastButtons = (groupId) => {
    const skills = groupId ? skillsState.getGroupSkills(groupId) : skillsState.subjectSkills

    if (skills && skills.length > 0) {
      const tableData = skills.sort((a, b) => a.displayOrder - b.displayOrder)
      for (let i = 0; i < tableData.length; i += 1) {
        tableData[i].disabledUpButton = false
        tableData[i].disabledDownButton = false
      }

      tableData[0].disabledUpButton = true
      tableData[tableData.length - 1].disabledDownButton = true
      if (groupId) {
        skillsState.setGroupSkills(tableData)
      }
    }
  }


  return {
    moveDisplayOrderUp,
    moveDisplayOrderDown,
    disableFirstAndLastButtons
  }
}