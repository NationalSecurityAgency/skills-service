package skills.intTests.clientDisplay

import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class SingleSkillSummarySpec extends DefaultIntSpec {

    def "load single skill summary"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(1).skillId)

        then:
        !summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.maxOccurrencesWithinIncrementInterval == proj1_skills.get(1).numMaxOccurrencesIncrementInterval
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 0
        summary.todaysPoints == 0
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
    }

    def "load single skill summary with some users points"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        Date yesterday = use(TimeCategory) { return 1.day.ago }
        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, yesterday)
        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        !summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 20
        summary.todaysPoints == 10
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
        !summary.dependencyInfo
    }


    def "load single skill summary with dependencies"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        when:
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(0).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.projectId == proj1.projectId
        summary.skillId == proj1_skills.get(1).skillId
        summary.dependencyInfo.numDirectDependents == 2
        !summary.dependencyInfo.achieved
    }

    def "load single skill summary with all dependencies achieved"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        when:
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(0).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId, new Date())

        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.projectId == proj1.projectId
        summary.skillId == proj1_skills.get(1).skillId
        summary.dependencyInfo.numDirectDependents == 2
        summary.dependencyInfo.achieved
    }

    def "if [help.url.root] property was set then must be used as a root for help url"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.get(0).helpUrl = "/some/path"
        proj1_skills.get(1).helpUrl = "https://keepMe.com/some/path"

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.changeSetting(proj1.projectId, "help.url.root", [projectId: proj1.projectId, setting: "help.url.root", value: "http://www.root.com/"])

        when:
        def summary = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(0).skillId)
        def summary1 = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.description.href == "http://www.root.com/some/path"
        summary1.description.href == "https://keepMe.com/some/path"
    }



}
