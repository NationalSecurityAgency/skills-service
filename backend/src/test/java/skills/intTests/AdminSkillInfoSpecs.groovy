package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class AdminSkillInfoSpecs extends DefaultIntSpec {

    def "get all skills for a project"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        // another project to make sure that code selects the right project
        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(3, 2, 1)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        def skills = skillsService.getSkillsForProject(proj1.projectId)
        then:
        skills.size() == 3

        skills.get(0).skillId == proj1_skills.get(0).skillId
        skills.get(0).projectId == proj1.projectId
        skills.get(0).name == proj1_skills.get(0).name
        skills.get(0).version == proj1_skills.get(0).version
        skills.get(0).displayOrder == 1
        skills.get(0).totalPoints == proj1_skills.get(0).pointIncrement * proj1_skills.get(0).numPerformToCompletion

        skills.get(1).skillId == proj1_skills.get(1).skillId
        skills.get(1).projectId == proj1.projectId
        skills.get(1).name == proj1_skills.get(1).name
        skills.get(1).version == proj1_skills.get(1).version
        skills.get(1).displayOrder == 2
        skills.get(1).totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion

        skills.get(2).skillId == proj1_skills.get(2).skillId
        skills.get(2).projectId == proj1.projectId
        skills.get(2).name == proj1_skills.get(2).name
        skills.get(2).version == proj1_skills.get(2).version
        skills.get(2).displayOrder == 3
        skills.get(2).totalPoints == proj1_skills.get(2).pointIncrement * proj1_skills.get(2).numPerformToCompletion
    }

    def "get all skills for a subject"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        def proj2_subj = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> proj1_skills_subj2 = SkillsFactory.createSkills(3, 1, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj1_skills)
        skillsService.createSkills(proj1_skills_subj2)

        when:
        def skills = skillsService.getSkillsForSubject(proj1.projectId, proj1_subj.subjectId)
        then:
        skills.size() == 3

        skills.get(0).skillId == proj1_skills.get(0).skillId
        skills.get(0).projectId == proj1.projectId
        skills.get(0).name == proj1_skills.get(0).name
        skills.get(0).version == proj1_skills.get(0).version
        skills.get(0).displayOrder == 1
        skills.get(0).totalPoints == proj1_skills.get(0).pointIncrement * proj1_skills.get(0).numPerformToCompletion

        skills.get(1).skillId == proj1_skills.get(1).skillId
        skills.get(1).projectId == proj1.projectId
        skills.get(1).name == proj1_skills.get(1).name
        skills.get(1).version == proj1_skills.get(1).version
        skills.get(1).displayOrder == 2
        skills.get(1).totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion

        skills.get(2).skillId == proj1_skills.get(2).skillId
        skills.get(2).projectId == proj1.projectId
        skills.get(2).name == proj1_skills.get(2).name
        skills.get(2).version == proj1_skills.get(2).version
        skills.get(2).displayOrder == 3
        skills.get(2).totalPoints == proj1_skills.get(2).pointIncrement * proj1_skills.get(2).numPerformToCompletion
    }
}
