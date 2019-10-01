package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class SuggestUsersSpecs extends DefaultIntSpec {

    def "suggest on dashboard users"() {
        // make unique enough lookups to avoid clashing with users created by other tests
        createService("FirstSuggestUsersSpecsUser", "p@ssw0rd", "SuggestUsersSpecsBob", "SuggestUsersSpecsSmith")
        createService("SecondSuggestUsersSpecsUser","p@ssw0rd", "SuggestUsersSpecsJane", "SuggestUsersSpecsDoe")
        createService("ThirdSuggestUsersSpecsUser","p@ssw0rd", "SuggestUsersSpecsJames", "SuggestUsersSpecsHan")

        expect:
        //one of the other tests, when run in a suite, results in extra users not added by this test, thus the change to containsAll
        skillsService.suggestDashboardUsers(query).collect({ it.userId }).sort().containsAll(userIds)
        where:
        query  | userIds
        // by user id
        "ndSuggestUsersSpecs"   | ["secondsuggestusersspecsuser"]
        "NDSuggestUsersSpecs"   | ["secondsuggestusersspecsuser"]
        "SuggestUsersSpecsuSer" | ["firstsuggestusersspecsuser", "secondsuggestusersspecsuser", "thirdsuggestusersspecsuser"]
        // by last name
        "SuggestUsersSpecsSm" | ["firstsuggestusersspecsuser"]
        // by first name
        "SuggestUsersSpecsJane" | ["secondsuggestusersspecsuser"]
        // by nickname
        "SuggestUsersSpecsBob SuggestUsersSpecsSmith" | ["firstsuggestusersspecsuser"]
        "" | ["firstsuggestusersspecsuser", "secondsuggestusersspecsuser", "thirdsuggestusersspecsuser"]
    }

    def "suggest client users for project"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skills.each{
            skillsService.addSkill([projectId:proj.projectId, skillId: it.skillId], "user-${it.skillId}", new Date())
        }

        expect:
        skillsService.suggestClientUsersForProject(proj.projectId, query).collect({ it. userId }).sort() == userIds

        where:
        query | userIds
        "skill5" | ["user-skill5"]
        "" | ["user-skill1", "user-skill10", "user-skill2", "user-skill3", "user-skill4"]
    }
}
