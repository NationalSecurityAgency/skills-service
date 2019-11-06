package skills.intTests.clientDisplay

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class UserLevelSpecs extends DefaultIntSpec {

    def "get user level when there are not levels achieved"() {
        when:
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        then:
        skillsService.getUserLevel(proj1.projectId, userId) == 0
    }

    def "get user level when there are achievements"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)


        when:
        List<Integer> levels = []
        (0..9).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], userId, new Date())
            levels.add(skillsService.getUserLevel(proj1.projectId, userId))
        }

        then:
        levels == [1, 1, 2, 2, 3, 3, 4, 4, 4, 5]
    }
}
