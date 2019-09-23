package skills.intTests.clientDisplay

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ClientDisplaySpec extends DefaultIntSpec {

    def "summary for an empty subject"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        when:
        def res = skillsService.getSkillSummary("user1", proj1.projectId)
        then:
        res
        res.subjects.size() == 1
        res.subjects.first().subjectId == subj1.subjectId
        res.subjects.first().totalPoints == 0
    }
}
