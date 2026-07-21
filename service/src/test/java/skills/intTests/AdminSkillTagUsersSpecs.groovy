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

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Pair
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class AdminSkillTagUsersSpecs extends DefaultIntSpec {

    def "users with various achievements"() {
        String tagValue = "New Tag"
        String tagId = 'newtag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(8, 1, 1, 10, 2)
        proj1Subj1Skills.each {
            it.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills[0..3])
        def proj1Subj1Group = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createSkill(proj1Subj1Group)
        proj1Subj1Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj1Subj1Group.skillId, it)
        }
        def proj1Subj2 = SkillsFactory.createSubject(1, 2)
        def proj1Subj2Skills = SkillsFactory.createSkills(8, 1, 2, 5, 3)
        proj1Subj2Skills.each {
            it.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(null, proj1Subj2, proj1Subj2Skills[0..3])
        def proj1Subj2Group = SkillsFactory.createSkillsGroup(1, 2, 12)
        skillsService.createSkill(proj1Subj2Group)
        proj1Subj2Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj1Subj2Group.skillId, it)
        }

        skillsService.addTagToSkills(proj1.projectId, [
                proj1Subj1Skills[0].skillId, // 10 * 2
                proj1Subj1Skills[1].skillId, // 10 * 2
                proj1Subj1Skills[2].skillId, // 10 * 2
                proj1Subj1Skills[7].skillId, // 10 * 2
                // 80pts
                proj1Subj2Skills[0].skillId, // 5 * 3
                proj1Subj2Skills[1].skillId, // 5 * 3
                proj1Subj2Skills[2].skillId, // 5 * 3
                proj1Subj2Skills[6].skillId, // 5 * 3
                proj1Subj2Skills[7].skillId, // 5 * 3
                // 75 pts
                // total = 155pts
        ], tagValue, tagId)

        def proj2 = SkillsFactory.createProject(2)
        def proj2Subj1 = SkillsFactory.createSubject(2, 1)
        def proj2Subj1Skills = SkillsFactory.createSkills(8, 2, 1, 25, 4)
        proj2Subj1Skills.each {
            it.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2Subj1, proj2Subj1Skills[0..3])
        def proj2Subj1Group = SkillsFactory.createSkillsGroup(2, 1, 11)
        skillsService.createSkill(proj2Subj1Group)
        proj2Subj1Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj2Subj1Group.skillId, it)
        }
        def proj2Subj2 = SkillsFactory.createSubject(2, 2)
        def proj2Subj2Skills = SkillsFactory.createSkills(8, 2, 2, 15, 3)
        proj2Subj2Skills.each {
            it.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(null, proj2Subj2, proj2Subj2Skills[0..3])
        def proj2Subj2Group = SkillsFactory.createSkillsGroup(2, 2, 12)
        skillsService.createSkill(proj2Subj2Group)
        proj2Subj2Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj2Subj2Group.skillId, it)
        }
        skillsService.addTagToSkills(proj2.projectId, [
                proj2Subj1Skills[0].skillId,
                proj2Subj1Skills[4].skillId,
                proj2Subj2Skills[0].skillId,
                proj2Subj2Skills[4].skillId
        ], tagValue, tagId)

        List<SkillsService> users = getRandomUsers(5).collect { createService(it)}
        // Proj1 --------------------
        // user 1 achieved everything
        proj1Subj1Skills.each {Map skill -> 2.times {users[0].addSkill(skill) }}
        proj1Subj2Skills.each { Map skill -> 3.times { users[0].addSkill(skill) } }
        // user 2 some achievement
        proj1Subj1Skills.each {Map skill -> users[1].addSkill(skill) }
        proj1Subj2Skills.each { Map skill -> 2.times {users[1].addSkill(skill) }}
        // user 3 less achievements
        users[2].addSkill(proj1Subj1Skills[0])
        users[2].addSkill(proj1Subj1Skills[7])
        users[2].addSkill(proj1Subj2Skills[1])
        users[2].addSkill(proj1Subj2Skills[6])
        // user 4 achieved in non-tagged skills
        users[3].addSkill(proj1Subj1Skills[3])
        users[3].addSkill(proj1Subj2Skills[3])

        // Proj2 --------------------
        // user 1 achieved something
        users[0].addSkill(proj2Subj1Skills[1])
        users[0].addSkill(proj2Subj1Skills[4])
        users[0].addSkill(proj2Subj2Skills[2])
        users[0].addSkill(proj2Subj2Skills[4])

        // user 2 achieved everything
        proj2Subj1Skills.each {Map skill -> 4.times {users[1].addSkill(skill) }}
        proj2Subj2Skills.each { Map skill -> 3.times { users[1].addSkill(skill) } }

        // user 5 achieved some stuff too
        // user 1 achieved something
        users[4].addSkill(proj2Subj1Skills[2])
        users[4].addSkill(proj2Subj1Skills[4])
        users[4].addSkill(proj2Subj1Skills[4])
        users[4].addSkill(proj2Subj2Skills[3])
        users[4].addSkill(proj2Subj2Skills[4])

        when:
        def p1Res = skillsService.getSkillTagUsers(proj1.projectId, tagId, 10, 1, 'firstUpdated', false)
        def p2Res = skillsService.getSkillTagUsers(proj2.projectId, tagId, 10, 1, 'firstUpdated', false)
        then:
        p1Res.count == 3
        p1Res.totalPoints == 155
        def p1Data = p1Res.data
        p1Data[0].totalPoints == 30
        p1Data[0].userId == users[2].userName
        p1Data[1].totalPoints == 90
        p1Data[1].userId == users[1].userName
        p1Data[2].totalPoints == 155
        p1Data[2].userId == users[0].userName

        p2Res.count == 3
        p2Res.totalPoints == 290
        def p2Data = p2Res.data
        p2Data[0].totalPoints == 65
        p2Data[0].userId == users[4].userName
        p2Data[1].totalPoints == 290
        p2Data[1].userId == users[1].userName
        p2Data[2].totalPoints == 40
        p2Data[2].userId == users[0].userName
    }

    def "sort and page by user"() {
        String tagValue = "New Tag"
        String tagId = 'newtag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(2, 1, 1, 100, 10)
        proj1Subj1Skills.each {it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills)

        skillsService.addTagToSkills(proj1.projectId, [
                proj1Subj1Skills[0].skillId,
                proj1Subj1Skills[1].skillId,
        ], tagValue, tagId)

        List<SkillsService> users = getRandomUsers(10).collect { createService(it)}

        List<Pair> userPoints = []
        users.eachWithIndex { SkillsService user, int index ->
            int earnedPoints = 0
            (index + 1).times {
                user.addSkill(proj1Subj1Skills[0])
                earnedPoints += proj1Subj1Skills[0].pointIncrement
            }
            String userIdForDisplay = userAttrsRepo.findByUserIdIgnoreCase(user.userName).userIdForDisplay
            userPoints.add(Pair.of(userIdForDisplay, earnedPoints))
        }
        userPoints = userPoints.sort { it.left.toString() }
        List<Pair> userPointsReversed = userPoints.reverse(false)

        when:
        def page1Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 1, 'userIdForDisplay', true)
        def page2Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 2, 'userIdForDisplay', true)
        def page3Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 3, 'userIdForDisplay', true)

        def page1Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 1, 'userIdForDisplay', false)
        def page2Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 2, 'userIdForDisplay', false)
        def page3Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 3, 'userIdForDisplay', false)
        then:
        page1Asc.count == 10
        page1Asc.totalPoints == 2000
        page1Asc.data.userIdForDisplay == userPoints[0..3].collect { it.left.toString() }
        page2Asc.data.userIdForDisplay == userPoints[4..7].collect { it.left.toString() }
        page3Asc.data.userIdForDisplay == userPoints[8..9].collect { it.left.toString() }

        page1Desc.data.userIdForDisplay == userPointsReversed[0..3].collect { it.left.toString() }
        page2Desc.data.userIdForDisplay == userPointsReversed[4..7].collect { it.left.toString() }
        page3Desc.data.userIdForDisplay == userPointsReversed[8..9].collect { it.left.toString() }
    }

    def "sort and page by totalPoints"() {
        String tagValue = "New Tag"
        String tagId = 'newtag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(2, 1, 1, 100, 10)
        proj1Subj1Skills.each {it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills)

        skillsService.addTagToSkills(proj1.projectId, [
                proj1Subj1Skills[0].skillId,
                proj1Subj1Skills[1].skillId,
        ], tagValue, tagId)

        List<SkillsService> users = getRandomUsers(10).collect { createService(it)}

        List<Pair> userPoints = []
        users.eachWithIndex { SkillsService user, int index ->
            int earnedPoints = 0
            (index + 1).times {
                user.addSkill(proj1Subj1Skills[0])
                earnedPoints += proj1Subj1Skills[0].pointIncrement
            }
            userPoints.add(Pair.of(user.userName, earnedPoints))
        }
        userPoints = userPoints.sort { it.right }
        List<Pair> userPointsReversed = userPoints.reverse(false)

        when:
        def page1Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 1, 'totalPoints', true)
        def page2Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 2, 'totalPoints', true)
        def page3Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 3, 'totalPoints', true)

        def page1Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 1, 'totalPoints', false)
        def page2Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 2, 'totalPoints', false)
        def page3Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 3, 'totalPoints', false)
        then:
        page1Asc.count == 10
        page1Asc.totalPoints == 2000
        page1Asc.data.totalPoints == userPoints[0..3].collect { it.right }
        page2Asc.data.totalPoints == userPoints[4..7].collect { it.right }
        page3Asc.data.totalPoints == userPoints[8..9].collect { it.right }

        page1Desc.data.totalPoints == userPointsReversed[0..3].collect { it.right }
        page2Desc.data.totalPoints == userPointsReversed[4..7].collect { it.right }
        page3Desc.data.totalPoints == userPointsReversed[8..9].collect { it.right }
    }

    def "sort and page by lastUpdated"() {
        String tagValue = "New Tag"
        String tagId = 'newtag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(2, 1, 1, 100, 10)
        proj1Subj1Skills.each {it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills)

        skillsService.addTagToSkills(proj1.projectId, [
                proj1Subj1Skills[0].skillId,
                proj1Subj1Skills[1].skillId,
        ], tagValue, tagId)

        List<SkillsService> users = getRandomUsers(10).collect { createService(it)}

        List<String> userOrder = []
        users.eachWithIndex { SkillsService user, int index ->
            user.addSkill(proj1Subj1Skills[0])
            userOrder.add(user.userName)
            // ensure differing timestamps
            Thread.sleep(5)
        }

        List<String> userOrderReversed = userOrder.reverse(false)

        when:
        def page1Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 1, 'lastUpdated', true)
        def page2Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 2, 'lastUpdated', true)
        def page3Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 3, 'lastUpdated', true)

        def page1Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 1, 'lastUpdated', false)
        def page2Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 2, 'lastUpdated', false)
        def page3Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 3, 'lastUpdated', false)
        then:
        page1Asc.count == 10
        page1Asc.data.userId == userOrder[0..3]
        page2Asc.data.userId == userOrder[4..7]
        page3Asc.data.userId == userOrder[8..9]

        page1Desc.data.userId == userOrderReversed[0..3]
        page2Desc.data.userId == userOrderReversed[4..7]
        page3Desc.data.userId == userOrderReversed[8..9]
    }

    def "sort and page by userTag"() {
        String tagValue = "New Tag"
        String tagId = 'newTag'
        String userTagKey = 'dutyOrganization'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(1, 1, 1, 100, 10)
        proj1Subj1Skills.each {it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills)

        skillsService.addTagToSkills(proj1.projectId, [proj1Subj1Skills[0].skillId], tagValue, tagId)

        List<SkillsService> users = getRandomUsers(10).collect { createService(it)}

        SkillsService rootUser = createRootSkillService()

        List<String> userTagOrder = []
        // assign each user a unique userTag so ordering is deterministic
        users.eachWithIndex { SkillsService user, int index ->
            String userTag = StringUtils.leftPad(index.toString(), 3, "0")
            userTagOrder.add(userTag)
            rootUser.saveUserTag(user.userName, userTagKey, [userTag])
            // give each user some tagged skill so they appear in results
            user.addSkill(proj1Subj1Skills[0])
        }
        List<String> tagOrderReversed = userTagOrder.reverse(false)

        when:
        def page1Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 1, 'userTag', true)
        def page2Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 2, 'userTag', true)
        def page3Asc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 3, 'userTag', true)

        def page1Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 1, 'userTag', false)
        def page2Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 2, 'userTag', false)
        def page3Desc = skillsService.getSkillTagUsers(proj1.projectId, tagId, 4, 3, 'userTag', false)
        then:
        page1Asc.count == 10
        page1Asc.data.userTag == userTagOrder[0..3]
        page2Asc.data.userTag == userTagOrder[4..7]
        page3Asc.data.userTag == userTagOrder[8..9]

        page1Desc.data.userTag == tagOrderReversed[0..3]
        page2Desc.data.userTag == tagOrderReversed[4..7]
        page3Desc.data.userTag == tagOrderReversed[8..9]
    }

    def "filter by userIdForDisplay"() {
        String tagValue = "New Tag"
        String tagId = 'newtag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(1, 1, 1, 100, 10)
        proj1Subj1Skills.each {it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills)

        skillsService.addTagToSkills(proj1.projectId, [proj1Subj1Skills[0].skillId], tagValue, tagId)

        List<SkillsService> users = getRandomUsers(10).collect { createService(it)}

        users.each { SkillsService user ->
            user.addSkill(proj1Subj1Skills[0])
        }

        when:
        def allUsers = skillsService.getSkillTagUsers(proj1.projectId, tagId, 100, 1, 'userId', true)
        // shuffle the case of this string
        String userIdForDisplayToQuery = allUsers.data[3].userIdForDisplay
                .toCharArray()
                .collect { Math.random() > 0.5 ? it.toUpperCase() : it.toLowerCase() }
                .join('')
        String userIdForDisplayToQuery1 = allUsers.data[4].userIdForDisplay
                .toCharArray()
                .collect { Math.random() > 0.5 ? it.toUpperCase() : it.toLowerCase() }
                .join('')

        def filteredResult = skillsService.getSkillTagUsers(proj1.projectId, tagId, 100, 1, 'userId', true, userIdForDisplayToQuery)
        def filteredResult1 = skillsService.getSkillTagUsers(proj1.projectId, tagId, 100, 1, 'userId', true, userIdForDisplayToQuery1)

        then:
        allUsers.count == 10
        filteredResult.count == 1
        filteredResult.data[0].userIdForDisplay == allUsers.data[3].userIdForDisplay
        filteredResult1.count == 1
        filteredResult1.data[0].userIdForDisplay == allUsers.data[4].userIdForDisplay
    }

    def "filter by userTag"() {
        String tagValue = "New Tag"
        String tagId = 'newTag'
        String userTagKey = 'dutyOrganization'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(1, 1, 1, 100, 10)
        proj1Subj1Skills.each {it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills)

        skillsService.addTagToSkills(proj1.projectId, [proj1Subj1Skills[0].skillId], tagValue, tagId)

        List<SkillsService> users = getRandomUsers(10).collect { createService(it)}

        SkillsService rootUser = createRootSkillService()

        // assign each user a unique userTag so ordering is deterministic
        users.eachWithIndex { SkillsService user, int index ->
            String userTag = StringUtils.leftPad(index.toString(), 3, "0")
            rootUser.saveUserTag(user.userName, userTagKey, [userTag])
            // give each user some tagged skill so they appear in results
            user.addSkill(proj1Subj1Skills[0])
        }

        when:
        def allUsers = skillsService.getSkillTagUsers(proj1.projectId, tagId, 10, 1, 'userTag', true)

        // shuffle the case of this string
        String userTag1ToQuery = allUsers.data[3].userTag
                .toCharArray()
                .collect { Math.random() > 0.5 ? it.toUpperCase() : it.toLowerCase() }
                .join('')
        String userTag2ToQuery = allUsers.data[4].userTag
                .toCharArray()
                .collect { Math.random() > 0.5 ? it.toUpperCase() : it.toLowerCase() }
                .join('')

        def filtered1 = skillsService.getSkillTagUsers(proj1.projectId, tagId, 10, 1, 'userTag', true, "", 0, 100, userTag1ToQuery)
        def filtered2 = skillsService.getSkillTagUsers(proj1.projectId, tagId, 10, 1, 'userTag', true, "", 0, 100, userTag2ToQuery)
        then:
        allUsers.count == 10
        filtered1.count == 1
        filtered1.data[0].userTag == allUsers.data[3].userTag
        filtered2.count == 1
        filtered2.data[0].userTag == allUsers.data[4].userTag
    }

    def "validation: minimum points must be > 0"() {
        String tagValue = "New Tag"
        String tagId = 'newTag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(1, 1, 1, 100, 10)
        proj1Subj1Skills.each {it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills)
        skillsService.addTagToSkills(proj1.projectId, [proj1Subj1Skills[0].skillId], tagValue, tagId)

        when:
        skillsService.getSkillTagUsers(proj1.projectId, tagId, 10, 1, 'userTag', true, "", -1)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.resBody.contains("Minimum Points is less than 0")
    }

    def "validation: maximum points must be <= 100"() {
        String tagValue = "New Tag"
        String tagId = 'newTag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(1, 1, 1, 100, 10)
        proj1Subj1Skills.each {it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills)
        skillsService.addTagToSkills(proj1.projectId, [proj1Subj1Skills[0].skillId], tagValue, tagId)

        when:
        skillsService.getSkillTagUsers(proj1.projectId, tagId, 10, 1, 'userTag', true, "", 0, 101)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.resBody.contains("Maximum Points is greater than 100")
    }
}