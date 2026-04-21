package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import skills.controller.AddSkillHelper
import skills.controller.request.model.BatchSkillEventRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class BatchSkillSubmissionSpecs extends DefaultIntSpec {

    @Autowired
    AddSkillHelper addSkillHelper

    def "Submit a batch of skills for a single user"() {
        String userId = "user1"
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], 'user1', new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], 'user2', new Date() - 1)
        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], 'user2', new Date())

        when:
        def result = addSkillHelper.addBatchSkillsForBatchUsers(proj1.projectId, new BatchSkillEventRequest(
                userIds: ['user1', 'user2', 'user3', 'user4'],
                skillIds: ['skill1', 'skill2'],
                timestamp: 1234l,
                notifyIfSkillNotApplied: true,
                isRetry: false,
        ))

        then:
        result
        result.results.size() == 8
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == 'user1'

        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == 'user1'

        result.results[2].skillId == 'skill1'
        !result.results[2].skillApplied
        result.results[2].explanation == 'This skill reached its maximum points'
        result.results[2].userId == 'user2'

        result.results[3].skillId == 'skill2'
        result.results[3].skillApplied
        result.results[3].userId == 'user2'

        result.results[4].skillId == 'skill1'
        result.results[4].skillApplied
        result.results[4].userId == 'user3'

        result.results[5].skillId == 'skill2'
        result.results[5].skillApplied
        result.results[5].userId == 'user3'

        result.results[6].skillId == 'skill1'
        result.results[6].skillApplied
        result.results[6].userId == 'user4'

        result.results[7].skillId == 'skill2'
        result.results[7].skillApplied
        result.results[7].userId == 'user4'
    }
}
