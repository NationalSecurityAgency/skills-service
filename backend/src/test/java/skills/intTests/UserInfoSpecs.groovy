package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class UserInfoSpecs extends DefaultIntSpec {

    def "get user info"() {
        String user = "UserInfoSpecsUserA"
        createService(user)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1, 40)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], user, new Date())

        when:
        def userInfo = skillsService.getUserInfoForProject(proj1.projectId, user)
        then:
        userInfo
        userInfo.userId == user.toLowerCase()
        userInfo.userIdForDisplay == user
    }

}
