package skills.intTests.reportSkills

import groovy.time.BaseDuration
import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ReportSkills_TimeWindowSpecs extends DefaultIntSpec {
    String userId = "user1"

    def "only achieve the skill if it's outside of the configured pointIncrementInterval"() {
        String projId = "proj1"
        List<Map> subj1 = (1).collect {
            [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 1 * 60, numMaxOccurrencesIncrementInterval: 1]
        }

        when:
        skillsService.createSchema([subj1])

        Date theDate
        Date atTheStartOfIncrement
        Date atTheEndOfIncrement
        Date afterEnd
        Date beforeStart
        use(TimeCategory) {
            theDate = new Date().minus(1)
            atTheStartOfIncrement = theDate - 1.hour + 1.second
            atTheEndOfIncrement = theDate + 1.hour - 1.second
            afterEnd = theDate + 1.hour + 1.second
            beforeStart = theDate - 1.hour - 1.second
        }

        def theDateRes1 = skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, theDate)
        def theDateRes1SummaryRes = skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId)

        def theDateRes2 = skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, theDate)
        def theDateRes2SummaryRes = skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId)

        def atTheStartOfIncrementRes = skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, atTheStartOfIncrement)
        def atTheStartOfIncrementSummaryRes = skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId)

        def atTheEndOfIncrementRes = skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, atTheEndOfIncrement)
        def atTheEndOfIncrementSummaryRes = skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId)

        def afterEndRes = skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, afterEnd)
        def afterEndSummaryRes = skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId)

        def beforeStartRes = skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, beforeStart)
        def beforeStartSummaryRes = skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId)

        then:
        theDateRes1.body.skillApplied
        theDateRes1.body.explanation == "Skill event was applied"
        theDateRes1SummaryRes.skills.first().points == 10

        !theDateRes2.body.skillApplied
        theDateRes2.body.explanation == "This skill was already performed within the configured time period (within the last 1 hour)"
        theDateRes2SummaryRes.skills.first().points == 10

        !atTheStartOfIncrementRes.body.skillApplied
        atTheStartOfIncrementRes.body.explanation == "This skill was already performed within the configured time period (within the last 1 hour)"
        atTheStartOfIncrementSummaryRes.skills.first().points == 10

        !atTheEndOfIncrementRes.body.skillApplied
        atTheEndOfIncrementRes.body.explanation == "This skill was already performed within the configured time period (within the last 1 hour)"
        atTheEndOfIncrementSummaryRes.skills.first().points == 10

        afterEndRes.body.skillApplied
        afterEndRes.body.explanation == "Skill event was applied"
        afterEndSummaryRes.skills.first().points == 20

        beforeStartRes.body.skillApplied
        beforeStartRes.body.explanation == "Skill event was applied"
        beforeStartSummaryRes.skills.first().points == 30
    }

    def "if skill time window is disabled then skills should be achieved right away"() {
        def proj = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(2)

        skills.get(0).pointIncrementInterval = 0 // disable
        skills.get(0).numPerformToCompletion = 3

        skills.get(1).pointIncrementInterval = 5 // 5 minutes
        skills.get(1).numPerformToCompletion = 3

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        Date afterEnd
        Date beforeStart
        use(TimeCategory) {
            afterEnd = new Date() + 6.minutes
            beforeStart = new Date() - 6.minutes
        }

        Date sameDate = new Date()
        when:
        def skill1_res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, sameDate)
        def skill1_res2 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, sameDate)
        def skill1_res3 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, sameDate)
        def skill1_res4 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, sameDate)

        def skill2_res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], userId, sameDate)
        def skill2_res2 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], userId, sameDate)
        def skill2_res3 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], userId, sameDate)

        then:
        skill1_res1.body.skillApplied
        skill1_res2.body.skillApplied
        skill1_res3.body.skillApplied
        !skill1_res4.body.skillApplied

        skill2_res1.body.skillApplied
        !skill2_res2.body.skillApplied
        skill2_res2.body.explanation == "This skill was already performed within the configured time period (within the last 5 minutes)"
        !skill2_res3.body.skillApplied
        skill2_res3.body.explanation == "This skill was already performed within the configured time period (within the last 5 minutes)"
    }


    def "user should be able to perform up to configured Max Occurrences Within Time Window"() {
        def proj = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(2)

        skills.get(0).pointIncrementInterval = 5
        skills.get(0).numPerformToCompletion = 15
        skills.get(0).numMaxOccurrencesIncrementInterval = 4

        skills.get(1).pointIncrementInterval = 5 // 5 minutes
        skills.get(1).numPerformToCompletion = 3

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        List<Date> minutes = []
        use(TimeCategory) {
            (0..30).each {
                minutes.add(new Date().minus(1) + it.minutes)
            }
        }

        when:
        def skill1_res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(0))
        def skill1_res2 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(1))
        def skill1_res3 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(1))
        def skill1_res4 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(2))
        def skill1_res5 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(1))
        def skill1_res6 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(2))

        def skill1_res7 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(6))
        def skill1_res8 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(7))
        def skill1_res9 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(8))
        def skill1_res10 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(9))
        def skill1_res11 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(9))

        def skill1_res12 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(12)) // 7,8,6 fall within 5 minutes
        def skill1_res13 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(12)) // 7,8,6 fall within 5 minutes
        def skill1_res14 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(12)) // 7,8,6 fall within 5 minutes

        def skill1_res15 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(13))
        def skill1_res16 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(14))
        def skill1_res17 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(15)) // 12 *2, 13, 14 within 5 minutes
        def skill1_res18 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(20))
        def skill1_res19 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(21))
        def skill1_res20 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(22))
        def skill1_res21 = skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, minutes.get(23))

        then:
        skill1_res1.body.skillApplied
        skill1_res2.body.skillApplied
        skill1_res3.body.skillApplied
        skill1_res4.body.skillApplied
        !skill1_res5.body.skillApplied
        skill1_res5.body.explanation == "This skill was already performed 4 out of 4 times within the configured time period (within the last 5 minutes)"
        !skill1_res6.body.skillApplied
        skill1_res6.body.explanation == "This skill was already performed 4 out of 4 times within the configured time period (within the last 5 minutes)"

        skill1_res7.body.skillApplied
        skill1_res8.body.skillApplied
        skill1_res9.body.skillApplied
        skill1_res10.body.skillApplied
        !skill1_res11.body.skillApplied
        skill1_res11.body.explanation == "This skill was already performed 4 out of 4 times within the configured time period (within the last 5 minutes)"

        skill1_res12.body.skillApplied
        skill1_res13.body.skillApplied
        !skill1_res14.body.skillApplied
        skill1_res14.body.explanation == "This skill was already performed 4 out of 4 times within the configured time period (within the last 5 minutes)"

        skill1_res15.body.skillApplied
        skill1_res16.body.skillApplied
        !skill1_res17.body.skillApplied
        skill1_res17.body.explanation == "This skill was already performed 4 out of 4 times within the configured time period (within the last 5 minutes)"
        skill1_res18.body.skillApplied
        skill1_res19.body.skillApplied
        skill1_res20.body.skillApplied
        !skill1_res21.body.skillApplied
    }
}
