/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.intTests

import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class UserPointsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds = ['haNson', 'haRry', 'tom']
    List<String> subjects
    List<List<String>> allSkillIds
    String badgeId
    
    Date threeDaysAgo = new Date()-3
    DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZoneUTC()

    def setup(){
        skillsService.deleteProjectIfExist(projId)

        subjects = ['testSubject1', 'testSubject2', 'testSubject3']
        allSkillIds = setupProjectWithSkills(subjects)
        badgeId = 'badge1'

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(0), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(0), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(1), threeDaysAgo)

        skillsService.addBadge([projectId: projId, badgeId: badgeId, name: 'Badge 1'])
        skillsService.assignSkillToBadge([projectId: projId, badgeId: badgeId, skillId: allSkillIds.get(0).get(0)])
    }

    def 'get project users when project exists'() {
        when:
        def results = skillsService.getProjectUsers(projId)

        then:
        results
        results.count == 2
        results.totalCount == 2
        results.data.size() == 2
        results.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results.data.get(0).totalPoints == 70
        results.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results.data.get(1).totalPoints == 35
        results.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)
    }

    def 'get project users with paging'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 1, 1)
        def results2 = skillsService.getProjectUsers(projId, 1, 2)

        then:
        results1
        results1.count == 2
        results1.totalCount == 2
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 70
        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
    }

    def 'get project users with paging and query'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 1, 1, "userId", true, "h")
        def results2 = skillsService.getProjectUsers(projId, 1, 2, "userId", true, "h")

        then:
        results1
        results1.count == 2 // result count
        results1.totalCount == 2  // total user count
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 70
        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
    }

    def 'get subject users when project exists'() {
        when:
        def results1 = skillsService.getSubjectUsers(projId, subjects.get(0))
        def results2 = skillsService.getSubjectUsers(projId, subjects.get(1))
        def results3 = skillsService.getSubjectUsers(projId, subjects.get(2))

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
        results1.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 2
        results2.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
        results2.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(1).totalPoints == 35
        results2.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get skill users when project exists'() {
        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0))
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(0))
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(1))

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
        results2.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 2
        results2.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
        results2.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(1).totalPoints == 35
        results2.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get skill users with paging when project exists'() {
        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 1, 1, "userId", true, "h")
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(0), 1, 1, "userId", true, "h")
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(1), 1, 1, "userId", true, "h")

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results2.data.get(0).totalPoints == 35

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get badge users when project exists'() {
        when:
        def results1 = skillsService.getBadgeUsers(projId, badgeId)

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
    }

    def "user updated date is updated when a skill is achieved"() {

        //testSubject1, testSubject2
        final uid = sampleUserIds.get(0).toLowerCase()

        when:
        def users = skillsService.getProjectUsers(projId, 100).data

        def userBeforeSkillAdd = users.find() {it.userId.contains(uid)}
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        def res = skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(1)], uid, new DateTime().toDate())
        assert res.body.skillApplied

        def users2 = skillsService.getProjectUsers(projId, 100).data

        def userAfterSkillAdd = users2.find() {it.userId.contains(uid)}


        then:
        formatter.parseDateTime(userBeforeSkillAdd.lastUpdated).isBefore(formatter.parseDateTime(userAfterSkillAdd.lastUpdated))

    }

   def "filter users by name"(){

        SkillsService createAcctService = createService()
        createAcctService.createUser([firstName: "John", lastName: "Doe", email: "jdoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Jane", lastName: "Doe", email: "jadoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Foo", lastName: "Bar", email: "fbar@email.foo", password: "password"])

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "jdoe@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "jadoe@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "fbar@email.foo", threeDaysAgo)

        when:

        def control = skillsService.getProjectUsers(projId)
        def result1 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "Jane")
        def result2 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "jadoe")
        def result3 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "Doe")

        then:

        control.data.size() == 5

        result1.data.size() == 1
        result1.data.find{it.userId.contains('jadoe@email.foo')}

        result2.data.size() == 1
        result2.data.find{it.userId.contains('jadoe@email.foo')}

        result3.data.size() == 2
        result3.data.find{it.userId.contains('jadoe@email.foo')}
        result3.data.find{it.userId.contains('jdoe@email.foo')}
    }

    def "sort users" () {
        SkillsService createAcctService = createService()
        createAcctService.createUser([firstName: "John", lastName: "Doe", email: "jdoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Jane", lastName: "Doe", email: "jadoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Foo", lastName: "Bar", email: "fbar@email.foo", password: "password"])

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "jdoe@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "jadoe@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "fbar@email.foo", threeDaysAgo)

        when:
        def allUsers = skillsService.getProjectUsers(projId)
        def fooUsers = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "foo")
        def fooUsersDesc = skillsService.getProjectUsers(projId, 10, 1, "userId", false, "foo")
        def fooUsersSortByFirstName = skillsService.getProjectUsers(projId, 10, 1, "firstName", true, "foo")
        def fooUsersSortByLastName = skillsService.getProjectUsers(projId, 10, 1, "lastName", true, "foo")

        then:
        allUsers.data.size() == 5
        allUsers.data[0].userId.contains('fbar@email.foo')
        allUsers.data[1].userId.contains('hanson')
        allUsers.data[2].userId.contains('harry')
        allUsers.data[3].userId.contains('jadoe@email.foo')
        allUsers.data[4].userId.contains('jdoe@email.foo')

        fooUsers.data.size() == 3
        fooUsers.data[0].userId.contains('fbar@email.foo')
        fooUsers.data[1].userId.contains('jadoe@email.foo')
        fooUsers.data[2].userId.contains('jdoe@email.foo')

        fooUsersDesc.data.size() == 3
        fooUsersDesc.data[2].userId.contains('fbar@email.foo')
        fooUsersDesc.data[1].userId.contains('jadoe@email.foo')
        fooUsersDesc.data[0].userId.contains('jdoe@email.foo')

        fooUsersSortByFirstName.data.size() == 3
        fooUsersSortByFirstName.data[0].userId.contains('fbar@email.foo')
        fooUsersSortByFirstName.data[1].userId.contains('jadoe@email.foo')
        fooUsersSortByFirstName.data[2].userId.contains('jdoe@email.foo')

        fooUsersSortByLastName.data.size() == 3
        fooUsersSortByLastName.data[0].lastName == 'Bar'
        fooUsersSortByLastName.data[1].lastName == 'Doe'
        fooUsersSortByLastName.data[2].lastName == 'Doe'
    }

    def "user paging" () {
        SkillsService createAcctService = createService()
        createAcctService.createUser([firstName: "Aaa", lastName: "Aaa", email: "aaa@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Bbb", lastName: "Bbb", email: "bbb@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Ccc", lastName: "Ccc", email: "ccc@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Ddd", lastName: "Ddd", email: "ddd@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Eee", lastName: "Eee", email: "eee@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Fff", lastName: "Fff", email: "fff@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Ggg", lastName: "Ggg", email: "ggg@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Hhh", lastName: "Hhh", email: "hhh@email.foo", password: "password"])

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "aaa@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "bbb@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "ccc@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "ddd@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "eee@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "fff@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "ggg@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "hhh@email.foo", threeDaysAgo)

        when:
        def firstPage = skillsService.getProjectUsers(projId, 5, 1, "userId", true, "foo")
        def secondPage = skillsService.getProjectUsers(projId, 5, 2, "userId", true, "foo")

        then:
        firstPage.data.size() == 5
        firstPage.data[0].userId.contains('aaa@email.foo')
        firstPage.data[1].userId.contains('bbb@email.foo')
        firstPage.data[2].userId.contains('ccc@email.foo')
        firstPage.data[3].userId.contains('ddd@email.foo')
        firstPage.data[4].userId.contains('eee@email.foo')

        secondPage.data.size() == 3
        secondPage.data[0].userId.contains('fff@email.foo')
        secondPage.data[1].userId.contains('ggg@email.foo')
        secondPage.data[2].userId.contains('hhh@email.foo')
    }



    private List<List<String>> setupProjectWithSkills(List<String> subjects = ['testSubject1', 'testSubject2'], String projectId=projId, name="Test Project") {
        List<List<String>> skillIds = []
        skillsService.createProject([projectId: projectId, name: name])
        subjects.eachWithIndex { String subject, int index ->
            skillsService.createSubject([projectId: projectId, subjectId: subject, name: "Test Subject $index".toString()])
            skillIds << addDependentSkills(projectId,  subject, 3)
        }
        return skillIds
    }

    private List<String> addDependentSkills(String projectId, String subject, int dependencyLevels = 1, int skillsAtEachLevel = 1) {
        List<String> parentSkillIds = []
        List<String> allSkillIds = []

        for (int i = 0; i < dependencyLevels; i++) {
            parentSkillIds = addSkillsForSubject(projectId, subject, skillsAtEachLevel, parentSkillIds)
            allSkillIds.addAll(parentSkillIds)
        }
        return allSkillIds
    }

    private List<String> addSkillsForSubject(String projectId, String subject, int numSkills = 1, List<String> dependentSkillIds = Collections.emptyList()) {
        List<String> skillIds = []
        for (int i = 0; i < numSkills; i++) {
            String skillId = 'skill' + RandomStringUtils.randomAlphabetic(5)
            skillsService.createSkill(
                    [
                            projectId: projectId,
                            subjectId: subject,
                            skillId: skillId,
                            name: 'Test Skill ' + RandomStringUtils.randomAlphabetic(8),
                            pointIncrement: 35,
                            numPerformToCompletion: 1,
                            pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                            dependenctSkillsIds: dependentSkillIds
                    ]
            )
            skillIds << skillId
        }
        return skillIds
    }


    def 'get project users respects project id for lastUpdatedDate'() {
        when:
        // setup a second project
        String projId2 = 'proj2'
        skillsService.deleteProjectIfExist(projId2)

        List<List<String>> proj2SkillIds = setupProjectWithSkills(['testSubject1', 'testSubject2', 'testSubject3'], projId2, 'Test Project 2')

        def results = skillsService.getProjectUsers(projId)
        String mostRecentDate1 = results.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated

        // report a skill for project2
        skillsService.addSkill(['projectId': projId2, skillId: proj2SkillIds.get(0).get(0)], sampleUserIds.get(0), new Date())

        // results two show not be affected
        def results2 = skillsService.getProjectUsers(projId)
        String mostRecentDate2 = results2.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated

        // now report another skill for project1
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(2), new Date())
        def results3 = skillsService.getProjectUsers(projId)
        String mostRecentDate3 = results3.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated

        then:
        results
        results.count == 2
        results.totalCount == 2
        results.data.size() == 2

        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 2

        results3.count == 3
        results3.totalCount == 3
        results3.data.size() == 3

        mostRecentDate1 == mostRecentDate2
        mostRecentDate3 > mostRecentDate2
    }

}
