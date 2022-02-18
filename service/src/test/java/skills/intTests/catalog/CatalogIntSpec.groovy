package skills.intTests.catalog


import skills.intTests.utils.DefaultIntSpec

import static skills.intTests.utils.SkillsFactory.*

class CatalogIntSpec extends DefaultIntSpec {

    protected createProjWithCatalogSkills ( Integer projNum ) {
        def proj = createProject(projNum)
        def subj1 = createSubject(projNum, 1)
        def subj2 = createSubject(projNum, 2)
        def subj3 = createSubject(projNum, 3)
        def subj1_skills = (1..3).collect {createSkill(projNum, 1, projNum * 10 + it, 0,2, 480, 100) }
        def subj2_skills = (1..3).collect {createSkill(projNum, 2, projNum * 10 + it + 3, 0, 2, 480, 100) }
        def subj3_skills = (1..3).collect {createSkill(projNum, 3, projNum * 10 + it + 6, 0, 2, 480, 100) }
        skillsService.createProject(proj)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        List skills = [subj1_skills, subj2_skills, subj3_skills].flatten()
        skillsService.createSkills(skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }
        return [
                p : proj,
                s1: subj1,
                s2: subj2,
                s1_skills: subj1_skills,
                s2_skills: subj2_skills,
                s3_skills: subj3_skills,
        ]
    }
}
