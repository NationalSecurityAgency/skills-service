package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

/**
 * Make sure systems behaves well when ruleset definitions don't contain a lot of points
 */
class TinyAmountOfPointsSpecs extends DefaultIntSpec {

    def "if there are not enough to properly calculate levels then skills events cannot be added"() {
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: "skill1".toString(),
                                   name     : "Test Skill 1".toString(),
                                   type     : "Skill", pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                                   version  : 0])

        when:
        def res1 = skillsService.addSkill([projectId: proj1.projectId, skillId: "skill1"])
        println res1

        then:
        thrown(SkillsClientException)
    }

    def "user level should be zero if project has insufficient points"(){
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: "skill1".toString(),
                                   name     : "Test Skill 1".toString(),
                                   type     : "Skill", pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                                   version  : 0])
        skillsService.createSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: "skill2".toString(),
                                   name     : "Test Skill 2".toString(),
                                   type     : "Skill", pointIncrement: 120, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                                   version  : 0])

        skillsService.addSkill([projectId: proj1.projectId, skillId: "skill1"])

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: "skill2"])
        def userLevel = skillsService.getSkillSummary(user, proj1.projectId).skillsLevel

        when:
        def res1 = skillsService.addSkill([projectId: proj1.projectId, skillId: "skill1"])

        then:
        userLevel == 0
    }

    def "skills may not be achieved if subject has insufficient points (even if project does)"(){
        when:
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        def proj1_subj1_skills = SkillsFactory.createSkills(25, 1, 1, )
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        proj1_subj1_skills.each{
            skillsService.createSkill(it)
        }
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkill([projectId: proj1.projectId, subjectId: proj1_subj2.subjectId, skillId: "skill111".toString(),
                                   name     : "Test Skill 11111111111111".toString(),
                                   type     : "Skill", pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                                   version  : 0])

        def result = skillsService.addSkill([projectId:proj1.projectId, skillId:"skill111"])

        then:
        thrown(SkillsClientException)
    }
}
