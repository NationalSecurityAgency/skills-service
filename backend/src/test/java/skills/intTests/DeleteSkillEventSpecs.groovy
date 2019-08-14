package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.intTests.utils.TestUtils
import spock.lang.Specification

class DeleteSkillEventSpecs extends DefaultIntSpec {

    TestUtils testUtils = new TestUtils()

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
    }

    def "delete skill event"() {
        String subj = "testSubj"
        String skillId = "skillId"

        setup:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill([projectId: projId, subjectId: subj, skillId: skillId, name: "Test Skill", type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
        ])
        def res = skillsService.addSkill([projectId: projId, skillId: skillId])

        assert res.body.skillApplied
        assert res.body.explanation == "Skill event was applied"

        assert res.body.completed.size() == 3
        assert res.body.completed.find({ it.type == "Skill" }).id == "skillId"
        assert res.body.completed.find({ it.type == "Skill" }).name == "Test Skill"

        assert res.body.completed.find({ it.type == "Overall" }).id == "OVERALL"
        assert res.body.completed.find({ it.type == "Overall" }).name == "OVERALL"
        assert res.body.completed.find({ it.type == "Overall" }).level == 5

        assert res.body.completed.find({ it.type == "Subject" }).id == "testSubj"
        assert res.body.completed.find({ it.type == "Subject" }).name == "Test Subject"
        assert res.body.completed.find({ it.type == "Subject" }).level == 5

        def addedSkills = skillsService.getPerformedSkills(skillsService.wsHelper.username, projId)
        assert addedSkills
        String addedSkillEventId = addedSkills.data.find { it.skillId == 'skillId'}.id
        assert addedSkillEventId

        when:
        skillsService.deleteSkillEvent([projectId: projId, skillEventId: addedSkillEventId])
        addedSkills = skillsService.getPerformedSkills(skillsService.wsHelper.username, projId)

        then:
        !addedSkills?.data?.find { it.id == addedSkillEventId }
    }


    def "cannot delete skill event after a dependent skill was performed"() {
        List<Map> skills = SkillsFactory.createSkills(2)

        setup:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(0).skillId])

        def res0 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId])
        def res = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId])

        assert res0.body.skillApplied
        assert res0.body.explanation == "Skill event was applied"

        assert res.body.skillApplied
        assert res.body.explanation == "Skill event was applied"

        def addedSkills = skillsService.getPerformedSkills(skillsService.wsHelper.username, projId)
        assert addedSkills
        String addedSkillEventId = addedSkills.data.find { it.skillId == skills.get(0).skillId}.id
        assert addedSkillEventId

        when:
        def response = skillsService.deleteSkillEvent([projectId: projId, skillEventId: addedSkillEventId])

        then:
        response.body.skillApplied == false
        response.body.explanation == 'You cannot delete a skill event when a parent skill dependency has already been performed. You must first delete the performed skills for the parent dependencies: [TestProject1:skill2].'
    }

    def "deleting skill event required for a badge will removed the achieved badge"() {
        String subj = "testSubj"
        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]


        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId]).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId]).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId]).body

        assert resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        assert resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        assert resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        assert resSkill4.skillApplied && resSkill4.completed.find { it.id == 'badge1'}

        when:
        def addedSkills = skillsService.getPerformedSkills(skillsService.wsHelper.username, projId)
        assert addedSkills?.count == 4
        String addedSkillEventId = addedSkills.data[2].id // grab the third skill performed, and delete it
        assert addedSkillEventId
        def badgesSummary = skillsService.getBadgesSummary(skillsService.wsHelper.username, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()
        assert badgesSummary.badgeId == 'badge1'
        assert badgesSummary.badgeAchieved == true
        assert badgesSummary.numSkillsAchieved == 4
        assert badgesSummary.numTotalSkills == 4

        skillsService.deleteSkillEvent([projectId: projId, skillEventId: addedSkillEventId])
        addedSkills = skillsService.getPerformedSkills(skillsService.wsHelper.username, projId)
        badgesSummary = skillsService.getBadgesSummary(skillsService.wsHelper.username, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()

        then:
        badgesSummary.badgeId == 'badge1'
        badgesSummary.badgeAchieved == false
        badgesSummary.numSkillsAchieved == 3
        badgesSummary.numTotalSkills == 4
        addedSkills?.count == 3
    }

    def "incrementally achieve a single skill, then delete one event and validate level, achievements and points are properly decremented"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        setup:
        skillsService.createSchema([subj1])

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)
        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(1).subjectId)
        }

        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        assert subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.points == 10
        assert subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        assert subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        assert !addSkillRes.get(0).body.completed.find({ it.type == "Skill" })

        assert subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.points == 20
        assert subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        assert subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        assert !addSkillRes.get(1).body.completed.find({ it.type == "Skill" })

        assert subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.points == 30
        assert subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        assert subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        assert !addSkillRes.get(2).body.completed.find({ it.type == "Skill" })

        assert subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.points == 40
        assert subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        assert subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        assert !addSkillRes.get(3).body.completed.find({ it.type == "Skill" })

        assert subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.points == 50
        assert subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 10
        assert subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        assert subjSummaryRes.get(4).skillsLevel == 3
        assert subjSummaryRes.get(4).levelPoints == 5
        assert subjSummaryRes.get(4).levelTotalPoints == 22

        assert addSkillRes.get(4).body.completed.find({ it.type == "Skill" }).id == subj1.get(1).skillId

        when:
        def addedSkills = skillsService.getPerformedSkills(userId, projId)
        assert addedSkills?.count == 5
        String addedSkillEventId = addedSkills.data[2].id // grab the third skill performed, and delete it
        assert addedSkillEventId

        skillsService.deleteSkillEvent([projectId: projId, skillEventId: addedSkillEventId])
        addedSkills = skillsService.getPerformedSkills(userId, projId)
        subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(1).subjectId)

        then:
        // skill event has been removed
        assert addedSkills?.count == 4
        !addedSkills?.data?.find { it.id == addedSkillEventId }

        // skill level should be reduced back to 2, levelPoints 15, levelTotalPoints 20, skill points 40 (so skill no longer completed)
        subjSummaryRes.get(5).skillsLevel == 2
        subjSummaryRes.get(5).levelPoints == 15
        subjSummaryRes.get(5).levelTotalPoints == 20

        subjSummaryRes.get(5).skills.find { it.skillId == subj1.get(1).skillId }.points == 40
        subjSummaryRes.get(5).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50
    }
}
