package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserRepo

class SkillOccurrencesSpecs extends DefaultIntSpec {

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserAchievedLevelRepo userAchievementRepo

    def "reduce skill occurrences after user completed the skill"() {
        String userId = "user1"
        String userId2 = "user2"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
            it.pointIncrementInterval = 0 // ability to achieve right away
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Closure fullAchieveSkill = { String userIdToAdd, String skillId ->
            skillsService.addSkill([projectId: proj1.projectId, skillId: skillId], userIdToAdd, new Date()).body
            skillsService.addSkill([projectId: proj1.projectId, skillId: skillId], userIdToAdd, new Date()).body
            skillsService.addSkill([projectId: proj1.projectId, skillId: skillId], userIdToAdd, new Date()).body
            skillsService.addSkill([projectId: proj1.projectId, skillId: skillId], userIdToAdd, new Date() - 1).body
            return skillsService.addSkill([projectId: proj1.projectId, skillId: skillId], userIdToAdd, new Date() - 2).body
        }
        // user 1
        def lastAddSkill1ResUser1 = fullAchieveSkill.call(userId, proj1_skills.get(0).skillId)
        def lastAddSkill3ResUser1 = fullAchieveSkill.call(userId, proj1_skills.get(2).skillId)

        // user 2
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId2, new Date()).body

        def beforeChangeUser1Summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def beforeChangeUser1SummarySubj1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId)

        def beforeChangeUser2Summary = skillsService.getSkillSummary(userId2, proj1.projectId)
        def beforeChangeUser2SummarySubj1 = skillsService.getSkillSummary(userId2, proj1.projectId, proj1_subj.subjectId)

        Closure<List<UserPerformedSkill>> getEvents = { String projId, String userIdToGet, String skillId ->
            return userPerformedSkillRepo.findAll().findAll({ UserPerformedSkill event -> event.projectId == projId && event.userId == userIdToGet && event.skillId == skillId})
        }
        List<UserPerformedSkill> eventsBeforeUser1Skill1 = getEvents.call(proj1.projectId, userId, proj1_skills.get(0).skillId)
        List<UserPerformedSkill> eventsBeforeUser2Skill1 = getEvents.call(proj1.projectId, userId2, proj1_skills.get(0).skillId)
        List<UserAchievement> beforeAchievements = userAchievementRepo.findAllByUserAndProjectIds(userId, [proj1.projectId])

        when:
        proj1_skills.get(0).numPerformToCompletion = 3
        skillsService.createSkill(proj1_skills.get(0))

        def afterChangeUser1Summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def afterChangeUser1SummarySubj1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId)

        def afterChangeUser2Summary = skillsService.getSkillSummary(userId2, proj1.projectId)
        def afterChangeUser2SummarySubj1 = skillsService.getSkillSummary(userId2, proj1.projectId, proj1_subj.subjectId)

        List<UserPerformedSkill> eventsAfterUser1Skill1 = getEvents.call(proj1.projectId, userId, proj1_skills.get(0).skillId)
        List<UserPerformedSkill> eventsAfterUser2Skill1 = getEvents.call(proj1.projectId, userId2, proj1_skills.get(0).skillId)
        List<UserAchievement> afterAchievements = userAchievementRepo.findAllByUserAndProjectIds(userId, [proj1.projectId])

        then:
        lastAddSkill1ResUser1.completed.size() == 1
        lastAddSkill1ResUser1.completed.get(0).id == proj1_skills.get(0).skillId
        lastAddSkill3ResUser1.completed.find {it.type == "Skill"}.id == proj1_skills.get(2).skillId

        // validate that events were properly removed
        eventsBeforeUser1Skill1.size() == 5
        eventsAfterUser1Skill1.size() == 3
        eventsAfterUser1Skill1.sort({ it.created }).subList(0, 3).collect({ it.id }) == eventsAfterUser1Skill1.sort({ it.created }).collect({ it.id })

        // validate that events were kept for user2
        eventsBeforeUser2Skill1.size() == 2
        eventsAfterUser2Skill1.size() == 2
        eventsBeforeUser2Skill1.sort({ it.created }).collect({ it.id }) == eventsAfterUser2Skill1.sort({ it.created }).collect({ it.id })

        // validate the achievement is persistent
        beforeAchievements.find { it.skillId == proj1_skills.get(0).skillId }
        afterAchievements.find { it.skillId == proj1_skills.get(0).skillId }

        // user 1
        beforeChangeUser1Summary.points == 100
        beforeChangeUser1Summary.todaysPoints == 60
        beforeChangeUser1Summary.subjects.get(0).points == 100
        beforeChangeUser1Summary.subjects.get(0).todaysPoints == 60

        beforeChangeUser1SummarySubj1
        beforeChangeUser1SummarySubj1.points == 100
        List skillsBeforeChange = beforeChangeUser1SummarySubj1.skills.sort { it.skillId }
        skillsBeforeChange.get(0).points == 50
        skillsBeforeChange.get(0).todaysPoints == 30
        skillsBeforeChange.get(1).points == 0
        skillsBeforeChange.get(1).todaysPoints == 0
        skillsBeforeChange.get(2).points == 50
        skillsBeforeChange.get(2).todaysPoints == 30

        afterChangeUser1Summary.points == 80
        afterChangeUser1Summary.todaysPoints == 40
        afterChangeUser1Summary.subjects.get(0).points == 80
        afterChangeUser1Summary.subjects.get(0).todaysPoints == 40

        afterChangeUser1SummarySubj1.points == 30
        List skillsAfterChange = afterChangeUser1SummarySubj1.skills.sort { it.skillId }
        skillsAfterChange.get(0).points == 30
        skillsAfterChange.get(0).todaysPoints == 10
        skillsAfterChange.get(1).points == 0
        skillsAfterChange.get(1).todaysPoints == 0
        skillsAfterChange.get(2).points == 50
        skillsAfterChange.get(2).todaysPoints == 30

        // user 2
        beforeChangeUser2Summary.points == 20
        beforeChangeUser2Summary.todaysPoints == 10
        beforeChangeUser2Summary.subjects.get(0).points == 20
        beforeChangeUser2Summary.subjects.get(0).todaysPoints == 10

        beforeChangeUser2SummarySubj1.points == 20
        beforeChangeUser2SummarySubj1.todaysPoints == 10
        List skillsBeforeUser2Change = beforeChangeUser2SummarySubj1.skills.sort { it.skillId }
        skillsBeforeUser2Change.get(0).points == 20
        skillsBeforeUser2Change.get(0).todaysPoints == 10
        skillsBeforeUser2Change.get(1).points == 0
        skillsBeforeUser2Change.get(1).todaysPoints == 0
        skillsBeforeUser2Change.get(2).points == 0
        skillsBeforeUser2Change.get(1).todaysPoints == 0

        beforeChangeUser2Summary.points == 20
        beforeChangeUser2Summary.todaysPoints == 10
        beforeChangeUser2Summary.subjects.get(0).points == 20
        beforeChangeUser2Summary.subjects.get(0).todaysPoints == 10

        afterChangeUser2Summary.points == 20
        afterChangeUser2Summary.todaysPoints == 10
        List skillsAfterUser2Change = afterChangeUser2SummarySubj1.skills.sort { it.skillId }
        skillsAfterUser2Change.get(0).points == 20
        skillsAfterUser2Change.get(0).todaysPoints == 10
        skillsAfterUser2Change.get(1).points == 0
        skillsAfterUser2Change.get(1).todaysPoints == 0
        skillsAfterUser2Change.get(2).points == 0
        skillsAfterUser2Change.get(1).todaysPoints == 0
    }

}
