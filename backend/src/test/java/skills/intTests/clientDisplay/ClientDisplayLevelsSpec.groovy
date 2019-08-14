package skills.intTests.clientDisplay

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ClientDisplayLevelsSpec extends DefaultIntSpec {

    def "return total number of levels"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(10, 1, 1)

        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(20, 1, 2)

        def proj1_subj3 = SkillsFactory.createSubject(1, 3)
        List<Map> proj1_subj3_skills = SkillsFactory.createSkills(20, 1, 3)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSubject(proj1_subj3)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSkills(proj1_subj2_skills)
        skillsService.createSkills(proj1_subj3_skills)

        when:
        def overallSummary = skillsService.getSkillSummary("user1", proj1.projectId)
        def overallSummarySubj1 = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj1.subjectId)
        def overallSummarySubj2 = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj2.subjectId)
        def overallSummarySubj3 = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj3.subjectId)

        skillsService.addLevel(proj1.projectId, null, [percent: 98])
        skillsService.addLevel(proj1.projectId, proj1_subj2.subjectId, [percent: 98])

        def overallSummary_res1 = skillsService.getSkillSummary("user1", proj1.projectId)
        def overallSummarySubj1_res1 = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj1.subjectId)
        def overallSummarySubj2_res1 = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj2.subjectId)
        def overallSummarySubj3_res1 = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj3.subjectId)

        then:
        overallSummary.totalLevels == 5
        overallSummarySubj1.totalLevels == 5
        overallSummarySubj2.totalLevels == 5
        overallSummarySubj3.totalLevels == 5

        overallSummary_res1.totalLevels == 6
        overallSummarySubj1_res1.totalLevels == 5
        overallSummarySubj2_res1.totalLevels == 6
        overallSummarySubj3_res1.totalLevels == 5
    }
}
