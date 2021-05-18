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
package skills.intTests.clientDisplay

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.controller.UserInfoController
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.skillLoading.RankingLoader
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAttrsRepo
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest

class ClientDisplayLeaderboardSpecs extends DefaultIntSpec {

    @Autowired
    UserAttrsRepo userAttrsRepo

    def "get top 10"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        12.times {Integer userNum ->
            String userId = users.get(userNum)
            userNum.times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }.reverse()
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId)
        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 10)
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == [110, 100, 90, 80, 70, 60, 50, 40, 30, 20]
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[8] ? it.isItMe : !it.isItMe) }
    }

    def "get top 10 - if less than 10 users then artificially insert the requested user at the bottom"(){
        List<String> users = createUsers(3)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], new Date())

        when:
        def leaderboard = skillsService.getLeaderboard(users[users.size()-1], proj.projectId)
        def leaderboard1 = skillsService.getLeaderboard(users[0], proj.projectId)

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        then:
        leaderboard.rankedUsers.size() == 3
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1], userIdsForDisplay[users.size()-1]]
        leaderboard.rankedUsers.collect{ it.isItMe } == [false, false, true]
        leaderboard.rankedUsers.collect{ it.points } == [20, 10, 0]

        leaderboard1.rankedUsers.size() == 2
        leaderboard1.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard1.rankedUsers.collect{ it.isItMe } == [true, false]
        leaderboard1.rankedUsers.collect{ it.points } == [20, 10]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "user has first and last name"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        createService(users[0], "p@ssw0rd", "Bob", "Smith")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], days.get(0))
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(1), proj.projectId)
        then:
        leaderboard.rankedUsers.size() == 2
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard.rankedUsers.collect{ it.firstName } == ["Bob", null]
        leaderboard.rankedUsers.collect{ it.lastName } == ["Smith", null]
        leaderboard.rankedUsers.collect{ it.rank } ==[1, 2]
        leaderboard.rankedUsers.collect { it.points } == [20, 10]
        leaderboard.rankedUsers.each { assert it.userFirstSeenTimestamp != null }
    }

    def "top 10 is per project"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(22, 2, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], days.get(0))


        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], users[1], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId], users[1], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId], users[2], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(2).skillId], users[2], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(3).skillId], users[2], days.get(0))

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(0), proj.projectId)

        def proj2_leaderboard = skillsService.getLeaderboard(users.get(0), proj2.projectId)
        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 2
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard.rankedUsers.collect{ it.rank } ==[1, 2]
        leaderboard.rankedUsers.collect { it.points } == [20, 10]

        proj2_leaderboard.availablePoints == 220
        proj2_leaderboard.rankedUsers.size() == 3
        proj2_leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[2], userIdsForDisplay[1], userIdsForDisplay[0]]
        proj2_leaderboard.rankedUsers.collect{ it.rank } ==[1, 2, 3]
        proj2_leaderboard.rankedUsers.collect { it.points } == [30, 20, 10]
    }

    def "top 10 is per project - no users"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(22, 2, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        def leaderboard = skillsService.getLeaderboard("newuser", proj.projectId)

        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 0
    }

    def "top 10 is per project - no users for a subject"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(10, 1, 1)

        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(10, 1, 2)
        subj2_skills.each { it.pointIncrement = 100 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], days.get(0))

        when:
        def leaderboard = skillsService.getLeaderboard("newuser", proj.projectId, subj.subjectId)
        def leaderboard2 = skillsService.getLeaderboard("newuser", proj.projectId, subj2.subjectId)

        then:
        leaderboard.availablePoints == 100
        leaderboard.rankedUsers.size() > 0

        leaderboard2.availablePoints == 1000
        leaderboard2.rankedUsers.size() == 0
    }

    def "top 10 is per project - get for a new user with zero points"(){
        List<String> users = createUsers(13)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(22, 2, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], days.get(0))


        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], users[1], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId], users[1], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId], users[2], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(2).skillId], users[2], days.get(0))
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(3).skillId], users[2], days.get(0))


        when:
        def leaderboard = skillsService.getLeaderboard(users[3], proj.projectId)

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 3
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1], userIdsForDisplay[3]]
        leaderboard.rankedUsers.collect{ it.rank } ==[1, 2, 3]
        leaderboard.rankedUsers.collect { it.points } == [20, 10, 0]
    }

    def "top 10 is per subject"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(20, 1, 2)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2_skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], days.get(0))


        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(0).skillId], users[1], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(1).skillId], users[1], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(1).skillId], users[2], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(2).skillId], users[2], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(3).skillId], users[2], days.get(0))

        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId, subj.subjectId)
        def subj2_leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId, subj2.subjectId)

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        then:
        leaderboard.rankedUsers.size() == 3
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1], userIdsForDisplay[3]]
        leaderboard.rankedUsers.collect{ it.rank } ==[1, 2, 3]
        leaderboard.rankedUsers.collect { it.points } == [20, 10, 0]

        subj2_leaderboard.rankedUsers.size() == 4
        subj2_leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[2], userIdsForDisplay[1], userIdsForDisplay[0], userIdsForDisplay[3]]
        subj2_leaderboard.rankedUsers.collect{ it.rank } ==[1, 2, 3, 4]
        subj2_leaderboard.rankedUsers.collect { it.points } == [30, 20, 10, 0]
    }

    def "top 10 is per project - users have same amount of points then sort by created"(){
        int numUsers = 12
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, days.get(0))
            Thread.sleep(100)
        }

        int i = 1;
        List<String> userIdsForDisplay = users.collect {
            String res = userAttrsRepo.findByUserId(it)?.userIdForDisplay
            i++
            return res;
        }

        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId)

        then:
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 10)
        leaderboard.rankedUsers.collect{ it.rank } == (1..10)
        leaderboard.rankedUsers.collect { it.points } == (1..10).collect { 10 }
    }

    def "top 10 is per project - users have same amount of points then sort by created - subject"(){
        int numUsers = 12
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(20, 1, 2)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2_skills)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, days.get(0))
            Thread.sleep(100)
        }

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(0).skillId], users[0], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(0).skillId], users[1], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(1).skillId], users[1], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(1).skillId], users[2], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(2).skillId], users[2], days.get(0))
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(3).skillId], users[2], days.get(0))

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId, subj.subjectId)

        then:
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 10)
        leaderboard.rankedUsers.collect{ it.rank } == (1..10)
        leaderboard.rankedUsers.collect { it.points } == (1..10).collect { 10 }
    }

    def "exception emitted for bad leaderboard type "(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.getLeaderboard(users.get(3), proj.projectId, null , "badType")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.toString().contains("BAD_REQUEST")
    }

    def "get 10 around user - user between 5 through 10 "(){
        int numUsers = 12;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        // sort users by rank
        users = users.reverse()
        int i = 1
        List<String> userIdsForDisplay = users.collect {
            UserAttrs userAttrs = userAttrsRepo.findByUserId(it)
            String userId = userAttrs.userIdForDisplay
            i++
            return userId
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(8), proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 9
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(3, 12)
        leaderboard.rankedUsers.collect{ it.rank } == (4..12).collect { it}
        leaderboard.rankedUsers.collect { it.points } == [90, 80, 70, 60, 50, 40, 30, 20, 10]
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[8] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - user below 10"(){
        int numUsers = 25;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(15), proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 11
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(10, 21)
        leaderboard.rankedUsers.collect{ it.rank } == (11..21).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (15..5).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[15] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - new user with 0 points will be very last user"(){
        int numUsers = 26;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        (0..(numUsers-2)).each { Integer userNum ->
            String userId = users.get(userNum)
            int numSkillsAdded = numUsers-userNum
            numSkillsAdded.times { Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users[25], proj.projectId, null, "tenAroundMe")

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        then:
        leaderboard.rankedUsers.size() == 6
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay.subList(20, 25), userIdsForDisplay[25]].flatten()
        leaderboard.rankedUsers.collect{ it.rank } == (21..26).collect { it}
        leaderboard.rankedUsers.collect { it.points } == [60, 50, 40, 30, 20, 0]
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[25] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - no users"(){
        int numUsers = 25;

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def leaderboard = skillsService.getLeaderboard("newuser", proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 0
    }

    private List<String> createUsers(int num){
        // it's important to remove default user since test rely on timestamps of creation
        // and default user is created at the starts of the tests
        List<String> res = getRandomUsers(num+1).findAll { it != "skills@skills.org"}
        if ( res.size() > num) {
            res = res.subList(0, num)
        }
        return res
    }

    def "get 10 around user - no users for a subject"(){
        int numUsers = 12;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(10, 1, 2)
        subj2_skills.each { it.pointIncrement = 100 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        when:
        def leaderboard = skillsService.getLeaderboard("newuser", proj.projectId, subj.subjectId, "tenAroundMe")
        def leaderboard2 = skillsService.getLeaderboard("newuser", proj.projectId, subj2.subjectId, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() > 0
        leaderboard2.rankedUsers.size() == 0
    }

    def "get 10 around user - user below 10 - everyone has the same points"(){
        int numUsers = 25;
        List<String> users = createUsers(numUsers)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        users.each {String userId ->
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, new Date())
            Thread.sleep(200) // important to produce unique timestamp
        }
        // sort users by rank
        int i = 1;
        List<String> userIdsForDisplay = users.collect {
            UserAttrs userAttrs = userAttrsRepo.findByUserId(it)
            String userId = userAttrs.userIdForDisplay
            i++
            return userId
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(15), proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 11
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(10, 21)
        leaderboard.rankedUsers.collect{ it.rank } == (11..21).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (1..11).collect{ 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[15] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - user below 10 - everyone has the same points - subject"(){
        int numUsers = 25;
        List<String> users = createUsers(numUsers)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        users.each {String userId ->
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], userId, new Date())
            Thread.sleep(200) // important to produce unique timestamp
        }
        // sort users by rank
        int i = 1;
        List<String> userIdsForDisplay = users.collect {
            UserAttrs userAttrs = userAttrsRepo.findByUserId(it)
            String userId = userAttrs.userIdForDisplay
            i++
            return userId
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(15), proj.projectId, subj.subjectId, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 11
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(10, 21)
        leaderboard.rankedUsers.collect{ it.rank } == (11..21).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (1..11).collect{ 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[15] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - user below 10 - everyone has the same points - sort by created then"(){
        int numUsers = 25;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(15), proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 11
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(10, 21)
        leaderboard.rankedUsers.collect{ it.rank } == (11..21).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (15..5).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[15] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - user below 10 - subject"(){
        int numUsers = 25;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(numUsers+2, 1, 2)
        subj2_skills.each { it.pointIncrement = 100 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        (0..numUsers-1).reverse().each {Integer userNum ->
            String userId = users.get(userNum)
            (numUsers - userNum).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }

        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(15), proj.projectId, subj.subjectId, "tenAroundMe")
        def leaderboard1 = skillsService.getLeaderboard(users.get(15), proj.projectId, subj2.subjectId, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 11
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(10, 21)
        leaderboard.rankedUsers.collect{ it.rank } == (11..21).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (15..5).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[15] ? it.isItMe : !it.isItMe) }

        leaderboard1.rankedUsers.size() == 11
        leaderboard1.rankedUsers.collect{ it.userId } == userIdsForDisplay.reverse().subList(4, 15)
        leaderboard1.rankedUsers.each {assert (it.userId == userIdsForDisplay[15] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - last place"(){
        int numUsers = 16;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        int selectedUser = numUsers -1
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(selectedUser), proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 6
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(10, 16)
        leaderboard.rankedUsers.collect{ it.rank } == (11..16).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (6..1).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[selectedUser] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - last place - subject"(){
        int numUsers = 16;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(numUsers+2, 1, 2)
        subj2_skills.each { it.pointIncrement = 100 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        (0..numUsers-1).reverse().each {Integer userNum ->
            String userId = users.get(userNum)
            (numUsers - userNum).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }

        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        int selectedUser = numUsers -1
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(selectedUser), proj.projectId, subj.subjectId, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 6
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(10, 16)
        leaderboard.rankedUsers.collect{ it.rank } == (11..16).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (6..1).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[selectedUser] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - first place should return top 10"(){
        int numUsers = 16;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        int selectedUser = 0
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(selectedUser), proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 10)
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (16..7).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[selectedUser] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - 5th place should return top 10"(){
        int numUsers = 16;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        int selectedUser = 4
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(selectedUser), proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 10)
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (16..7).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[selectedUser] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - 5th place should return top 10 - subject"(){
        int numUsers = 16;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(numUsers+2, 1, 2)
        subj2_skills.each { it.pointIncrement = 100 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        (0..numUsers-1).reverse().each {Integer userNum ->
            String userId = users.get(userNum)
            (numUsers - userNum).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }

        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        int selectedUser = 4
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(selectedUser), proj.projectId, subj.subjectId, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 10)
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (16..7).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[selectedUser] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - 6th place should return 10 around me"(){
        int numUsers = 16;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        int selectedUser = 5
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(selectedUser), proj.projectId, null, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 11
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 11)
        leaderboard.rankedUsers.collect{ it.rank } == (1..11).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (16..6).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[selectedUser] ? it.isItMe : !it.isItMe) }
    }

    def "get 10 around user - 6th place should return 10 around me - subject"(){
        int numUsers = 16;
        List<String> users = createUsers(numUsers)
        List<Date> days = (0..numUsers+1).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(numUsers+2, 1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(numUsers+2, 1, 2)
        subj2_skills.each { it.pointIncrement = 100 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        (0..numUsers-1).each {Integer userNum ->
            String userId = users.get(userNum)
            (userNum + 1).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }
        (0..numUsers-1).reverse().each {Integer userNum ->
            String userId = users.get(userNum)
            (numUsers - userNum).times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }

        // sort users by rank
        users = users.reverse()
        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        int selectedUser = 5
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(selectedUser), proj.projectId, subj.subjectId, "tenAroundMe")
        then:
        leaderboard.rankedUsers.size() == 11
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 11)
        leaderboard.rankedUsers.collect{ it.rank } == (1..11).collect { it}
        leaderboard.rankedUsers.collect { it.points } == (16..6).collect{ it * 10}
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[selectedUser] ? it.isItMe : !it.isItMe) }
    }

    def "availablePoints"(){
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(1, 1, 1)
        skills.each { it.pointIncrement = 100 }

        def subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> subj2_skills = SkillsFactory.createSkills(2, 1, 2)
        subj2_skills.each { it.pointIncrement = 100 }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(3, 2, 1)
        proj2_skills.each { it.pointIncrement = 100 }

        def proj2_subj2 = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_subj2_skills = SkillsFactory.createSkills(4, 2, 2)
        proj2_subj2_skills.each { it.pointIncrement = 100 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)
        skillsService.createSubject(proj2_subj2)
        skillsService.createSkills(proj2_subj2_skills)

        when:
        def leaderboard_proj1 = skillsService.getLeaderboard("newuser", proj.projectId)
        def leaderboard_proj1_subj1 = skillsService.getLeaderboard("newuser", proj.projectId, subj.subjectId)
        def leaderboard_proj1_subj2 = skillsService.getLeaderboard("newuser", proj.projectId, subj2.subjectId)
        def leaderboard_proj2 = skillsService.getLeaderboard("newuser", proj2.projectId)
        def leaderboard_proj2_subj1 = skillsService.getLeaderboard("newuser", proj2.projectId, subj.subjectId)
        def leaderboard_proj2_subj2 = skillsService.getLeaderboard("newuser", proj2.projectId, subj2.subjectId)

        then:
        leaderboard_proj1.availablePoints == 300
        leaderboard_proj1_subj1.availablePoints == 100
        leaderboard_proj1_subj2.availablePoints == 200

        leaderboard_proj2.availablePoints == 700
        leaderboard_proj2_subj1.availablePoints == 300
        leaderboard_proj2_subj2.availablePoints == 400
    }

    def "get top 10 - user opted out"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        12.times {Integer userNum ->
            String userId = users.get(userNum)
            userNum.times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }

        // user opt-out
        createService(users.reverse()[2]).addOrUpdateUserSetting(UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF, 'true')

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }.reverse()

        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId)
        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay.subList(0, 2), userIdsForDisplay.subList(3, 11)].flatten()
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == [110, 100, 80, 70, 60, 50, 40, 30, 20, 10]
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[8] ? it.isItMe : !it.isItMe) }
    }

    def "get top 10 - if less than 10 users and the requester opted-out then do NOT add that user artificially at the bottom"(){
        List<String> users = createUsers(3)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], new Date())

        String userIdWithNoPoints = users[users.size()-1]
        createService(userIdWithNoPoints).addOrUpdateUserSetting(UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF, 'true')

        when:
        def leaderboard = skillsService.getLeaderboard(userIdWithNoPoints, proj.projectId)
        def leaderboard1 = skillsService.getLeaderboard(users[0], proj.projectId)

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        then:
        leaderboard.rankedUsers.size() == 2
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard.rankedUsers.collect{ it.isItMe } == [false, false]
        leaderboard.rankedUsers.collect{ it.points } == [20, 10]

        leaderboard1.rankedUsers.size() == 2
        leaderboard1.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard1.rankedUsers.collect{ it.isItMe } == [true, false]
        leaderboard1.rankedUsers.collect{ it.points } == [20, 10]
    }

    def "get top 10 - user opted out - subject"() {
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        12.times {Integer userNum ->
            String userId = users.get(userNum)
            userNum.times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }

        // user opt-out
        createService(users.reverse()[2]).addOrUpdateUserSetting(UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF, 'true')

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }.reverse()

        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId, subj.subjectId)

        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay.subList(0, 2), userIdsForDisplay.subList(3, 11)].flatten()
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == [110, 100, 80, 70, 60, 50, 40, 30, 20, 10]
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[8] ? it.isItMe : !it.isItMe) }
    }

    def "opt-out flag"() {
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        // user opt-out
        createService(users[2]).addOrUpdateUserSetting(UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF, 'true')

        when:
        def leaderboard = skillsService.getLeaderboard(users.get(2), proj.projectId, subj.subjectId)
        def leaderboard1 = skillsService.getLeaderboard(users.get(3), proj.projectId, subj.subjectId)
        then:
        leaderboard.optedOut
        !leaderboard1.optedOut
    }

    def "opt-out users are not allowed to requested type of 'tenAroundMe'"() {
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        // user opt-out
        createService(users[2]).addOrUpdateUserSetting(UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF, 'true')

        when:
        skillsService.getLeaderboard(users.get(2), proj.projectId, subj.subjectId, "tenAroundMe")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.resBody.contains("Leaderboard type of [tenAroundMe] is not supported for opted-out users")
    }

    def "get top 10 - admins opted out"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        12.times {Integer userNum ->
            String userId = users.get(userNum)
            userNum.times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }

        // user opt-out
        String optOutUser = users.reverse()[2];
        createService(optOutUser)
        skillsService.addProjectAdmin(proj.projectId, optOutUser)
        skillsService.addOrUpdateProjectSetting(proj.projectId, RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, true.toString())

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }.reverse()

        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId)
        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay.subList(0, 2), userIdsForDisplay.subList(3, 11)].flatten()
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == [110, 100, 80, 70, 60, 50, 40, 30, 20, 10]
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[8] ? it.isItMe : !it.isItMe) }
    }

    def "get top 10 - if less than 10 users and the requester opted-out ADMIN then do NOT add that user artificially at the bottom"(){
        List<String> users = createUsers(3)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(1).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], new Date())

        String userIdWithNoPoints = users[users.size()-1]
        createService(userIdWithNoPoints)
        skillsService.addProjectAdmin(proj.projectId, userIdWithNoPoints)
        skillsService.addOrUpdateProjectSetting(proj.projectId, RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, true.toString())

        when:
        def leaderboard = skillsService.getLeaderboard(userIdWithNoPoints, proj.projectId)
        def leaderboard1 = skillsService.getLeaderboard(users[0], proj.projectId)

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        then:
        leaderboard.rankedUsers.size() == 2
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard.rankedUsers.collect{ it.isItMe } == [false, false]
        leaderboard.rankedUsers.collect{ it.points } == [20, 10]

        leaderboard1.rankedUsers.size() == 2
        leaderboard1.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard1.rankedUsers.collect{ it.isItMe } == [true, false]
        leaderboard1.rankedUsers.collect{ it.points } == [20, 10]
    }

    def "get top 10 - admins opted out - subject"(){
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        12.times {Integer userNum ->
            String userId = users.get(userNum)
            userNum.times {Integer skillNum ->
                skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(skillNum).skillId], userId, days.get(userNum))
            }
        }

        // user opt-out
        String optOutUser = users.reverse()[2];
        createService(optOutUser)
        skillsService.addProjectAdmin(proj.projectId, optOutUser)
        skillsService.addOrUpdateProjectSetting(proj.projectId, RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, true.toString())

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }.reverse()

        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId, subj.subjectId)
        then:
        leaderboard.availablePoints == 200
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay.subList(0, 2), userIdsForDisplay.subList(3, 11)].flatten()
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == [110, 100, 80, 70, 60, 50, 40, 30, 20, 10]
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[8] ? it.isItMe : !it.isItMe) }
    }

    def "opt-out flag for admin-based opt-out"() {
        List<String> users = createUsers(12)
        List<Date> days = (0..20).collect { new Date() - it }

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(20, 1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        // user opt-out
//        createService(users[2]).addOrUpdateUserSetting(UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF, 'true')
        String optOutUser = users[2];
        createService(optOutUser)
        skillsService.addProjectAdmin(proj.projectId, optOutUser)
        skillsService.addOrUpdateProjectSetting(proj.projectId, RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, true.toString())

        when:
        def leaderboard = skillsService.getLeaderboard(users.get(2), proj.projectId, subj.subjectId)
        def leaderboard1 = skillsService.getLeaderboard(users.get(3), proj.projectId, subj.subjectId)
        then:
        leaderboard.optedOut
        !leaderboard1.optedOut
    }
}
