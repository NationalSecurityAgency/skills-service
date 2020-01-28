package skills.intTests

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo

import java.text.DateFormat

class SkillOccurrencesSpecs extends DefaultIntSpec {

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserAchievedLevelRepo userAchievementRepo

    DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd")
    Closure getPoints = { def ptsHistory, int daysAgo ->
        use(TimeCategory) {
            return ptsHistory.pointsHistory.find {
                df.parse(it.dayPerformed) == daysAgo.days.ago
            }.points
        }
    }

    Closure createProject = { int projNum, boolean twoSubjs = false ->
        def proj1 = SkillsFactory.createProject(projNum)
        def proj1_subj1 = SkillsFactory.createSubject(projNum, 1)


        List<Map> proj1_skills = SkillsFactory.createSkills(3, projNum, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
            it.pointIncrementInterval = 0 // ability to achieve right away
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)

        def proj1_subj2
        List<Map> proj1_subj2_skills
        if (twoSubjs) {
            proj1_subj2 = SkillsFactory.createSubject(projNum, 2)
            proj1_subj2_skills = SkillsFactory.createSkills(3, projNum, 2)
            proj1_subj2_skills.each {
                it.numPerformToCompletion = 5
                it.pointIncrementInterval = 0 // ability to achieve right away
            }
            skillsService.createSubject(proj1_subj2)
            skillsService.createSkills(proj1_subj2_skills)
        }
        return [proj1, proj1_subj1, proj1_skills, proj1_subj2, proj1_subj2_skills]
    }

