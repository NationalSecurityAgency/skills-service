package skills.intTests.reportSkills

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.TestUtils

class ReportSkillsSpecs extends DefaultIntSpec {

    TestUtils testUtils = new TestUtils()

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
    }

    def "complete very simple skill"() {
        String subj = "testSubj"
        String skillId = "skillId"

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill([projectId     : projId, subjectId: subj, skillId: skillId, name: "Test Skill", type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1])
        def res = skillsService.addSkill([projectId: projId, skillId: skillId])

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"

        res.body.completed.size() == 3
        res.body.completed.find({ it.type == "Skill" }).id == "skillId"
        res.body.completed.find({ it.type == "Skill" }).name == "Test Skill"

        res.body.completed.find({ it.type == "Overall" }).id == "OVERALL"
        res.body.completed.find({ it.type == "Overall" }).name == "OVERALL"
            res.body.completed.find({ it.type == "Overall" }).level == 5

        res.body.completed.find({ it.type == "Subject" }).id == "testSubj"
        res.body.completed.find({ it.type == "Subject" }).name == "Test Subject"
        res.body.completed.find({ it.type == "Subject" }).level == 5
    }

    def "incrementally achieve a single skill"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createSchema([subj1])

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)
        (0..4).each {
            println "Adding ${subj1.get(1).skillId} on ${dates.get(it)}"
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(1).subjectId)
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.points == 10
        subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50
        subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0

        !addSkillRes.get(0).body.completed.find({ it.type == "Skill" })

        subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.points == 20
        subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        !addSkillRes.get(1).body.completed.find({ it.type == "Skill" })

        subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.points == 30
        subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        !addSkillRes.get(2).body.completed.find({ it.type == "Skill" })

        subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.points == 40
        subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        !addSkillRes.get(3).body.completed.find({ it.type == "Skill" })

        subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.points == 50
        subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 10
        subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        addSkillRes.get(4).body.completed.find({ it.type == "Skill" }).id == subj1.get(1).skillId
    }

    def "achieve subject's level through a single skill"(){
        List<Map> subj1 = (1..5).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createSchema([subj1, subj2, subj3])

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)
        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(1).subjectId)
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        assert !addSkillRes.get(0).body.completed
        assert subjSummaryRes.get(0).skillsLevel == 0
        assert subjSummaryRes.get(0).points == 10
        assert subjSummaryRes.get(0).todaysPoints == 0
        assert subjSummaryRes.get(0).levelPoints == 10

        assert !addSkillRes.get(1).body.completed
        assert subjSummaryRes.get(1).skillsLevel == 0
        assert subjSummaryRes.get(1).points == 20
        assert subjSummaryRes.get(1).todaysPoints == 0
        assert subjSummaryRes.get(1).levelPoints == 20

        assert !addSkillRes.get(2).body.completed
        assert subjSummaryRes.get(2).skillsLevel == 0
        assert subjSummaryRes.get(2).points == 30
        assert subjSummaryRes.get(2).todaysPoints == 0
        assert subjSummaryRes.get(2).levelPoints == 30

        assert !addSkillRes.get(3).body.completed
        assert subjSummaryRes.get(3).skillsLevel == 0
        assert subjSummaryRes.get(3).points == 40
        assert subjSummaryRes.get(3).todaysPoints == 0
        assert subjSummaryRes.get(3).levelPoints == 40

        assert addSkillRes.get(4).body.completed.size() == 1
        assert addSkillRes.get(4).body.completed.get(0).type == "Subject"
        assert addSkillRes.get(4).body.completed.get(0).level == 1
        assert addSkillRes.get(4).body.completed.get(0).id == "subj1"

        assert subjSummaryRes.get(4).skillsLevel == 1
        assert subjSummaryRes.get(4).points == 50
        assert subjSummaryRes.get(4).todaysPoints == 10
        assert subjSummaryRes.get(4).levelPoints == 0
    }

    def "achieve subject's level by progressing through several skill"(){
        List<Map> subj1 = (1..5).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "s3${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, totalPoints: 200, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createSchema([subj1, subj2, subj3])

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)
        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(it).skillId], userId, dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(it).subjectId)
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        !addSkillRes.get(0).body.completed
        subjSummaryRes.get(0).skillsLevel == 0
        subjSummaryRes.get(0).points == 10
        subjSummaryRes.get(0).todaysPoints == 0
        subjSummaryRes.get(0).levelPoints == 10

        !addSkillRes.get(1).body.completed
        subjSummaryRes.get(1).skillsLevel == 0
        subjSummaryRes.get(1).points == 20
        subjSummaryRes.get(1).todaysPoints == 0
        subjSummaryRes.get(1).levelPoints == 20

        !addSkillRes.get(2).body.completed
        subjSummaryRes.get(2).skillsLevel == 0
        subjSummaryRes.get(2).points == 30
        subjSummaryRes.get(2).todaysPoints == 0
        subjSummaryRes.get(2).levelPoints == 30

        !addSkillRes.get(3).body.completed
        subjSummaryRes.get(3).skillsLevel == 0
        subjSummaryRes.get(3).points == 40
        subjSummaryRes.get(3).todaysPoints == 0
        subjSummaryRes.get(3).levelPoints == 40

        addSkillRes.get(4).body.completed.size() == 1
        addSkillRes.get(4).body.completed.get(0).type == "Subject"
        addSkillRes.get(4).body.completed.get(0).level == 1
        addSkillRes.get(4).body.completed.get(0).id == "subj1"

        subjSummaryRes.get(4).skillsLevel == 1
        subjSummaryRes.get(4).points == 50
        subjSummaryRes.get(4).todaysPoints == 10
        subjSummaryRes.get(4).levelPoints == 0
    }


    def "fully achieve a subject"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 3, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createSchema([subj1, subj2, subj3])

        List<Date> dates = testUtils.getLastNDays(3)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)
        (0..1).each { int skillIndex ->
            (0..2).each {
                addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(skillIndex).skillId], userId, dates.get(it))
                subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId)
            }
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        subjSummaryRes.get(0).skillsLevel == 1
        subjSummaryRes.get(0).points == 10
        subjSummaryRes.get(0).todaysPoints == 0
        subjSummaryRes.get(0).levelPoints == 4
        addSkillRes.get(0).body.completed.find { it.type == "Subject" }.level == 1

        subjSummaryRes.get(1).skillsLevel == 2
        subjSummaryRes.get(1).points == 20
        subjSummaryRes.get(1).todaysPoints == 0
        subjSummaryRes.get(1).levelPoints == 5
        addSkillRes.get(1).body.completed.find { it.type == "Subject" }.level == 2

        subjSummaryRes.get(2).skillsLevel == 3
        subjSummaryRes.get(2).points == 30
        subjSummaryRes.get(2).todaysPoints == 10
        subjSummaryRes.get(2).levelPoints == 3
        addSkillRes.get(2).body.completed.find { it.type == "Subject" }.level == 3

        subjSummaryRes.get(3).skillsLevel == 4
        subjSummaryRes.get(3).points == 40
        subjSummaryRes.get(3).todaysPoints == 10
        subjSummaryRes.get(3).levelPoints == 0
        addSkillRes.get(3).body.completed.find { it.type == "Subject" }.level == 4

        subjSummaryRes.get(4).skillsLevel == 4
        subjSummaryRes.get(4).points == 50
        subjSummaryRes.get(4).todaysPoints == 10
        subjSummaryRes.get(4).levelPoints == 10
        subjSummaryRes.get(4).levelTotalPoints == 15
        !addSkillRes.get(4).body.completed.find { it.type == "Subject" }

        subjSummaryRes.get(5).skillsLevel == 5
        subjSummaryRes.get(5).points == 60
        subjSummaryRes.get(5).todaysPoints == 20
        subjSummaryRes.get(5).levelPoints == 5
        subjSummaryRes.get(5).levelTotalPoints == -1
        addSkillRes.get(5).body.completed.find { it.type == "Subject" }.level == 5
    }

    def "fully achieve overall"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 3, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 4, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..2).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }


        //30, 20, 20 -- used to be 30, 20, 6
        //60, 80, 12 -- 60, 80, 40

        List<List<Map>> subjects = [subj1, subj2, subj3]
        when:
        skillsService.createSchema(subjects)

        List<Date> dates = testUtils.getLastNDays(10)
        List addSkillRes = []
        List summaries = []
        String userId = sampleUserIds.get(0)

        subjects.each { List<Map> subject ->
            subject.each { Map skill->
                (0..(skill.numPerformToCompletion-1)).each {
                    addSkillRes << skillsService.addSkill([projectId: projId, skillId: skill.skillId], userId, dates.get(it))
                    summaries << skillsService.getSkillSummary(userId, projId)
                }
            }
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }

        summaries.get(0).skillsLevel == 0
        summaries.get(0).points == 10
        summaries.get(0).todaysPoints == 0
        summaries.get(0).levelPoints == 10
        summaries.get(0).levelTotalPoints == 18
        summaries.get(0).totalPoints == 180
        !addSkillRes.get(0).body.completed.find { it.type == "Overall" }

        summaries.get(1).skillsLevel == 1
        summaries.get(1).points == 20
        summaries.get(1).todaysPoints == 0
        summaries.get(1).levelPoints == 20 - 18
        addSkillRes.get(1).body.completed.find { it.type == "Overall" }.level == 1

        summaries.get(2).skillsLevel == 1
        summaries.get(2).points == 30
        summaries.get(2).todaysPoints == 0
        summaries.get(2).levelPoints == 30 - 18
        !addSkillRes.get(2).body.completed.find { it.type == "Overall" }

        summaries.get(3).skillsLevel == 1
        summaries.get(3).points == 40
        summaries.get(3).todaysPoints == 0
        summaries.get(3).levelPoints == 40 - 18
        !addSkillRes.get(3).body.completed.find { it.type == "Overall" }

        summaries.get(4).skillsLevel == 2
        summaries.get(4).points == 50
        summaries.get(4).todaysPoints == 0
        summaries.get(4).levelPoints == 50 - 45
        addSkillRes.get(4).body.completed.find { it.type == "Overall" }.level == 2

        summaries.get(5).skillsLevel == 2
        summaries.get(5).points == 60
        summaries.get(5).todaysPoints == 0
        summaries.get(5).levelPoints == 60 - 45
        !addSkillRes.get(5).body.completed.find { it.type == "Overall" }

        summaries.get(6).skillsLevel == 2
        summaries.get(6).points == 65
        summaries.get(6).todaysPoints == 0
        summaries.get(6).levelPoints == 65 - 45
        !addSkillRes.get(6).body.completed.find { it.type == "Overall" }

        summaries.get(7).skillsLevel == 2
        summaries.get(7).points == 70
        summaries.get(7).todaysPoints == 0
        summaries.get(7).levelPoints == 70 - 45
        !addSkillRes.get(7).body.completed.find { it.type == "Overall" }

        summaries.get(8).skillsLevel == 2
        summaries.get(8).points == 75
        summaries.get(8).todaysPoints == 0
        summaries.get(8).levelPoints == 75 - 45
        !addSkillRes.get(8).body.completed.find { it.type == "Overall" }

        summaries.get(9).points == 80
        summaries.get(9).skillsLevel == 2
        summaries.get(9).todaysPoints == 0
        summaries.get(9).levelPoints == 80 - 45
        !addSkillRes.get(9).body.completed.find { it.type == "Overall" }

        summaries.get(10).points == 85
        summaries.get(10).skillsLevel == 3
        summaries.get(10).todaysPoints == 0
        summaries.get(10).levelPoints == 85 - (45+36)
        addSkillRes.get(10).body.completed.find { it.type == "Overall" }.level == 3

        summaries.get(11).points == 90
        summaries.get(11).skillsLevel == 3
        summaries.get(11).todaysPoints == 0
        summaries.get(11).levelPoints == 90 - (45+36)
        !addSkillRes.get(11).body.completed.find { it.type == "Overall" }

        summaries.get(12).points == 95
        summaries.get(12).skillsLevel == 3
        summaries.get(12).todaysPoints == 0
        summaries.get(12).levelPoints == 95 - (45+36)
        !addSkillRes.get(12).body.completed.find { it.type == "Overall" }

        summaries.get(13).points == 100
        summaries.get(13).skillsLevel == 3
        summaries.get(13).todaysPoints == 0
        summaries.get(13).levelPoints == 100 - (45+36)
        !addSkillRes.get(13).body.completed.find { it.type == "Overall" }

        summaries.get(14).points == 105
        summaries.get(14).skillsLevel == 3
        summaries.get(14).todaysPoints == 0
        summaries.get(14).levelPoints == 105 - (45+36)
        !addSkillRes.get(14).body.completed.find { it.type == "Overall" }

        summaries.get(15).points == 110
        summaries.get(15).skillsLevel == 3
        summaries.get(15).todaysPoints == 0
        summaries.get(15).levelPoints == 110 - (45+36)
        !addSkillRes.get(15).body.completed.find { it.type == "Overall" }

        summaries.get(16).points == 115
        summaries.get(16).skillsLevel == 3
        summaries.get(16).todaysPoints == 0
        summaries.get(16).levelPoints == 115 - (45+36)
        !addSkillRes.get(16).body.completed.find { it.type == "Overall" }

        summaries.get(17).points == 120
        summaries.get(17).skillsLevel == 4
        summaries.get(17).todaysPoints == 0
        summaries.get(17).levelPoints == 120 - (45+36+39)
        addSkillRes.get(17).body.completed.find { it.type == "Overall" }.level == 4

        summaries.get(18).points == 125
        summaries.get(18).skillsLevel == 4
        summaries.get(18).todaysPoints == 0
        summaries.get(18).levelPoints == 125 - (45+36+39)
        !addSkillRes.get(18).body.completed.find { it.type == "Overall" }

        summaries.get(19).points == 130
        summaries.get(19).skillsLevel == 4
        summaries.get(19).todaysPoints == 0
        summaries.get(19).levelPoints == 130 - (45+36+39)
        !addSkillRes.get(19).body.completed.find { it.type == "Overall" }

        summaries.get(20).points == 135
        summaries.get(20).skillsLevel == 4
        summaries.get(20).todaysPoints == 0
        summaries.get(20).levelPoints == 135 - (45+36+39)
        !addSkillRes.get(20).body.completed.find { it.type == "Overall" }

        summaries.get(21).points == 140
        summaries.get(21).skillsLevel == 4
        summaries.get(21).todaysPoints == 0
        summaries.get(21).levelPoints == 140 - (45+36+39)
        !addSkillRes.get(21).body.completed.find { it.type == "Overall" }

        summaries.get(22).points == 150
        summaries.get(22).skillsLevel == 4
        summaries.get(22).todaysPoints == 0
        summaries.get(22).levelPoints == 150 - (45+36+39)
        !addSkillRes.get(22).body.completed.find { it.type == "Overall" }

        summaries.get(23).points == 160
        summaries.get(23).skillsLevel == 4
        summaries.get(23).todaysPoints == 0
        summaries.get(23).levelPoints == 160 - (45+36+39)
        !addSkillRes.get(23).body.completed.find { it.type == "Overall" }

        summaries.get(24).points == 170
        summaries.get(24).skillsLevel == 5
        summaries.get(24).todaysPoints == 0
        summaries.get(24).levelPoints == 170 - (45+36+39+45)
        addSkillRes.get(24).body.completed.find { it.type == "Overall" }.level == 5
