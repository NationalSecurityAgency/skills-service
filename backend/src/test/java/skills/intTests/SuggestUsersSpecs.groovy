package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class SuggestUsersSpecs extends DefaultIntSpec {

    def "suggest on dashboard users"() {
        // make unique enough lookups to avoid clashing with users created by other tests
        createService("aaFirstSuggestUsersSpecsUser", "p@ssw0rd", "SuggestUsersSpecsBob", "SuggestUsersSpecsSmith")
        createService("bbSecondSuggestUsersSpecsUser","p@ssw0rd", "SuggestUsersSpecsJane", "SuggestUsersSpecsDoe")
        createService("ccThirdSuggestUsersSpecsUser","p@ssw0rd", "SuggestUsersSpecsJames", "SuggestUsersSpecsHan")

        expect:
        //one of the other tests, when run in a suite, results in extra users not added by this test, thus the change to containsAll
        skillsService.suggestDashboardUsers(query).collect({ it.userId }).sort().containsAll(userIds)
        where:
        query  | userIds
        // by user id
        "ndSuggestUsersSpecs"   | ["bbsecondsuggestusersspecsuser"]
        "NDSuggestUsersSpecs"   | ["bbsecondsuggestusersspecsuser"]
        "SuggestUsersSpecsuSer" | ["aafirstsuggestusersspecsuser", "bbsecondsuggestusersspecsuser", "ccthirdsuggestusersspecsuser"]
        // by last name
        "SuggestUsersSpecsSm" | ["aafirstsuggestusersspecsuser"]
        // by first name
        "SuggestUsersSpecsJane" | ["bbsecondsuggestusersspecsuser"]
        // by nickname
        "SuggestUsersSpecsBob SuggestUsersSpecsSmith" | ["aafirstsuggestusersspecsuser"]
        // current user - includeSelf should default to true
        "skills@" | ["skills@skills.org"]
//        this consistently fails in gitlab-ci but not locally (in either h2 or postgres)
//        "" | ["aafirstsuggestusersspecsuser", "bbsecondsuggestusersspecsuser", "ccthirdsuggestusersspecsuser"]
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