    Closure fullyAchieveSkill = { String userIdToAdd, String projectId, String skillId ->
        assert skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date()).body.skillApplied
        assert skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date()).body.skillApplied
        assert skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date()).body.skillApplied
        assert skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date() - 1).body.skillApplied
        def res = skillsService.addSkill([projectId: projectId, skillId: skillId], userIdToAdd, new Date() - 2).body
        assert res.skillApplied

        return res
    }

    private List<String> getSubjectSkillsPtsSlashTotalPts(String userId, String projId, String subjId) {
        return skillsService.getSkillSummary(userId, projId, subjId).skills.sort { it.skillId }.collect { "${it.points}/${it.totalPoints}" }
    }

    private List<Integer> getPointHistory(String userId, String projectId, String subjId = null) {
        return skillsService.getPointHistory(userId, projectId, subjId).pointsHistory.sort { df.parse(it.dayPerformed) }.collect { it.points }
    }


    def "reduce skill occurrences after user completed the skill - multiple users, projects and skills"() {
        String userId = "user1"
        String userId2 = "user2"

        def proj1, proj1_subj, proj1_skills
        def proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        // user 1
        def lastAddProj1Skill1ResUser1 = fullyAchieveSkill.call(userId, proj1.projectId, proj1_skills.get(0).skillId)
        def lastAddProj1Skill3ResUser1 = fullyAchieveSkill.call(userId, proj1.projectId, proj1_skills.get(2).skillId)
        def lastAddProj2Skill1ResUser1 = fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(0).skillId)
        def lastAddProj2Skill3ResUser1 = fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(2).skillId)

        // user 2
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId2, new Date()).body

        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], userId2, new Date()).body

        def beforeChangeUser1Proj1Summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def beforeChangeUser1Proj1SummarySubj1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId)

        def beforeChangeUser2Summary = skillsService.getSkillSummary(userId2, proj1.projectId)
        def beforeChangeUser2SummarySubj1 = skillsService.getSkillSummary(userId2, proj1.projectId, proj1_subj.subjectId)

        Closure<List<UserPerformedSkill>> getEvents = { String projId, String userIdToGet, String skillId ->
            return userPerformedSkillRepo.findAll().findAll({ UserPerformedSkill event -> event.projectId == projId && event.userId == userIdToGet && event.skillId == skillId })
        }
        List<UserPerformedSkill> eventsBeforeUser1Skill1 = getEvents.call(proj1.projectId, userId, proj1_skills.get(0).skillId)
        List<UserPerformedSkill> eventsBeforeUser1Skill3 = getEvents.call(proj1.projectId, userId, proj1_skills.get(2).skillId)
        List<UserPerformedSkill> eventsBeforeUser2Skill1 = getEvents.call(proj1.projectId, userId2, proj1_skills.get(0).skillId)
        List<UserAchievement> beforeAchievements = userAchievementRepo.findAllByUserAndProjectIds(userId, [proj1.projectId])

        def beforePointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def beforePointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)

        when:
        proj1_skills.get(0).numPerformToCompletion = 3
        skillsService.createSkill(proj1_skills.get(0))

        def afterChangeUser1Proj1Summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def afterChangeUser1Proj1SummarySubj1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId)
        def afterChangeUser1Proj2Summary = skillsService.getSkillSummary(userId, proj2.projectId)
        def afterChangeUser1Proj2SummarySubj1 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)

        def afterChangeUser2Proj1Summary = skillsService.getSkillSummary(userId2, proj1.projectId)
        def afterChangeUser2Proj1SummarySubj1 = skillsService.getSkillSummary(userId2, proj1.projectId, proj1_subj.subjectId)
        def afterChangeUser2Proj2Summary = skillsService.getSkillSummary(userId2, proj2.projectId)
        def afterChangeUser2Proj2SummarySubj1 = skillsService.getSkillSummary(userId2, proj2.projectId, proj2_subj.subjectId)

        List<UserPerformedSkill> eventsAfterUser1Skill1 = getEvents.call(proj1.projectId, userId, proj1_skills.get(0).skillId)
        List<UserPerformedSkill> eventsAfterUser1Skill3 = getEvents.call(proj1.projectId, userId, proj1_skills.get(2).skillId)
        List<UserPerformedSkill> eventsAfterUser2Skill1 = getEvents.call(proj1.projectId, userId2, proj1_skills.get(0).skillId)
        List<UserAchievement> afterAchievements = userAchievementRepo.findAllByUserAndProjectIds(userId, [proj1.projectId])

        def afterPointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def afterPointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)
        def afterPointHistoryUser2Proj1 = skillsService.getPointHistory(userId2, proj1.projectId)
        def afterPointHistoryUser2Proj1Subj1 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId)

        then:
        lastAddProj1Skill1ResUser1.completed.size() == 1
        lastAddProj1Skill1ResUser1.completed.get(0).id == proj1_skills.get(0).skillId
        lastAddProj1Skill3ResUser1.completed.find { it.type == "Skill" }.id == proj1_skills.get(2).skillId

        lastAddProj2Skill1ResUser1.completed.size() == 1
        lastAddProj2Skill1ResUser1.completed.get(0).id == proj2_skills.get(0).skillId
        lastAddProj2Skill3ResUser1.completed.find { it.type == "Skill" }.id == proj2_skills.get(2).skillId

        // validate that events were properly removed, the latest events must be removed
        eventsBeforeUser1Skill1.size() == 5
        eventsAfterUser1Skill1.size() == 3
        eventsBeforeUser1Skill1.sort({ it.performedOn }).subList(0, 3).collect({ it.id }) == eventsAfterUser1Skill1.sort({ it.performedOn }).collect({ it.id })

        // validate that events were kept for user 1 - skill 3
        eventsBeforeUser1Skill3.size() == 5
        eventsAfterUser1Skill3.size() == 5
        eventsBeforeUser1Skill3.sort({ it.performedOn }).collect({ it.id }) == eventsAfterUser1Skill3.sort({ it.performedOn }).collect({ it.id })

        // validate that events were kept for user2
        eventsBeforeUser2Skill1.size() == 2
        eventsAfterUser2Skill1.size() == 2
        eventsBeforeUser2Skill1.sort({ it.performedOn }).collect({ it.id }) == eventsAfterUser2Skill1.sort({ it.performedOn }).collect({ it.id })

        // validate the achievement is persistent
        beforeAchievements.find { it.skillId == proj1_skills.get(0).skillId }
        afterAchievements.find { it.skillId == proj1_skills.get(0).skillId }

        // user 1
        beforeChangeUser1Proj1Summary.points == 100
        beforeChangeUser1Proj1Summary.todaysPoints == 60
        beforeChangeUser1Proj1Summary.subjects.get(0).points == 100
        beforeChangeUser1Proj1Summary.subjects.get(0).todaysPoints == 60

        beforeChangeUser1Proj1SummarySubj1
        beforeChangeUser1Proj1SummarySubj1.points == 100
        List skillsBeforeChange = beforeChangeUser1Proj1SummarySubj1.skills.sort { it.skillId }
        skillsBeforeChange.get(0).points == 50
        skillsBeforeChange.get(0).todaysPoints == 30
        skillsBeforeChange.get(1).points == 0
        skillsBeforeChange.get(1).todaysPoints == 0
        skillsBeforeChange.get(2).points == 50
        skillsBeforeChange.get(2).todaysPoints == 30

        afterChangeUser1Proj1Summary.points == 80
        afterChangeUser1Proj1Summary.todaysPoints == 40
        afterChangeUser1Proj1Summary.subjects.get(0).points == 80
        afterChangeUser1Proj1Summary.subjects.get(0).todaysPoints == 40

        afterChangeUser1Proj1SummarySubj1.points == 80
        List skillsAfterChange = afterChangeUser1Proj1SummarySubj1.skills.sort { it.skillId }
        skillsAfterChange.get(0).points == 30
        skillsAfterChange.get(0).todaysPoints == 10
        skillsAfterChange.get(1).points == 0
        skillsAfterChange.get(1).todaysPoints == 0
        skillsAfterChange.get(2).points == 50
        skillsAfterChange.get(2).todaysPoints == 30

        // project 2 should not be changed
        afterChangeUser1Proj2Summary.points == 100
        afterChangeUser1Proj2Summary.todaysPoints == 60
        afterChangeUser1Proj2Summary.subjects.get(0).points == 100
        afterChangeUser1Proj2Summary.subjects.get(0).todaysPoints == 60
        afterChangeUser1Proj2SummarySubj1.points == 100
        List skillsAfterChangeProj2 = afterChangeUser1Proj2SummarySubj1.skills.sort { it.skillId }
        skillsAfterChangeProj2.get(0).points == 50
        skillsAfterChangeProj2.get(0).todaysPoints == 30
        skillsAfterChangeProj2.get(1).points == 0
        skillsAfterChangeProj2.get(1).todaysPoints == 0
        skillsAfterChangeProj2.get(2).points == 50
        skillsAfterChangeProj2.get(2).todaysPoints == 30

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

        afterChangeUser2Proj1Summary.points == 20
        afterChangeUser2Proj1Summary.todaysPoints == 10
        List skillsAfterUser2Change = afterChangeUser2Proj1SummarySubj1.skills.sort { it.skillId }
        skillsAfterUser2Change.get(0).points == 20
        skillsAfterUser2Change.get(0).todaysPoints == 10
        skillsAfterUser2Change.get(1).points == 0
        skillsAfterUser2Change.get(1).todaysPoints == 0
        skillsAfterUser2Change.get(2).points == 0
        skillsAfterUser2Change.get(1).todaysPoints == 0

        afterChangeUser2Proj2Summary.points == 20
        afterChangeUser2Proj2Summary.todaysPoints == 10
        List skillsAfterUser2ChangeProj2 = afterChangeUser2Proj2SummarySubj1.skills.sort { it.skillId }
        skillsAfterUser2ChangeProj2.get(0).points == 20
        skillsAfterUser2ChangeProj2.get(0).todaysPoints == 10
        skillsAfterUser2ChangeProj2.get(1).points == 0
        skillsAfterUser2ChangeProj2.get(1).todaysPoints == 0
        skillsAfterUser2ChangeProj2.get(2).points == 0
        skillsAfterUser2ChangeProj2.get(1).todaysPoints == 0

        /////////////////////////
        // Point History Validation
        beforePointHistoryUser1Proj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj1, 2) == 20
        getPoints(beforePointHistoryUser1Proj1, 1) == 40
        getPoints(beforePointHistoryUser1Proj1, 0) == 100

        afterPointHistoryUser1Proj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj1, 2) == 20
        getPoints(afterPointHistoryUser1Proj1, 1) == 40
        getPoints(afterPointHistoryUser1Proj1, 0) == 80

        beforePointHistoryUser1Proj1Subj1
        getPoints(beforePointHistoryUser1Proj1Subj1, 2) == 20
        getPoints(beforePointHistoryUser1Proj1Subj1, 1) == 40
        getPoints(beforePointHistoryUser1Proj1Subj1, 0) == 100

        afterPointHistoryUser1Proj1Subj1
        getPoints(afterPointHistoryUser1Proj1Subj1, 2) == 20
        getPoints(afterPointHistoryUser1Proj1Subj1, 1) == 40
        getPoints(afterPointHistoryUser1Proj1Subj1, 0) == 80

        afterPointHistoryUser2Proj1
        getPoints(afterPointHistoryUser2Proj1, 1) == 10
        getPoints(afterPointHistoryUser2Proj1, 0) == 20

        afterPointHistoryUser2Proj1Subj1
        getPoints(afterPointHistoryUser2Proj1Subj1, 1) == 10
        getPoints(afterPointHistoryUser2Proj1Subj1, 0) == 20
    }


    def "reduce skill occurrences - multiple users and multiple subjects - check pt history"() {

        String userId = "user1"
        String userId2 = "user2"

        def proj1, proj1_subj, proj1_skills, proj1_subj2, proj1_skills_subj2
        def proj2, proj2_subj, proj2_skills, proj2_subj2, proj2_skills_subj2
        (proj1, proj1_subj, proj1_skills, proj1_subj2, proj1_skills_subj2) = createProject.call(1, true)
        (proj2, proj2_subj, proj2_skills, proj2_subj2, proj2_skills_subj2) = createProject.call(2, true)

        // proj 1 - user 1
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 2).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date() - 0).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId, new Date() - 0).body

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(0).skillId], userId, new Date() - 0).body
        fullyAchieveSkill.call(userId, proj1.projectId, proj1_skills_subj2.get(1).skillId)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(2).skillId], userId, new Date() - 2).body

        // proj 1 - user 2
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId2, new Date() - 0).body
        fullyAchieveSkill.call(userId2, proj1.projectId, proj1_skills.get(1).skillId)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId2, new Date() - 2).body

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(0).skillId], userId2, new Date() - 2).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(0).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(1).skillId], userId2, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(1).skillId], userId2, new Date() - 0).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills_subj2.get(2).skillId], userId2, new Date() - 0).body

        // proj2 - user 1 - fully achieve it all
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(0).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(1).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills.get(2).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills_subj2.get(0).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills_subj2.get(1).skillId)
        fullyAchieveSkill.call(userId, proj2.projectId, proj2_skills_subj2.get(2).skillId)

        def beforePointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def beforePointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)
        def beforePointHistoryUser1Proj1Subj2 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj2.subjectId)

        def beforePointHistoryUser2Proj1 = skillsService.getPointHistory(userId2, proj1.projectId)
        def beforePointHistoryUser2Proj1Subj1 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId)
        def beforePointHistoryUser2Proj1Subj2 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj2.subjectId)

        def beforePointHistoryUser1Proj2 = skillsService.getPointHistory(userId, proj2.projectId)
        def beforePointHistoryUser1Proj2Subj1 = skillsService.getPointHistory(userId, proj2.projectId, proj2_subj.subjectId)
        def beforePointHistoryUser1Proj2Subj2 = skillsService.getPointHistory(userId, proj2.projectId, proj2_subj2.subjectId)

        when:
        proj1_skills.get(0).numPerformToCompletion = 1
        skillsService.createSkill(proj1_skills.get(0))

        def afterPointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def afterPointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)
        def afterPointHistoryUser1Proj1Subj2 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj2.subjectId)

        def afterPointHistoryUser2Proj1 = skillsService.getPointHistory(userId2, proj1.projectId)
        def afterPointHistoryUser2Proj1Subj1 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId)
        def afterPointHistoryUser2Proj1Subj2 = skillsService.getPointHistory(userId2, proj1.projectId, proj1_subj2.subjectId)

        def afterPointHistoryUser1Proj2 = skillsService.getPointHistory(userId, proj2.projectId)
        def afterPointHistoryUser1Proj2Subj1 = skillsService.getPointHistory(userId, proj2.projectId, proj2_subj.subjectId)
        def afterPointHistoryUser1Proj2Subj2 = skillsService.getPointHistory(userId, proj2.projectId, proj2_subj2.subjectId)

        then:
        /// BEFORE
        // user 1 - proj1
        beforePointHistoryUser1Proj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj1, 2) == 30
        getPoints(beforePointHistoryUser1Proj1, 1) == 60
        getPoints(beforePointHistoryUser1Proj1, 0) == 120

        beforePointHistoryUser1Proj1Subj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj1Subj1, 2) == 10
        getPoints(beforePointHistoryUser1Proj1Subj1, 1) == 30
        getPoints(beforePointHistoryUser1Proj1Subj1, 0) == 50

        beforePointHistoryUser1Proj1Subj2.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj1Subj2, 2) == 20
        getPoints(beforePointHistoryUser1Proj1Subj2, 1) == 30
        getPoints(beforePointHistoryUser1Proj1Subj2, 0) == 70

        // user 2 - proj1
        beforePointHistoryUser2Proj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser2Proj1, 2) == 30
        getPoints(beforePointHistoryUser2Proj1, 1) == 60
        getPoints(beforePointHistoryUser2Proj1, 0) == 120

        beforePointHistoryUser2Proj1Subj2.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser2Proj1Subj1, 2) == 20
        getPoints(beforePointHistoryUser2Proj1Subj1, 1) == 30
        getPoints(beforePointHistoryUser2Proj1Subj1, 0) == 70

        beforePointHistoryUser2Proj1Subj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser2Proj1Subj2, 2) == 10
        getPoints(beforePointHistoryUser2Proj1Subj2, 1) == 30
        getPoints(beforePointHistoryUser2Proj1Subj2, 0) == 50

        // user 1 - proj 2
        beforePointHistoryUser1Proj2.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj2, 2) == 60
        getPoints(beforePointHistoryUser1Proj2, 1) == 120
        getPoints(beforePointHistoryUser1Proj2, 0) == 120 + 180

        beforePointHistoryUser1Proj2Subj1.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj2Subj1, 2) == 30
        getPoints(beforePointHistoryUser1Proj2Subj1, 1) == 60
        getPoints(beforePointHistoryUser1Proj2Subj1, 0) == 60 + 90

        beforePointHistoryUser1Proj2Subj2.pointsHistory.size() == 3
        getPoints(beforePointHistoryUser1Proj2Subj2, 2) == 30
        getPoints(beforePointHistoryUser1Proj2Subj2, 1) == 60
        getPoints(beforePointHistoryUser1Proj2Subj2, 0) == 60 + 90

        /// AFTER
        // user 1 - proj1
        afterPointHistoryUser1Proj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj1, 2) == 30
        getPoints(afterPointHistoryUser1Proj1, 1) == 50 // LOST 10 POINTS
        getPoints(afterPointHistoryUser1Proj1, 0) == 110 // LOST 10 POINTS

        afterPointHistoryUser1Proj1Subj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj1Subj1, 2) == 10
        getPoints(afterPointHistoryUser1Proj1Subj1, 1) == 20 // LOST 10 POINTS
        getPoints(afterPointHistoryUser1Proj1Subj1, 0) == 40 // LOST 10 POINTS

        afterPointHistoryUser1Proj1Subj2.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj1Subj2, 2) == 20
        getPoints(afterPointHistoryUser1Proj1Subj2, 1) == 30
        getPoints(afterPointHistoryUser1Proj1Subj2, 0) == 70

        // user 2 - proj1
        afterPointHistoryUser2Proj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser2Proj1, 2) == 30
        getPoints(afterPointHistoryUser2Proj1, 1) == 60
        getPoints(afterPointHistoryUser2Proj1, 0) == 120

        afterPointHistoryUser2Proj1Subj2.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser2Proj1Subj1, 2) == 20
        getPoints(afterPointHistoryUser2Proj1Subj1, 1) == 30
        getPoints(afterPointHistoryUser2Proj1Subj1, 0) == 70

        afterPointHistoryUser2Proj1Subj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser2Proj1Subj2, 2) == 10
        getPoints(afterPointHistoryUser2Proj1Subj2, 1) == 30
        getPoints(afterPointHistoryUser2Proj1Subj2, 0) == 50

        // user 1 - proj 2
        afterPointHistoryUser1Proj2.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj2, 2) == 60
        getPoints(afterPointHistoryUser1Proj2, 1) == 120
        getPoints(afterPointHistoryUser1Proj2, 0) == 120 + 180

        afterPointHistoryUser1Proj2Subj1.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj2Subj1, 2) == 30
        getPoints(afterPointHistoryUser1Proj2Subj1, 1) == 60
        getPoints(afterPointHistoryUser1Proj2Subj1, 0) == 60 + 90

        afterPointHistoryUser1Proj2Subj2.pointsHistory.size() == 3
        getPoints(afterPointHistoryUser1Proj2Subj2, 2) == 30
        getPoints(afterPointHistoryUser1Proj2Subj2, 1) == 60
        getPoints(afterPointHistoryUser1Proj2Subj2, 0) == 60 + 90
    }

    def "do not change points if user have not fu-filled removed occurrences"() {
        String userId = "user1"

        def proj1, proj1_subj, proj1_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 1).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 0).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 0).body

        when:
        proj1_skills.get(0).numPerformToCompletion = 3
        skillsService.createSkill(proj1_skills.get(0))

        def afterChangeUser1Proj1Summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def afterChangeUser1Proj1SummarySubj1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId)

        def afterPointHistoryUser1Proj1 = skillsService.getPointHistory(userId, proj1.projectId)
        def afterPointHistoryUser1Proj1Subj1 = skillsService.getPointHistory(userId, proj1.projectId, proj1_subj.subjectId)

        then:
        afterChangeUser1Proj1Summary.points == 30
        afterChangeUser1Proj1Summary.todaysPoints == 20
        afterChangeUser1Proj1Summary.subjects.get(0).points == 30
        afterChangeUser1Proj1Summary.subjects.get(0).todaysPoints == 20

        afterChangeUser1Proj1SummarySubj1.points == 30
        List skillsAfterChange = afterChangeUser1Proj1SummarySubj1.skills.sort { it.skillId }
        skillsAfterChange.get(0).points == 30
        skillsAfterChange.get(0).todaysPoints == 20
        skillsAfterChange.get(1).points == 0
        skillsAfterChange.get(1).todaysPoints == 0
        skillsAfterChange.get(2).points == 0
        skillsAfterChange.get(2).todaysPoints == 0

        afterPointHistoryUser1Proj1.pointsHistory.size() == 2
        getPoints(afterPointHistoryUser1Proj1, 1) == 10
        getPoints(afterPointHistoryUser1Proj1, 0) == 30

        afterPointHistoryUser1Proj1Subj1.pointsHistory.size() == 2
        getPoints(afterPointHistoryUser1Proj1Subj1, 1) == 10
        getPoints(afterPointHistoryUser1Proj1Subj1, 0) == 30
    }

    def "decreasing occurrences puts user(s) into completion of the skill - multiple users"() {
        String userId1 = "user1" // will get an achievement
        String userId2 = "user2" // will be 1 event short
        String userId3 = "user3" // already achieved skill 1 so will keep its achievement
        String userId4 = "user4" // achieved another skill, should not be changed

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId1, new Date() - 0).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId1, new Date() - 0).body

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 3).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        proj1_skills.get(0).numPerformToCompletion = 2
        skillsService.createSkill(proj1_skills.get(0))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        // validate that 1 achievement was added for user 1 for skill 1
        beforeAchievements.size() == afterAchievements.size() - 1
        !beforeAchievements.find { it.userId == userId1 && it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }
        afterAchievements.find { it.userId == userId1 && it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["20/20", "40/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["10/20", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["20/20", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/20", "50/50", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["30/50", "40/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]
    }

    def "if occurrences are added after skill is completed then skill achievement will be removed"() {
        String userId1 = "user1" // will have an achievement needs to be removed
        String userId2 = "user2" //
        String userId3 = "user3" // will have an achievement needs to be removed
        String userId4 = "user4" //

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        proj1_skills.get(0).numPerformToCompletion = 10
        skillsService.createSkill(proj1_skills.get(0))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        beforeAchievements.size() == afterAchievements.size() + 2 // 2 achievement should be removed
        beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }.collect { it.userId }.sort() == [userId1, userId3]
        !afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["50/100", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["10/100", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["50/100", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/100", "50/50", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]
    }

    def "point increment increased and occurrences increased"() {
        String userId1 = "user1" // will have an achievement needs to be removed
        String userId2 = "user2" //
        String userId3 = "user3" // will have an achievement needs to be removed
        String userId4 = "user4" //

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        assert getPointHistory(userId1, proj1.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        assert getPointHistory(userId2, proj1.projectId) == []
        assert getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        assert getPointHistory(userId2, proj2.projectId) == []
        assert getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        assert getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        assert getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        proj1_skills.get(0).numPerformToCompletion = 10
        proj1_skills.get(0).pointIncrement = 15
        skillsService.createSkill(proj1_skills.get(0))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        beforeAchievements.size() == afterAchievements.size() + 2 // 2 achievement should be removed
        beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }.collect { it.userId }.sort() == [userId1, userId3]
        !afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["75/150", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["15/150", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["75/150", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/150", "50/50", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]

        getPointHistory(userId1, proj1.projectId) == [35, 70, 70 + 85]
        getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [35, 70, 70 + 85]
        getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        getPointHistory(userId2, proj1.projectId) == []
        getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        getPointHistory(userId2, proj2.projectId) == []
        getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        getPointHistory(userId3, proj1.projectId) == [15, 30, 75]
        getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [15, 30, 75]
        getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]
    }


    def "point increment decreased and occurrences increased"() {
        String userId1 = "user1" // will have an achievement needs to be removed
        String userId2 = "user2" //
        String userId3 = "user3" // will have an achievement needs to be removed
        String userId4 = "user4" //

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        assert getPointHistory(userId1, proj1.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        assert getPointHistory(userId2, proj1.projectId) == []
        assert getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        assert getPointHistory(userId2, proj2.projectId) == []
        assert getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        assert getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        assert getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        proj1_skills.get(0).numPerformToCompletion = 10
        proj1_skills.get(0).pointIncrement = 3
        skillsService.createSkill(proj1_skills.get(0))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        beforeAchievements.size() == afterAchievements.size() + 2 // 2 achievement should be removed
        beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }.collect { it.userId }.sort() == [userId1, userId3]
        !afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(0).skillId }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["15/30", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["3/30", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["15/30", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/30", "50/50", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]

        getPointHistory(userId1, proj1.projectId) == [23, 46, 46 + 3 * 3 + 10 * 3 + 10]
        getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [23, 46, 46 + 3 * 3 + 10 * 3 + 10]
        getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        getPointHistory(userId2, proj1.projectId) == []
        getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        getPointHistory(userId2, proj2.projectId) == []
        getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        getPointHistory(userId3, proj1.projectId) == [3, 6, 6 + 3 * 3]
        getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [3, 6, 6 + 3 * 3]
        getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]
    }

    def "point increment increased and occurrences decreased"() {
        String userId1 = "user1"
        String userId2 = "user2"
        String userId3 = "user3"
        String userId4 = "user4"

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        assert getPointHistory(userId1, proj1.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        assert getPointHistory(userId2, proj1.projectId) == []
        assert getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        assert getPointHistory(userId2, proj2.projectId) == []
        assert getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        assert getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        assert getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        proj1_skills.get(1).numPerformToCompletion = 3
        proj1_skills.get(1).pointIncrement = 15
        skillsService.createSkill(proj1_skills.get(1))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        beforeAchievements.size() == afterAchievements.size() - 1 // 1 achievement should be added
        !beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(1).skillId && it.userId == userId1 }
        afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(1).skillId && it.userId == userId1 }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["50/50", "45/45", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["10/50", "0/45", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["50/50", "0/45", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/50", "45/45", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]

        getPointHistory(userId1, proj1.projectId) == [35, 70, 70 + 75]
        getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [35, 70, 70 + 75]
        getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        getPointHistory(userId2, proj1.projectId) == []
        getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        getPointHistory(userId2, proj2.projectId) == []
        getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        getPointHistory(userId4, proj1.projectId) == [15, 30, 45]
        getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [15, 30, 45]
        getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]
    }

    def "point increment decreased and occurrences decreased"() {
        String userId1 = "user1"
        String userId2 = "user2"
        String userId3 = "user3"
        String userId4 = "user4"

        def proj1, proj1_subj, proj1_skills, proj2, proj2_subj, proj2_skills
        (proj1, proj1_subj, proj1_skills) = createProject.call(1)
        (proj2, proj2_subj, proj2_skills) = createProject.call(2)

        Closure initSkillsForProject = { String projId, def projSkills ->
            fullyAchieveSkill.call(userId1, projId, projSkills.get(0).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 2).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 1).body
            skillsService.addSkill([projectId: projId, skillId: projSkills.get(1).skillId], userId1, new Date() - 0).body

            fullyAchieveSkill.call(userId1, projId, projSkills.get(2).skillId)

            skillsService.addSkill([projectId: projId, skillId: projSkills.get(0).skillId], userId2, new Date() - 1).body

            fullyAchieveSkill.call(userId3, projId, projSkills.get(0).skillId)
            fullyAchieveSkill.call(userId4, projId, projSkills.get(1).skillId)
        }

        initSkillsForProject.call(proj1.projectId, proj1_skills)
        initSkillsForProject.call(proj2.projectId, proj2_skills)

        List<UserAchievement> beforeAchievements = userAchievementRepo.findAll()
        when:
        assert getPointHistory(userId1, proj1.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        assert getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        assert getPointHistory(userId2, proj1.projectId) == []
        assert getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        assert getPointHistory(userId2, proj2.projectId) == []
        assert getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        assert getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        assert getPointHistory(userId4, proj1.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        assert getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        proj1_skills.get(1).numPerformToCompletion = 3
        proj1_skills.get(1).pointIncrement = 3
        skillsService.createSkill(proj1_skills.get(1))

        List<UserAchievement> afterAchievements = userAchievementRepo.findAll()

        then:
        beforeAchievements.size() == afterAchievements.size() - 1 // 1 achievement should be added
        !beforeAchievements.findAll { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(1).skillId && it.userId == userId1 }
        afterAchievements.find { it.projectId == proj1.projectId && it.skillId == proj1_skills.get(1).skillId && it.userId == userId1 }

        getSubjectSkillsPtsSlashTotalPts(userId1, proj1.projectId, proj1_subj.subjectId) == ["50/50", "9/9", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj1.projectId, proj1_subj.subjectId) == ["10/50", "0/9", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj1.projectId, proj1_subj.subjectId) == ["50/50", "0/9", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj1.projectId, proj1_subj.subjectId) == ["0/50", "9/9", "0/50"]

        getSubjectSkillsPtsSlashTotalPts(userId1, proj2.projectId, proj2_subj.subjectId) == ["50/50", "30/50", "50/50"]
        getSubjectSkillsPtsSlashTotalPts(userId2, proj2.projectId, proj2_subj.subjectId) == ["10/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId3, proj2.projectId, proj2_subj.subjectId) == ["50/50", "0/50", "0/50"]
        getSubjectSkillsPtsSlashTotalPts(userId4, proj2.projectId, proj2_subj.subjectId) == ["0/50", "50/50", "0/50"]

        getPointHistory(userId1, proj1.projectId) == [23, 46, 46 + 63]
        getPointHistory(userId1, proj1.projectId, proj1_subj.subjectId) == [23, 46, 46 + 63]
        getPointHistory(userId1, proj2.projectId) == [30, 60, 60 + 70]
        getPointHistory(userId1, proj2.projectId, proj2_subj.subjectId) == [30, 60, 60 + 70]

        // user only has 1 day so history is not returned
        getPointHistory(userId2, proj1.projectId) == []
        getPointHistory(userId2, proj1.projectId, proj1_subj.subjectId) == []
        getPointHistory(userId2, proj2.projectId) == []
        getPointHistory(userId2, proj2.projectId, proj2_subj.subjectId) == []

        getPointHistory(userId3, proj1.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj1.projectId, proj1_subj.subjectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId3, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]

        getPointHistory(userId4, proj1.projectId) == [3, 6, 9]
        getPointHistory(userId4, proj1.projectId, proj1_subj.subjectId) == [3, 6, 9]
        getPointHistory(userId4, proj2.projectId) == [10, 20, 50]
        getPointHistory(userId4, proj2.projectId, proj2_subj.subjectId) == [10, 20, 50]
    }
}