//        !addSkillRes.get(24).body.completed.find { it.type == "Overall" }

        summaries.get(25).points == 180
        summaries.get(25).skillsLevel == 5
        summaries.get(25).todaysPoints == 0
        summaries.get(25).levelPoints == 180 - (45+36+39+45)
        summaries.get(25).levelTotalPoints == -1
        !addSkillRes.get(25).body.completed.find { it.type == "Overall" }
    }

    def "two users achieving fully should not step on each other"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 3, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 4, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..2).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        List<List<Map>> subjects = [subj1, subj2, subj3]
        when:
        skillsService.createSchema(subjects)

        List<Date> dates = testUtils.getLastNDays(10)
        List addSkillRes = []
        List addSkillRes2 = []
        List summaries = []
        List summaries2 = []
        String userId = "user1"
        String userId2 = "user2"

        subjects.each { List<Map> subject ->
            subject.each { Map skill->
                (0..(skill.numPerformToCompletion-1)).each {
                    addSkillRes << skillsService.addSkill([projectId: projId, skillId: skill.skillId], userId, dates.get(it))
                    summaries << skillsService.getSkillSummary(userId, projId)
                }

                (0..(skill.numPerformToCompletion-1)).each {
                    addSkillRes2 << skillsService.addSkill([projectId: projId, skillId: skill.skillId], userId2, dates.get(it))
                    summaries2 << skillsService.getSkillSummary(userId2, projId)
                }
            }
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }

        addSkillRes2.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }

        assertResults(summaries, addSkillRes)

        // --------------------------------------
        // 2nd user
        // --------------------------------------
        assertResults(summaries2, addSkillRes2)
    }

    private boolean assertResults(List summaries, List addSkillRes){
        assert summaries.get(0).skillsLevel == 0
        assert summaries.get(0).points == 10
        assert summaries.get(0).todaysPoints == 0
        assert summaries.get(0).levelPoints == 10
        assert summaries.get(0).levelTotalPoints == 18
        assert summaries.get(0).totalPoints == 180
        assert !addSkillRes.get(0).body.completed.find { it.type == "Overall" }

        assert summaries.get(1).skillsLevel == 1
        assert summaries.get(1).points == 20
        assert summaries.get(1).todaysPoints == 0
        assert summaries.get(1).levelPoints == 20 - 18
        assert addSkillRes.get(1).body.completed.find { it.type == "Overall" }.level == 1

        assert summaries.get(2).skillsLevel == 1
        assert summaries.get(2).points == 30
        assert summaries.get(2).todaysPoints == 0
        assert summaries.get(2).levelPoints == 30 - 18
        assert !addSkillRes.get(2).body.completed.find { it.type == "Overall" }

        assert summaries.get(3).skillsLevel == 1
        assert summaries.get(3).points == 40
        assert summaries.get(3).todaysPoints == 0
        assert summaries.get(3).levelPoints == 40 - 18
        assert !addSkillRes.get(3).body.completed.find { it.type == "Overall" }

        assert summaries.get(4).skillsLevel == 2
        assert summaries.get(4).points == 50
        assert summaries.get(4).todaysPoints == 0
        assert summaries.get(4).levelPoints == 50 - 45
        assert addSkillRes.get(4).body.completed.find { it.type == "Overall" }.level == 2

        assert summaries.get(5).skillsLevel == 2
        assert summaries.get(5).points == 60
        assert summaries.get(5).todaysPoints == 0
        assert summaries.get(5).levelPoints == 60 - 45
        assert !addSkillRes.get(5).body.completed.find { it.type == "Overall" }

        assert summaries.get(6).skillsLevel == 2
        assert summaries.get(6).points == 65
        assert summaries.get(6).todaysPoints == 0
        assert summaries.get(6).levelPoints == 65 - 45
        assert !addSkillRes.get(6).body.completed.find { it.type == "Overall" }

        assert summaries.get(7).skillsLevel == 2
        assert summaries.get(7).points == 70
        assert summaries.get(7).todaysPoints == 0
        assert summaries.get(7).levelPoints == 70 - 45
        assert !addSkillRes.get(7).body.completed.find { it.type == "Overall" }

        assert summaries.get(8).skillsLevel == 2
        assert summaries.get(8).points == 75
        assert summaries.get(8).todaysPoints == 0
        assert summaries.get(8).levelPoints == 75 - 45
        assert !addSkillRes.get(8).body.completed.find { it.type == "Overall" }

        assert summaries.get(9).points == 80
        assert summaries.get(9).skillsLevel == 2
        assert summaries.get(9).todaysPoints == 0
        assert summaries.get(9).levelPoints == 80 - 45
        assert !addSkillRes.get(9).body.completed.find { it.type == "Overall" }

        assert summaries.get(10).points == 85
        assert summaries.get(10).skillsLevel == 3
        assert summaries.get(10).todaysPoints == 0
        assert summaries.get(10).levelPoints == 85 - (45+36)
        assert addSkillRes.get(10).body.completed.find { it.type == "Overall" }.level == 3

        assert summaries.get(11).points == 90
        assert summaries.get(11).skillsLevel == 3
        assert summaries.get(11).todaysPoints == 0
        assert summaries.get(11).levelPoints == 90 - (45+36)
        assert !addSkillRes.get(11).body.completed.find { it.type == "Overall" }

        assert summaries.get(12).points == 95
        assert summaries.get(12).skillsLevel == 3
        assert summaries.get(12).todaysPoints == 0
        assert summaries.get(12).levelPoints == 95 - (45+36)
        assert !addSkillRes.get(12).body.completed.find { it.type == "Overall" }

        assert summaries.get(13).points == 100
        assert summaries.get(13).skillsLevel == 3
        assert summaries.get(13).todaysPoints == 0
        assert summaries.get(13).levelPoints == 100 - (45+36)
        assert !addSkillRes.get(13).body.completed.find { it.type == "Overall" }

        assert summaries.get(14).points == 105
        assert summaries.get(14).skillsLevel == 3
        assert summaries.get(14).todaysPoints == 0
        assert summaries.get(14).levelPoints == 105 - (45+36)
        assert !addSkillRes.get(14).body.completed.find { it.type == "Overall" }

        assert summaries.get(15).points == 110
        assert summaries.get(15).skillsLevel == 3
        assert summaries.get(15).todaysPoints == 0
        assert summaries.get(15).levelPoints == 110 - (45+36)
        assert !addSkillRes.get(15).body.completed.find { it.type == "Overall" }

        assert summaries.get(16).points == 115
        assert summaries.get(16).skillsLevel == 3
        assert summaries.get(16).todaysPoints == 0
        assert summaries.get(16).levelPoints == 115 - (45+36)
        assert !addSkillRes.get(16).body.completed.find { it.type == "Overall" }

        assert summaries.get(17).points == 120
        assert summaries.get(17).skillsLevel == 4
        assert summaries.get(17).todaysPoints == 0
        assert summaries.get(17).levelPoints == 120 - (45+36+39)
        assert addSkillRes.get(17).body.completed.find { it.type == "Overall" }.level == 4

        assert summaries.get(18).points == 125
        assert summaries.get(18).skillsLevel == 4
        assert summaries.get(18).todaysPoints == 0
        assert summaries.get(18).levelPoints == 125 - (45+36+39)
        assert !addSkillRes.get(18).body.completed.find { it.type == "Overall" }

        assert summaries.get(19).points == 130
        assert summaries.get(19).skillsLevel == 4
        assert summaries.get(19).todaysPoints == 0
        assert summaries.get(19).levelPoints == 130 - (45+36+39)
        assert !addSkillRes.get(19).body.completed.find { it.type == "Overall" }

        assert summaries.get(20).points == 135
        assert summaries.get(20).skillsLevel == 4
        assert summaries.get(20).todaysPoints == 0
        assert summaries.get(20).levelPoints == 135 - (45+36+39)
        assert !addSkillRes.get(20).body.completed.find { it.type == "Overall" }

        assert summaries.get(21).points == 140
        assert summaries.get(21).skillsLevel == 4
        assert summaries.get(21).todaysPoints == 0
        assert summaries.get(21).levelPoints == 140 - (45+36+39)
        assert !addSkillRes.get(21).body.completed.find { it.type == "Overall" }

        assert summaries.get(22).points == 150
        assert summaries.get(22).skillsLevel == 4
        assert summaries.get(22).todaysPoints == 0
        assert summaries.get(22).levelPoints == 150 - (45+36+39)
        assert !addSkillRes.get(22).body.completed.find { it.type == "Overall" }

        assert summaries.get(23).points == 160
        assert summaries.get(23).skillsLevel == 4
        assert summaries.get(23).todaysPoints == 0
        assert summaries.get(23).levelPoints == 160 - (45+36+39)
        assert !addSkillRes.get(23).body.completed.find { it.type == "Overall" }

        assert summaries.get(24).points == 170
        assert summaries.get(24).skillsLevel == 5
        assert summaries.get(24).todaysPoints == 0
        assert summaries.get(24).levelPoints == 170 - (45+36+39+45)
        assert addSkillRes.get(24).body.completed.find { it.type == "Overall" }.level == 5
//        !addSkillRes.get(24).body.completed.find { it.type == "Overall" }

        assert summaries.get(25).points == 180
        assert summaries.get(25).skillsLevel == 5
        assert summaries.get(25).todaysPoints == 0
        assert summaries.get(25).levelPoints == 180 - (45+36+39+45)
        assert summaries.get(25).levelTotalPoints == -1
        assert !addSkillRes.get(25).body.completed.find { it.type == "Overall" }
        return true
    }



    def "if skill is already completed then simply inform the caller"() {
        String subj = "testSubj"
        String skillId = "skillId"

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill([projectId     : projId, subjectId: subj, skillId: skillId, name: "Test Skill", type: "Skill",
                                   pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
        ])
        def res = skillsService.addSkill([projectId: projId, skillId: skillId])
        def res1 = skillsService.addSkill([projectId: projId, skillId: skillId])

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
        res.body.completed.size() == 3

        !res1.body.skillApplied
        res1.body.explanation == "This skill reached its maximum points"
        !res1.body.completed
    }


    def "skills from different projects with the same subject id do not intermingle"() {
        String subj = "testSubj"
        String skillId = "skillId"
        String projId2 = "${SkillsFactory.defaultProjId}2"

        setup:
        skillsService.deleteProjectIfExist(projId2)

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill([projectId     : projId, subjectId: subj, skillId: skillId, name: "Test Skill", type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1
        ])
        def res = skillsService.addSkill([projectId: projId, skillId: skillId])



        skillsService.createProject([projectId: projId2, name: "Test Project2"])
        skillsService.createSubject([projectId: projId2, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill([projectId     : projId2, subjectId: subj, skillId: skillId, name: "Test Skill", type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1
        ])
        def res2 = skillsService.addSkill([projectId: projId2, skillId: skillId])

        def skillsResult1 = skillsService.getSkillsForSubject(projId, subj)
        def skillsResult2 = skillsService.getSkillsForSubject(projId2, subj)

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"

        res.body.completed.size() == 3
        res.body.completed.find({ it.type == "Skill" }).id == "skillId"
        res.body.completed.find({ it.type == "Skill" }).name == "Test Skill"

        res.body.completed.find({ it.type == "Overall" }).id == "OVERALL"
        res.body.completed.find({ it.type == "Overall" }).name == "OVERALL"
        res.body.completed.find({ it.type == "Overall" }).level == 5

        res.body.completed.find({ it.type == "Subject" }).id == "testSubj"
        res.body.completed.find({ it.type == "Subject" }).name == "Test Subject"
        res.body.completed.find({ it.type == "Subject" }).level == 5

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"

        skillsResult1.size() == 1
        skillsResult1.first().projectId == projId && skillsResult1.first().skillId == skillId

        skillsResult2.size() == 1
        skillsResult2.first().projectId == projId2 && skillsResult2.first().skillId == skillId
    }
}
