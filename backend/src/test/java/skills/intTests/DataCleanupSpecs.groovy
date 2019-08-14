package skills.intTests


import skills.intTests.utils.DBHelper
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreIf
import spock.lang.Shared

class DataCleanupSpecs extends DefaultIntSpec {

    @Shared
    DBHelper dbHelper = new DBHelper()

    @IgnoreIf({ !DBChecksUtil.isEnabled() })
    def "make sure there are no orphan levels in db when project is removed"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()

        when:
        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        String sqlCountLevelsForProj = "select count(*) as num from level_definition l, project_definition p where l.project_id = p.id and p.project_id = '${proj.projectId}'".toString()
        int projLevelBefore = dbHelper.firstRow(sqlCountLevelsForProj).num

        String sqlCountLevelsForSubj = "select count(*) as num from level_definition l, skill_definition s where l.skill_id = s.id and  s.project_id = '${proj.projectId}' and s.skill_id = '${subject.subjectId}'".toString()
        int subjLevelsBefore = dbHelper.firstRow(sqlCountLevelsForSubj).num

        skillsService.deleteProject(proj.projectId)

        int projLevelAfter = dbHelper.firstRow(sqlCountLevelsForProj).num
        int subjLevelsAfter = dbHelper.firstRow(sqlCountLevelsForSubj).num

        then:
        projLevelBefore > 0
        subjLevelsBefore > 0

        projLevelAfter == 0
        subjLevelsAfter == 0

        // now make sure there are no orphans where neither project or subj assigned
        dbHelper.firstRow("select count(*) as num from level_definition where skill_id is null and project_id is null").num == 0
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
