import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { ref } from 'vue'

export const useLoadTranscriptData = () => {
  const skillsDisplayService = useSkillsDisplayService()
  const isLoading = ref(false)
  const loadTranscriptData = () => {
    isLoading.value = true
    const transcriptInfo = {
      labelsConf: {
        subject: 'Subject',
        skill: 'Skill',
        level: 'Level',
        badge: 'Badge'
      },
      userName: '<Placeholder - John Doe>',
      userLevel: 1,
      totalLevels: 5,
      userPoints: 200,
      totalPoints: 1500,
      userSkillsCompleted: 3,
      totalSkills: 26,
      subjects: [{
        name: 'Subject A',
        userLevel: 1,
        totalLevels: 5,
        userPoints: 200,
        totalPoints: 1500,
        userSkillsCompleted: 3,
        totalSkills: 5,
        skills: [
          {
            name: 'Skill A',
            userPoints: 200,
            totalPoints: 1500
          },
          {
            name: 'Skill B',
            userPoints: 200,
            totalPoints: 1500
          }
        ]
      }, {
        name: 'Subject B',
        userLevel: 1,
        totalLevels: 5,
        userPoints: 200,
        totalPoints: 1500,
        userSkillsCompleted: 3,
        totalSkills: 5
      }, {
        name: 'Subject C',
        userLevel: 1,
        totalLevels: 5,
        userPoints: 200,
        totalPoints: 1500,
        userSkillsCompleted: 3,
        totalSkills: 5,
        skills: [
          {
            name: 'Skill A',
            userPoints: 200,
            totalPoints: 1500
          },
          {
            name: 'Skill B',
            userPoints: 200,
            totalPoints: 1500
          }
        ]
      }, {
        name: 'Subject D',
        userLevel: 1,
        totalLevels: 5,
        userPoints: 200,
        totalPoints: 1500,
        userSkillsCompleted: 3,
        totalSkills: 5
      }, {
        name: 'Subject E',
        userLevel: 1,
        totalLevels: 5,
        userPoints: 200,
        totalPoints: 1500,
        userSkillsCompleted: 3,
        totalSkills: 5
      }, {
        name: 'Subject F',
        userLevel: 1,
        totalLevels: 5,
        userPoints: 200,
        totalPoints: 1500,
        userSkillsCompleted: 3,
        totalSkills: 5
      }],
      achievedBadges: [
        {
          name: 'Badge 1',
          dateAchieved: '2020-01-01'
        }
      ]
    }


    return skillsDisplayService.loadUserProjectSummary()
      .then((projRes) => {

        const getSubjectPromises = transcriptInfo.subjects = projRes.subjects.map((subjRes) => skillsDisplayService.loadSubjectSummary(subjRes.subjectId, true))
        return Promise.all(getSubjectPromises).then((subjRes) => {
          console.log(subjRes)

          transcriptInfo.projectName = projRes.projectName
          transcriptInfo.userLevel = projRes.skillsLevel
          transcriptInfo.totalLevels = projRes.totalLevels
          transcriptInfo.userPoints = projRes.points
          transcriptInfo.totalPoints = projRes.totalPoints
          transcriptInfo.subjects = subjRes.map((subjRes) => {
            return {
              name: subjRes.subject,
              userLevel: subjRes.skillsLevel,
              totalLevels: subjRes.totalLevels,
              userPoints: subjRes.points,
              totalPoints: subjRes.totalPoints,
              userSkillsCompleted: -1,
              totalSkills: -1,
              skills: subjRes.skills?.map((skillRes) => {
                return {
                  name: skillRes.skill,
                  userPoints: skillRes.points,
                  totalPoints: skillRes.totalPoints
                }
              })
            }
          })
          return transcriptInfo
        })



      })
      .finally(() => {
        isLoading.value = false
      })
  }

  return {
    isLoading,
    loadTranscriptData
  }
}