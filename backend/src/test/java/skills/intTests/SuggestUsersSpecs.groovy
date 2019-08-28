package skills.intTests

import skills.intTests.utils.DefaultIntSpec

class SuggestUsersSpecs extends DefaultIntSpec {

    def "suggest on dashboard users"() {
        // make unique enough lookups to avoid clashing with users created by other tests
        createService("FirstSuggestUsersSpecsUser", "p@ssw0rd", "SuggestUsersSpecsBob", "SuggestUsersSpecsSmith")
        createService("SecondSuggestUsersSpecsUser","p@ssw0rd", "SuggestUsersSpecsJane", "SuggestUsersSpecsDoe")
        createService("ThirdSuggestUsersSpecsUser","p@ssw0rd", "SuggestUsersSpecsJames", "SuggestUsersSpecsHan")

        expect:
        skillsService.suggestDashboardUsers(query).collect({ it.userId }).sort() == userIds
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
    }
}
