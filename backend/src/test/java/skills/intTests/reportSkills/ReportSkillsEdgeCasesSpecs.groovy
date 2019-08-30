package skills.intTests.reportSkills

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.events.CompletionItem
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo

class ReportSkillsEdgeCasesSpecs extends DefaultIntSpec {

    @Autowired
    UserAchievedLevelRepo achievedRepo

    def "when a skill event achieves multiple levels each level achievement must be awarded and persisted"() {
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)
        proj1_skills[0].pointIncrement = 10000

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def res = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills[0].skillId], user, new Date())

        then:
        res
        res.body.completed.findAll { it.type == CompletionItem.CompletionItemType.Overall.name()}.collect({it.level}).sort() == [1, 2, 3, 4, 5]
        res.body.completed.findAll { it.type == CompletionItem.CompletionItemType.Subject.name()}.collect({it.level}).sort() == [1, 2, 3, 4, 5]

        List<UserAchievement> overallAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, proj1.projectId, null)
        overallAchievements.size() == 5

        List<UserAchievement> subjectAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, proj1.projectId, proj1_subj.subjectId)
        subjectAchievements.size() == 5
    }
}
