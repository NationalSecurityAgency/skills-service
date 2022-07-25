package skills.intTests.moveSkills

import org.junit.Ignore
import skills.intTests.utils.DefaultIntSpec
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class MoveSkillsManagementSpec extends DefaultIntSpec {

    def "move skill into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills.size() == 1
        subj2Skills[0].skillId == p1Skills[0].skillId
        subj2Skills[0].name == p1Skills[0].name
        !subj2Skills[0].reusedSkill
        subj2Skills[0].totalPoints == 100

        subj1Stats.numSkills == 2
        subj1Stats.totalPoints == 200
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        subj2Stats.numSkills == 1
        subj2Stats.totalPoints == 100
        subj2Stats.numSkillsReused == 0
        subj2Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
    }

    @IgnoreRest
    def "move skill from group into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills.size() == 1
        subj2Skills[0].skillId == p1Skills[0].skillId
        subj2Skills[0].name == p1Skills[0].name
        !subj2Skills[0].reusedSkill
        subj2Skills[0].totalPoints == 100

        subj1Stats.numSkills == 2
        subj1Stats.totalPoints == 200
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        subj2Stats.numSkills == 1
        subj2Stats.totalPoints == 100
        subj2Stats.numSkillsReused == 0
        subj2Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
    }
}
