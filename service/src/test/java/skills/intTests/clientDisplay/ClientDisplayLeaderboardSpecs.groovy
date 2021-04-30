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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.repos.UserAttrsRepo
import spock.lang.IgnoreRest

class ClientDisplayLeaderboardSpecs extends DefaultIntSpec {

    @Autowired
    UserAttrsRepo userAttrsRepo

    def "get top 10"(){
        List<String> users = getRandomUsers(12)
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
        println JsonOutput.prettyPrint(JsonOutput.toJson(leaderboard))
        then:
        leaderboard.totalProjPoints == 200
        leaderboard.rankedUsers.size() == 10
        leaderboard.rankedUsers.collect{ it.userId } == userIdsForDisplay.subList(0, 10)
        leaderboard.rankedUsers.collect{ it.rank } == (1..10).collect { it}
        leaderboard.rankedUsers.collect { it.points } == [110, 100, 90, 80, 70, 60, 50, 40, 30, 20]
        leaderboard.rankedUsers.each { assert it.firstName == null }
        leaderboard.rankedUsers.each { assert it.lastName == null }
        leaderboard.rankedUsers.each {assert (it.userId == userIdsForDisplay[8] ? it.isItMe : !it.isItMe) }
    }

    def "user has first and last name"(){
        List<String> users = getRandomUsers(12)
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
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId)
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
        List<String> users = getRandomUsers(12)
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
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId)

        def proj2_leaderboard = skillsService.getLeaderboard(users.get(3), proj2.projectId)
        then:
        leaderboard.totalProjPoints == 200
        leaderboard.rankedUsers.size() == 2
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard.rankedUsers.collect{ it.rank } ==[1, 2]
        leaderboard.rankedUsers.collect { it.points } == [20, 10]

        proj2_leaderboard.totalProjPoints == 220
        proj2_leaderboard.rankedUsers.size() == 3
        proj2_leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[2], userIdsForDisplay[1], userIdsForDisplay[0]]
        proj2_leaderboard.rankedUsers.collect{ it.rank } ==[1, 2, 3]
        proj2_leaderboard.rankedUsers.collect { it.points } == [30, 20, 10]
    }

    def "top 10 is per subject"(){
        List<String> users = getRandomUsers(12)
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

        List<String> userIdsForDisplay = users.collect {
            userAttrsRepo.findByUserId(it)?.userIdForDisplay
        }
        when:
        def leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId, subj.subjectId)

        def subj2_leaderboard = skillsService.getLeaderboard(users.get(3), proj.projectId, subj2.subjectId)
        then:
        leaderboard.rankedUsers.size() == 2
        leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[0], userIdsForDisplay[1]]
        leaderboard.rankedUsers.collect{ it.rank } ==[1, 2]
        leaderboard.rankedUsers.collect { it.points } == [20, 10]

        subj2_leaderboard.rankedUsers.size() == 3
        subj2_leaderboard.rankedUsers.collect{ it.userId } == [userIdsForDisplay[2], userIdsForDisplay[1], userIdsForDisplay[0]]
        subj2_leaderboard.rankedUsers.collect{ it.rank } ==[1, 2, 3]
        subj2_leaderboard.rankedUsers.collect { it.points } == [30, 20, 10]
    }

    def "exception emitted for bad leaderboard type "(){
        List<String> users = getRandomUsers(12)
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
}
