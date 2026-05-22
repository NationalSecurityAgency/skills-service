/**
 * Copyright 2026 SkillTree
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

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired
import skills.services.events.AddSkillHelper
import skills.intTests.utils.*
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAchievedLevelRepo
import spock.lang.IgnoreIf

import java.text.DateFormat

class BatchSkillSubmissionSpecs extends DefaultIntSpec {

    @Autowired
    AddSkillHelper addSkillHelper

    @Autowired(required = false)
    CertificateRegistry certificateRegistry

    @Autowired
    UserAchievedLevelRepo userAchievementRepo

    DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd")
    String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"

    def "Submit a single skill for a single user"() {
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

        List<String> users = getRandomUsersForThisTest(1)
        users.each { createService(it) }

        def skillRequest = [
                userIds: [users[0]],
                skillIds: ['skill1'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 1
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]
        result.results[0].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[0]).userIdForDisplay
    }
    
    def "Submit a batch of skills for a single user"() {
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
        List<String> users = getRandomUsersForThisTest(1)

        def skillRequest = [
                userIds: [users[0]],
                skillIds: ['skill1', 'skill2', 'skill3'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 3
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]
        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == users[0]
        result.results[2].skillId == 'skill3'
        result.results[2].skillApplied
        result.results[2].userId == users[0]
    }

    private List<String> getRandomUsersForThisTest(int num) {
        boolean isPki = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki'
        if (isPki) {
            List<String> copyOfAllUserIds = new ArrayList<>(certificateRegistry.allUserIds.collect { it.toLowerCase()})
            List<String> res = certificateRegistry.allUserIds
                    .collect { it.toLowerCase() }
                    .findAll { String userIdToConsider -> copyOfAllUserIds.count { it.contains(userIdToConsider.substring(0, userIdToConsider.size()-1))} == 1 }
            return res.subList(0, num)
        }

        return getRandomUsers(num)
    }

    def "Submit a single skill for multiple users"() {
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
        List<String> users = getRandomUsersForThisTest(4)
        users.each { createService(it)}

        def skillRequest = [
                userIds: users,
                skillIds: ['skill1'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 4
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]
        result.results[0].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[0]).userIdForDisplay

        result.results[1].skillId == 'skill1'
        result.results[1].skillApplied
        result.results[1].userId == users[1]
        result.results[1].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[1]).userIdForDisplay

        result.results[2].skillId == 'skill1'
        result.results[2].skillApplied
        result.results[2].userId == users[2]
        result.results[2].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[2]).userIdForDisplay

        result.results[3].skillId == 'skill1'
        result.results[3].skillApplied
        result.results[3].userId == users[3]
        result.results[3].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[3]).userIdForDisplay
    }

    def "Submit a batch of skills for multiple users"() {
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
        List<String> users = getRandomUsersForThisTest(4)

        def skillRequest = [
                userIds: users,
                skillIds: proj1_skills[0..1].skillId,
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result.results.size() == 8
        result.results[0].skillId == proj1_skills[0].skillId
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == proj1_skills[1].skillId
        result.results[1].skillApplied
        result.results[1].userId == users[0]

        result.results[2].skillId == proj1_skills[0].skillId
        result.results[2].skillApplied
        result.results[2].userId == users[1]

        result.results[3].skillId == proj1_skills[1].skillId
        result.results[3].skillApplied
        result.results[3].userId == users[1]

        result.results[4].skillId == proj1_skills[0].skillId
        result.results[4].skillApplied
        result.results[4].userId == users[2]

        result.results[5].skillId == proj1_skills[1].skillId
        result.results[5].skillApplied
        result.results[5].userId == users[2]

        result.results[6].skillId == proj1_skills[0].skillId
        result.results[6].skillApplied
        result.results[6].userId == users[3]

        result.results[7].skillId == proj1_skills[1].skillId
        result.results[7].skillApplied
        result.results[7].userId == users[3]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "Submit a batch of skills for multiple users - userIds are looked up via suggest"() {
        List<String> allUserIds = certificateRegistry.allUserIds
        List<String> suggestUserIds = [
                "aafirstsuggestusersspecsuser",
                "aaa@email.foo",
                "bob@email.com",
                "ddd@email.foo"
        ]
        suggestUserIds.each {
            assert allUserIds.contains(it)
        }
        List<String> substringsForSuggestions = suggestUserIds.collect { it.substring(0, it.length()-1)}
        substringsForSuggestions.each { String checkSuggestId ->
            assert !allUserIds.contains(checkSuggestId)
            assert allUserIds.count { it.contains(checkSuggestId) } == 1
        }

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
        List<String> users = getRandomUsersForThisTest(4)

        def skillRequest = [
                userIds: substringsForSuggestions,
                skillIds: proj1_skills[0..1].skillId,
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result.results.size() == 8
        result.results[0].skillId == proj1_skills[0].skillId
        result.results[0].skillApplied
        result.results[0].userId == suggestUserIds[0]

        result.results[1].skillId == proj1_skills[1].skillId
        result.results[1].skillApplied
        result.results[1].userId == suggestUserIds[0]

        result.results[2].skillId == proj1_skills[0].skillId
        result.results[2].skillApplied
        result.results[2].userId == suggestUserIds[1]

        result.results[3].skillId == proj1_skills[1].skillId
        result.results[3].skillApplied
        result.results[3].userId == suggestUserIds[1]

        result.results[4].skillId == proj1_skills[0].skillId
        result.results[4].skillApplied
        result.results[4].userId == suggestUserIds[2]

        result.results[5].skillId == proj1_skills[1].skillId
        result.results[5].skillApplied
        result.results[5].userId == suggestUserIds[2]

        result.results[6].skillId == proj1_skills[0].skillId
        result.results[6].skillApplied
        result.results[6].userId == suggestUserIds[3]

        result.results[7].skillId == proj1_skills[1].skillId
        result.results[7].skillApplied
        result.results[7].userId == suggestUserIds[3]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "Batch submission handles both ambiguous and non-existent users gracefully"() {
        List<String> allUserIds = certificateRegistry.allUserIds
        String userWithMultipleResults = "usera"
        allUserIds.count { it.contains(userWithMultipleResults) } == 2
        String nonExistentUser = "notAvailbleUser"
        List<String> suggestUserIds = [
                "aaa@email.foo",
                "bob@email.com",
        ]
        suggestUserIds.each {
            assert allUserIds.contains(it)
        }
        List<String> substringsForSuggestions = suggestUserIds.collect { it.substring(0, it.length()-1)}
        substringsForSuggestions.each { String checkSuggestId ->
            assert !allUserIds.contains(checkSuggestId)
            assert allUserIds.count { it.contains(checkSuggestId) } == 1
        }

        suggestUserIds.add(1, userWithMultipleResults)
        substringsForSuggestions.add(1, userWithMultipleResults)

        suggestUserIds.add(nonExistentUser)
        substringsForSuggestions.add(nonExistentUser)

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

        def skillRequest = [
                userIds: substringsForSuggestions,
                skillIds: proj1_skills[0..1].skillId,
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result.results.size() == 8
        result.results[0].skillId == proj1_skills[0].skillId
        result.results[0].skillApplied
        result.results[0].userId == suggestUserIds[0]

        result.results[1].skillId == proj1_skills[1].skillId
        result.results[1].skillApplied
        result.results[1].userId == suggestUserIds[0]

        result.results[2].skillId == proj1_skills[0].skillId
        !result.results[2].skillApplied
        result.results[2].userId == suggestUserIds[1]
        result.results[2].explanation == "Ambiguous user ID [${suggestUserIds[1]}]. Found multiple DNs: [\"CN=UserInfoSpecsUserA, OU=integration tests, O=Skilltree Test, C=US\", \"CN=usera, OU=integration tests, O=Skilltree Test, C=US\"]"

        result.results[3].skillId == proj1_skills[1].skillId
        !result.results[3].skillApplied
        result.results[3].userId == suggestUserIds[1]
        result.results[3].explanation == "Ambiguous user ID [${suggestUserIds[1]}]. Found multiple DNs: [\"CN=UserInfoSpecsUserA, OU=integration tests, O=Skilltree Test, C=US\", \"CN=usera, OU=integration tests, O=Skilltree Test, C=US\"]"

        result.results[4].skillId == proj1_skills[0].skillId
        result.results[4].skillApplied
        result.results[4].userId == suggestUserIds[2]

        result.results[5].skillId == proj1_skills[1].skillId
        result.results[5].skillApplied
        result.results[5].userId == suggestUserIds[2]

        result.results[6].skillId == proj1_skills[0].skillId
        !result.results[6].skillApplied
        result.results[6].userId == suggestUserIds[3]
        result.results[6].explanation == "User [" + suggestUserIds[3] + "] was not found"

        result.results[7].skillId == proj1_skills[1].skillId
       !result.results[7].skillApplied
        result.results[7].userId == suggestUserIds[3]
        result.results[7].explanation == "User [" + suggestUserIds[3] + "] was not found"
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "Submit a batch of skills and support suggestOptionParam"() {
        List<String> allUserIds = certificateRegistry.allUserIds
        List<String> suggestUserIds = [
                "aafirstsuggestusersspecsuser",
                "aaa@email.foo",
                "bob@email.com",
                "ddd@email.foo"
        ]
        suggestUserIds.each {
            assert allUserIds.contains(it)
        }
        List<String> substringsForSuggestions = suggestUserIds.collect { it.substring(0, it.length()-1)}
        substringsForSuggestions.each { String checkSuggestId ->
            assert !allUserIds.contains(checkSuggestId)
            assert allUserIds.count { it.contains(checkSuggestId) } == 1
        }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj1, proj1_skills)

        def skillRequest = [
                userIds: substringsForSuggestions,
                skillIds: proj1_skills[0..1].skillId,
                timestamp: 1234l,
                userSuggestOption: "ONLY_ONE_USER_IN_THIS_SET"
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result.results.size() == 8
        result.results[0].skillId == proj1_skills[0].skillId
        result.results[0].skillApplied
        result.results[0].userId == suggestUserIds[0]

        result.results[1].skillId == proj1_skills[1].skillId
        result.results[1].skillApplied
        result.results[1].userId == suggestUserIds[0]

        result.results[2].skillId == proj1_skills[0].skillId
        !result.results[2].skillApplied
        result.results[2].userId == substringsForSuggestions[1]
        result.results[2].explanation == "User [" + substringsForSuggestions[1] + "] was not found"

        result.results[3].skillId == proj1_skills[1].skillId
        !result.results[3].skillApplied
        result.results[3].userId == substringsForSuggestions[1]
        result.results[3].explanation == "User [" + substringsForSuggestions[1] + "] was not found"

        result.results[4].skillId == proj1_skills[0].skillId
        !result.results[4].skillApplied
        result.results[4].userId == substringsForSuggestions[2]
        result.results[4].explanation == "User [" + substringsForSuggestions[2] + "] was not found"

        result.results[5].skillId == proj1_skills[1].skillId
        !result.results[5].skillApplied
        result.results[5].userId == substringsForSuggestions[2]
        result.results[5].explanation == "User [" + substringsForSuggestions[2] + "] was not found"

        result.results[6].skillId == proj1_skills[0].skillId
        !result.results[6].skillApplied
        result.results[6].userId == substringsForSuggestions[3]
        result.results[6].explanation == "User [" + substringsForSuggestions[3] + "] was not found"

        result.results[7].skillId == proj1_skills[1].skillId
        !result.results[7].skillApplied
        result.results[7].userId == substringsForSuggestions[3]
        result.results[7].explanation == "User [" + substringsForSuggestions[3] + "] was not found"
    }

    def "Submit a batch of skills for multiple users with a skill not applied"() {
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

        List<String> users = getRandomUsersForThisTest(4)

        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], users[0], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], users[1], new Date() - 1)
        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], users[1], new Date())

        def skillRequest = [
                userIds: users,
                skillIds: ['skill1', 'skill2'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 8
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == users[0]

        result.results[2].skillId == 'skill1'
        !result.results[2].skillApplied
        result.results[2].explanation == 'This skill reached its maximum points'
        result.results[2].userId == users[1]

        result.results[3].skillId == 'skill2'
        result.results[3].skillApplied
        result.results[3].userId == users[1]

        result.results[4].skillId == 'skill1'
        result.results[4].skillApplied
        result.results[4].userId == users[2]

        result.results[5].skillId == 'skill2'
        result.results[5].skillApplied
        result.results[5].userId == users[2]

        result.results[6].skillId == 'skill1'
        result.results[6].skillApplied
        result.results[6].userId == users[3]

        result.results[7].skillId == 'skill2'
        result.results[7].skillApplied
        result.results[7].userId == users[3]
    }

    def "Submit a batch of skills for multiple users with a bad skill identifier"() {
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
        List<String> users = getRandomUsersForThisTest(4)
        users.each { createService(it) }

        def skillRequest = [
                userIds: users,
                skillIds: ['skill1', 'fakeskill', 'skill2'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result.results.size() == 12
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]
        result.results[0].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[0]).userIdForDisplay

        result.results[1].skillId == 'fakeskill'
        !result.results[1].skillApplied
        result.results[1].userId == users[0]
        result.results[1].userIdForDisplay == users[0]

        result.results[2].skillId == 'skill2'
        result.results[2].skillApplied
        result.results[2].userId == users[0]
        result.results[2].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[0]).userIdForDisplay

        result.results[3].skillId == 'skill1'
        result.results[3].skillApplied
        result.results[3].userId == users[1]
        result.results[3].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[1]).userIdForDisplay

        result.results[4].skillId == 'fakeskill'
        !result.results[4].skillApplied
        result.results[4].userId == users[1]
        result.results[4].userIdForDisplay == users[1]

        result.results[5].skillId == 'skill2'
        result.results[5].skillApplied
        result.results[5].userId == users[1]
        result.results[5].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[1]).userIdForDisplay

        result.results[6].skillId == 'skill1'
        result.results[6].skillApplied
        result.results[6].userId == users[2]
        result.results[6].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[2]).userIdForDisplay

        result.results[7].skillId == 'fakeskill'
        !result.results[7].skillApplied
        result.results[7].userId == users[2]
        result.results[7].userIdForDisplay == users[2]

        result.results[8].skillId == 'skill2'
        result.results[8].skillApplied
        result.results[8].userId == users[2]
        result.results[8].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[2]).userIdForDisplay

        result.results[9].skillId == 'skill1'
        result.results[9].skillApplied
        result.results[9].userId == users[3]
        result.results[9].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[3]).userIdForDisplay

        result.results[10].skillId == 'fakeskill'
        !result.results[10].skillApplied
        result.results[10].userId == users[3]
        result.results[10].userIdForDisplay == users[3]

        result.results[11].skillId == 'skill2'
        result.results[11].skillApplied
        result.results[11].userId == users[3]
        result.results[11].userIdForDisplay == userAttrsRepo.findByUserIdIgnoreCase(users[3]).userIdForDisplay
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "Submit a single skill for a single user by DN"() {
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

        String user = getRandomUsersForThisTest(1)[0]
        SkillsService user1Service = createService(user, "passefeafeaef", "John", "Smith")
        UserAttrs expectedAttrs = userAttrsRepo.findByUserIdIgnoreCase(user)

        def skillRequest = [
                userIds: [expectedAttrs.dn],
                skillIds: ['skill1'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 1
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == user

    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "Submit multiple skills for a single user by DN"() {
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

        String user = getRandomUsersForThisTest(1)[0]
        SkillsService user1Service = createService(user, "passefeafeaef", "John", "Smith")
        UserAttrs expectedAttrs = userAttrsRepo.findByUserIdIgnoreCase(user)

        def skillRequest = [
                userIds: [expectedAttrs.dn],
                skillIds: ['skill1', 'skill2', 'skill3'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 3
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == user

        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == user

        result.results[2].skillId == 'skill3'
        result.results[2].skillApplied
        result.results[2].userId == user
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "Submit a skills for multiple users by DN"() {
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

        List<String> users = getRandomUsersForThisTest(3)
        SkillsService user1Service = createService(users[0], "passefeafeaef", "John", "Smith")
        SkillsService user2Service = createService(users[1], "passefeafeaef", "Dave", "Johnson")
        SkillsService user3Service = createService(users[2], "passefeafeaef", "Bob", "Williams")
        UserAttrs user1Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[0])
        UserAttrs user2Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[1])
        UserAttrs user3Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[2])

        def skillRequest = [
                userIds: [user1Attrs.dn, user2Attrs.dn, user3Attrs.dn],
                skillIds: ['skill1'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 3
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == 'skill1'
        result.results[1].skillApplied
        result.results[1].userId == users[1]

        result.results[2].skillId == 'skill1'
        result.results[2].skillApplied
        result.results[2].userId == users[2]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "Submit multiple skills for multiple users by DN"() {
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

        List<String> users = getRandomUsersForThisTest(3)
        SkillsService user1Service = createService(users[0], "passefeafeaef", "John", "Smith")
        SkillsService user2Service = createService(users[1], "passefeafeaef", "Dave", "Johnson")
        SkillsService user3Service = createService(users[2], "passefeafeaef", "Bob", "Williams")
        UserAttrs user1Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[0])
        UserAttrs user2Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[1])
        UserAttrs user3Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[2])

        def skillRequest = [
                userIds: [user1Attrs.dn, user2Attrs.dn, user3Attrs.dn],
                skillIds: ['skill1', 'skill2', 'skill3'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 9
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == users[0]

        result.results[2].skillId == 'skill3'
        result.results[2].skillApplied
        result.results[2].userId == users[0]

        result.results[3].skillId == 'skill1'
        result.results[3].skillApplied
        result.results[3].userId == users[1]

        result.results[4].skillId == 'skill2'
        result.results[4].skillApplied
        result.results[4].userId == users[1]

        result.results[5].skillId == 'skill3'
        result.results[5].skillApplied
        result.results[5].userId == users[1]

        result.results[6].skillId == 'skill1'
        result.results[6].skillApplied
        result.results[6].userId == users[2]

        result.results[7].skillId == 'skill2'
        result.results[7].skillApplied
        result.results[7].userId == users[2]

        result.results[8].skillId == 'skill3'
        result.results[8].skillApplied
        result.results[8].userId == users[2]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "Submit a skill for multiple users with a combination of DN and IDs"() {
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

        List<String> users = getRandomUsersForThisTest(4)
        SkillsService user1Service = createService(users[0], "passefeafeaef", "John", "Smith")
        SkillsService user2Service = createService(users[1], "passefeafeaef", "Dave", "Johnson")
        SkillsService user3Service = createService(users[2], "passefeafeaef", "Bob", "Williams")
        SkillsService user4Service = createService(users[3], "passefeafeaef", "Tom", "Thompson")
        UserAttrs user1Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[0])
        UserAttrs user2Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[1])
        UserAttrs user3Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[2])
        UserAttrs user4Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[3])

        def skillRequest = [
                userIds: [user1Attrs.dn, user2Attrs.userId, user3Attrs.userId, user4Attrs.dn],
                skillIds: ['skill1'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 4
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == 'skill1'
        result.results[1].skillApplied
        result.results[1].userId == users[1]

        result.results[2].skillId == 'skill1'
        result.results[2].skillApplied
        result.results[2].userId == users[2]

        result.results[3].skillId == 'skill1'
        result.results[3].skillApplied
        result.results[3].userId == users[3]
    }

    def "Submit a batch of skills for multiple users as a root user"() {
        SkillsService rootSkillsService = createRootSkillService()
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
        List<String> users = getRandomUsersForThisTest(4)
        users.each{
            createService(it)
        }

        def skillRequest = [
                userIds: users,
                skillIds: ['skill1', 'skill2'],
                timestamp: 1234l,
        ]

        when:
        def result = rootSkillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result.results.size() == 8
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == users[0]

        result.results[2].skillId == 'skill1'
        result.results[2].skillApplied
        result.results[2].userId == users[1]

        result.results[3].skillId == 'skill2'
        result.results[3].skillApplied
        result.results[3].userId == users[1]

        result.results[4].skillId == 'skill1'
        result.results[4].skillApplied
        result.results[4].userId == users[2]

        result.results[5].skillId == 'skill2'
        result.results[5].skillApplied
        result.results[5].userId == users[2]

        result.results[6].skillId == 'skill1'
        result.results[6].skillApplied
        result.results[6].userId == users[3]

        result.results[7].skillId == 'skill2'
        result.results[7].skillApplied
        result.results[7].userId == users[3]
    }

    def "Submit a single skill for a single user by user ID"() {
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

        String user = getRandomUsersForThisTest(1)[0]
        createService(user, "passefeafeaef", "John", "Smith")
        UserAttrs expectedAttrs = userAttrsRepo.findByUserIdIgnoreCase(user)

        def skillRequest = [
                userIds: [expectedAttrs.userId],
                skillIds: ['skill1'],
                timestamp: 1234l,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 1
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == user

    }

    def "BatchSkillEventRequest is required"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj1, proj1_skills)

        when:
        skillsService.addBatchSkillsForBatchUsers(proj1.projectId, null).body

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Required request body is missing")
    }

    def "BatchSkillEventRequest.userIds is required"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj1, proj1_skills)
        def skillRequest = [
//                userIds: [users[0]],
                skillIds: ['skill1'],
                timestamp: 1234l,
        ]

        when:
        skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("batchSkillEventRequest.userIds must contain at least 1 item")
    }

    def "BatchSkillEventRequest.skillIds is required"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj1, proj1_skills)
        List<String> users = getRandomUsersForThisTest(1)

        def skillRequest = [
                userIds  : users,
//                skillIds : ['skill1'],
                timestamp: 1234l,
        ]

        when:
        skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("batchSkillEventRequest.skillIds must contain at least 1 item")
    }

    def "BatchSkillEventRequest.userIds * BatchSkillEventRequest.skillIds must not exceed max threshold"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj1, proj1_skills)

        List<String> users = getRandomUsersForThisTest(11)
        def skillRequest = [
                userIds  : users,
                skillIds : proj1_subj1.skillId,
                timestamp: 1234l,
        ]

        when:
        skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("batchSkillEventRequest.skillIds must contain at least 1 item")
    }

    def "BatchSkillEventRequest.timestamp is not required"() {
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

        List<String> users = getRandomUsersForThisTest(1)

        def skillRequest = [
                userIds: [users[0]],
                skillIds: ['skill1'],
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result.results.size() == 1
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]
    }

    def "Can add dates in the past"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 200
            it.numPerformToCompletion = 1
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)

        List<String> users = getRandomUsersForThisTest(1)

        def dateToAchieve = new Date() - 5

        def skillRequest = [
                userIds: [users[0]],
                skillIds: ['skill1'],
                timestamp: dateToAchieve,
        ]

        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body
        assert result.results.size() == 1
        assert result.results[0].skillId == 'skill1'
        assert result.results[0].skillApplied
        assert result.results[0].userId == users[0]

        when:
        def skillSummary = userAchievementRepo.findAllByUserAndProjectIds(users[0], [proj1.projectId])

        then:
        skillSummary.find { it.projectId == proj1.projectId && it.skillId == 'skill1' }.achievedOn.format(dateFormat) == dateToAchieve.format(dateFormat)
    }

    def "Can not add dates in the future"() {
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

        List<String> users = getRandomUsersForThisTest(1)

        def skillRequest = [
                userIds: [users[0]],
                skillIds: ['skill1'],
                timestamp: new Date() + 10,
        ]

        when:
        skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Skill Events may not be in the future")
    }

    def "Skills groups will fail to report"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        Map group = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj1, [proj1_skills[0], group])
        proj1_skills[1..2].each{
            skillsService.assignSkillToSkillsGroup(group.skillId, it)
        }
        List<String> users = getRandomUsersForThisTest(1)

        def skillRequest = [
                userIds: users,
                skillIds: [proj1_skills[0].skillId, group.skillId],
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 2
        result.results[0].skillId == proj1_skills[0].skillId
        result.results[0].skillApplied
        result.results[0].userId == users[0]
        result.results[0].explanation == "Skill event was applied"

        result.results[1].skillId == group.skillId
        !result.results[1].skillApplied
        result.results[1].userId == users[0]
        result.results[1].explanation == "Failed to report skill event because skill definition does not exist."
    }

    def "able to batch report skills under a group"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        Map group = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj1, [proj1_skills[0], group])
        proj1_skills[1..2].each{
            skillsService.assignSkillToSkillsGroup(group.skillId, it)
        }
        List<String> users = getRandomUsersForThisTest(1)

        def skillRequest = [
                userIds: users,
                skillIds: [proj1_skills[1].skillId, proj1_skills[2].skillId],
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 2
        result.results[0].skillId == proj1_skills[1].skillId
        result.results[0].skillApplied
        result.results[0].userId == users[0]
        result.results[0].explanation == "Skill event was applied"

        result.results[1].skillId == proj1_skills[2].skillId
        result.results[1].skillApplied
        result.results[1].userId == users[0]
        result.results[1].explanation == "Skill event was applied"
    }

    def "must not be able to report skill events if there is not enough points because group is not enabled"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 33
            it.numPerformToCompletion = 1
        }
        Map group = SkillsFactory.createSkillsGroup(1, 1, 11)
        group.enabled = false
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj1, [proj1_skills[0], group])
        proj1_skills[1..2].each{
            it.enabled = false
            skillsService.assignSkillToSkillsGroup(group.skillId, it)
        }
        List<String> users = getRandomUsersForThisTest(1)

        def skillRequest = [
                userIds: users,
                skillIds: [proj1_skills[0].skillId],
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 1
        result.results[0].skillId == proj1_skills[0].skillId
        !result.results[0].skillApplied
        result.results[0].userId == users[0]
        result.results[0].explanation == "Insufficient project points, skill achievement is disallowed"

    }
}
