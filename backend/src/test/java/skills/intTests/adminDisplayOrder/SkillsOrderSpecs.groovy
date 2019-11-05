package skills.intTests.adminDisplayOrder

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class SkillsOrderSpecs extends DefaultIntSpec {
    def proj
    def subj
    List skills
    def setup(){
        proj = SkillsFactory.createProject()
        subj = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        int numSkills = 5
        skills = (1..numSkills).collect {
            def skill = SkillsFactory.createSkill(1, 1, it)
            skillsService.createSkill(skill)
            return skill
        }
    }

    def "move skill down"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillDown(skills.first())
        def afterMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        afterMove.collect({it.skillId}) == ["skill2", "skill1", "skill3", "skill4", "skill5"]
    }

    def "should not be able to move down the last skill"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillDown(skills.last())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
    }

    def "move skill up"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillUp(skills.get(1))
        def afterMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        afterMove.collect({it.skillId}) == ["skill2", "skill1", "skill3", "skill4", "skill5"]
    }

    def "should not be able to move the first skill up"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillUp(skills.first())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
    }
}
