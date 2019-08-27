package skills.intTests

import skills.intTests.utils.DefaultIntSpec

class SuggestUsersSpecs extends DefaultIntSpec {

    def "suggest on dashboard users"() {
        createService("FirstUser")
        createService("SecondUser")
        createService("ThirdUser")

        expect:
        skillsService.suggestDashboardUsers(query).collect({ it.userId }).sort() == userIds
        where:
        query  | userIds
        // by user id
        "nd"   | ["seconduser"]
        "ND"   | ["seconduser"]
        "uSer" | ["firstuser", "seconduser", "thirduser"]
        // by last name
        "TesT" | ["firstuser", "seconduser", "thirduser"]
        "esT"  | ["firstuser", "seconduser", "thirduser"]
        // by first name
        "ills" | ["firstuser", "seconduser", "thirduser"]
        // by nickname
        "Skills Test" | ["firstuser", "seconduser", "thirduser"]
    }
}
