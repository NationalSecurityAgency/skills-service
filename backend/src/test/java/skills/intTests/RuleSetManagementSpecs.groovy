package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.intTests.utils.TestUtils
import spock.lang.Specification

class RuleSetManagementSpecs extends DefaultIntSpec {

    TestUtils testUtils = new TestUtils()

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
    }

    def "total points should be a summation of child skills"() {
        String subj = "testSubj"

        Map skill1 = [projectId     : projId, subjectId: subj, skillId: "skill1", name: "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId     : projId, subjectId: subj, skillId: "skill2", name: "Test Skill 2", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId     : projId, subjectId: subj, skillId: "skill3", name: "Test Skill 3", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 3, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        def subject = skillsService.getSubject([projectId: projId, subjectId: subj])

        then:
        subject.totalPoints == 60
    }

    def "verify stats that are produced by subjects"() {
        List<Map> subj1 = (1..3).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: it*10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: it*10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: it*10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        def project = skillsService.createProject([projectId: projId, name: "Test Project"]).body
        assert project.success == true
        project = skillsService.getProject(projId)
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])
        skillsService.createSubject([projectId: projId, subjectId: subj2.first().subjectId, name: "Test Subject 2"])
        skillsService.createSubject([projectId: projId, subjectId: subj3.first().subjectId, name: "Test Subject 3"])

        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each {
            skillsService.createSkill(it)
        }
        subj3.each {
            skillsService.createSkill(it)
        }

        def subjects = skillsService.getSubjects(projId)

        then:
        subjects.size() == 3

        subjects.get(0).numSkills == 3
        subjects.get(0).totalPoints == 60
        subjects.get(0).pointsPercentage == 19

        subjects.get(1).numSkills == 4
        subjects.get(1).totalPoints == 100
        subjects.get(1).pointsPercentage == 32

        subjects.get(2).numSkills == 5
        subjects.get(2).totalPoints == 150
        subjects.get(2).pointsPercentage == 49
    }

    def "project can have badge, subject and skill with the same name"(){
        String sameName = "Same Name"
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = sameName
        def badge = SkillsFactory.createBadge(1, 1)
        badge.name = sameName
        def skill = SkillsFactory.createSkill(1, 1)
        skill.name = sameName
        skillsService.createProject(proj)
        when:
        def subjRes = skillsService.createSubject(subj)
        def skillRes = skillsService.createSkill(skill)
        def badgeRes = skillsService.createBadge(badge)

        def subj1 = skillsService.getSubject([projectId: proj.projectId, subjectId: subj.subjectId])
        def badge1 = skillsService.getBadge([projectId: proj.projectId, badgeId: badge.badgeId])
        def skill1 = skillsService.getSkill([projectId: proj.projectId, subjectId: subj.subjectId, skillId: skill.skillId])
        then:
        subjRes.success
        skillRes.success
        badgeRes.success

        subj1.name == sameName
        badge1.name == sameName
        skill1.name == sameName
    }

    def 'edit skill should cause totalPoints to change'(){
        setup:
        def project = skillsService.createProject(SkillsFactory.createProject(1)).body
        skillsService.createSubject(SkillsFactory.createSubject(1, 1))
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 1, 0))

        when:
        def skillResult = skillsService.getSkill([projectId: SkillsFactory.getDefaultProjId(1), subjectId: SkillsFactory.getSubjectId(1), skillId: "skill1"])
        def totalPointsBeforeEdit = skillResult.totalPoints

        skillsService.createSkill([id: skillResult.id,
                                  projectId: skillResult.projectId,
                                  subjectId: SkillsFactory.getSubjectId(1),
                                  skillId: skillResult.skillId,
                                  numPerformToCompletion: skillResult.numPerformToCompletion*100,
                                  pointIncrement: skillResult.pointIncrement,
                                  pointIncrementInterval: skillResult.pointIncrementInterval,
                                  numMaxOccurrencesIncrementInterval: skillResult.numMaxOccurrencesIncrementInterval,
                                  version: skillResult.version,
                                  name: skillResult.name])

        def updateResult = skillsService.getSkill([projectId: SkillsFactory.getDefaultProjId(1), subjectId: SkillsFactory.getSubjectId(1), skillId: "skill1"])

        then:
        skillResult.name == updateResult.name
        skillResult.pointIncrement == updateResult.pointIncrement
        updateResult.totalPoints == skillResult.totalPoints*100
    }
}
