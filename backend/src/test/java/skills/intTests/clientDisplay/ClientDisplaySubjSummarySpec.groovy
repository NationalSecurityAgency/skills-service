package skills.intTests.clientDisplay

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ClientDisplaySubjSummarySpec extends DefaultIntSpec {

    def "load subject summary"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        proj1_subj.description = "This is a description"
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId)
        then:
        summary.skills.size() == 3
        summary.skills.each {
            it.maxOccurrencesWithinIncrementInterval == 1
        }
        summary.description == "This is a description"
    }
}
