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
        overallAchievements.collect({it.level}).sort() == [1, 2, 3, 4, 5]

        List<UserAchievement> subjectAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, proj1.projectId, proj1_subj.subjectId)
        subjectAchievements.size() == 5
        subjectAchievements.collect({it.level}).sort() == [1, 2, 3, 4, 5]
    }

    def "when a skill event achieves multiple levels each level achievement must be awarded and persisted - start at level1 and end at level 4"() {
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills[0].pointIncrement = 10
        proj1_skills[1].pointIncrement = 80

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        setup:
        def res = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills[0].skillId], user, new Date())
        assert res
        assert res.body.completed.findAll { it.type == CompletionItem.CompletionItemType.Overall.name()}.collect({it.level}).sort() == [1]
        assert res.body.completed.findAll { it.type == CompletionItem.CompletionItemType.Subject.name()}.collect({it.level}).sort() == [1]

        List<UserAchievement> overallAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, proj1.projectId, null)
        assert overallAchievements.size() == 1
        assert overallAchievements.collect({it.level}).sort() == [1]

        List<UserAchievement> subjectAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, proj1.projectId, proj1_subj.subjectId)
        assert subjectAchievements.size() == 1
        assert subjectAchievements.collect({it.level}).sort() == [1]

        when:
        def res1 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills[1].skillId], user, new Date())

        then:
        res1.body.completed.findAll { it.type == CompletionItem.CompletionItemType.Overall.name()}.collect({it.level}).sort() == [2, 3, 4]
        res1.body.completed.findAll { it.type == CompletionItem.CompletionItemType.Subject.name()}.collect({it.level}).sort() == [2, 3, 4]

        List<UserAchievement> overallAchievements1 = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, proj1.projectId, null)
        overallAchievements1.size() == 4
        overallAchievements1.collect({it.level}).sort() == [1, 2, 3, 4]

        List<UserAchievement> subjectAchievements1 = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, proj1.projectId, proj1_subj.subjectId)
        subjectAchievements1.size() == 4
        subjectAchievements1.collect({it.level}).sort() == [1, 2, 3, 4]
    }
}
