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
    return SkillsService.updateSkill(row, actionToSubmit)
      .then(() => {
        const index = skillsState.subjectSkills.findIndex((item) => item.skillId === row.skillId)
        const newIndex = index + displayIndexIncrement

        const movedSkill = skillsState.subjectSkills[index]
        const otherSkill = skillsState.subjectSkills[newIndex]

        // switch display orders
        const movedSkillDisplayOrder = movedSkill.displayOrder
        movedSkill.displayOrder = otherSkill.displayOrder
        otherSkill.displayOrder = movedSkillDisplayOrder
        // this.skills = this.skills.map((s) => s);
        disableFirstAndLastButtons()
      })
  }
  const disableFirstAndLastButtons = () => {
    if (skillsState.subjectSkills && skillsState.subjectSkills.length > 0) {
      const tableData = skillsState.subjectSkills.sort((a, b) => a.displayOrder - b.displayOrder)
      for (let i = 0; i < tableData.length; i += 1) {
        tableData[i].disabledUpButton = false
        tableData[i].disabledDownButton = false
      }

      tableData[0].disabledUpButton = true
      tableData[tableData.length - 1].disabledDownButton = true
    }
  }


  return {
    moveDisplayOrderUp,
    moveDisplayOrderDown,
    disableFirstAndLastButtons
  }
}