package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.repos.LevelDefRepo

class DataCleanupSpecs extends DefaultIntSpec {

    @Autowired
    LevelDefRepo levelDefRepo

    def "make sure there are no orphan levels in db when project is removed"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()

        when:
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        long countBefore = levelDefRepo.count()
        skillsService.deleteProject(proj.projectId)
        long countAfter = levelDefRepo.count()

        then:
        countBefore > 0
        countAfter == 0
    }

    def "when project is removed performed events must be removed as well"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())

        when:
        def skills = skillsService.getPerformedSkills(userId, proj1.projectId)
        skillsService.deleteProject(proj1.projectId)
        skillsService.createProject(proj1)
        def skillsAfter = skillsService.getPerformedSkills(userId, proj1.projectId)
        then:
        skills.data.size() == 2
        !skillsAfter.data
    }

    def "when skill is removed performed events must be removed as well"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())

        when:
        def skills = skillsService.getPerformedSkills(userId, proj1.projectId)
        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(0).skillId])
        def skillsAfter = skillsService.getPerformedSkills(userId, proj1.projectId)
        then:
        skills.data.size() == 2
        skillsAfter.data.size() == 1
    }
}
